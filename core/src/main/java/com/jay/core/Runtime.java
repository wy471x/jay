package com.jay.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jay.agent.ModelRegistry;
import com.jay.config.model.CliRuntimeOverrides;
import com.jay.config.model.ConfigToml;
import com.jay.core.job.JobManager;
import com.jay.core.thread.InitialHistory;
import com.jay.core.thread.ThreadManager;
import com.jay.execpolicy.*;
import com.jay.hooks.HookDispatcher;
import com.jay.hooks.HookEvent;
import com.jay.mcp.manager.McpManager;
import com.jay.mcp.lifecycle.McpStartupCompleteEvent;
import com.jay.protocol.approval.AskForApproval;
import com.jay.protocol.core.*;
import com.jay.protocol.params.*;
import com.jay.tools.ToolCall;
import com.jay.tools.ToolRegistry;
import com.jay.tools.ToolResult;

import java.util.*;

/**
 * Top-level orchestrator that wires 7 subsystems into a coordinated runtime.
 * Equivalent to Rust's Runtime — handles ThreadRequest dispatch, prompt processing,
 * tool invocation with policy + approval flow, and MCP server startup.
 */
public class Runtime {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final ConfigToml config;
    private final ModelRegistry modelRegistry;
    private final ThreadManager threadManager;
    private final ToolRegistry toolRegistry;
    private final McpManager mcpManager;
    private final ExecPolicyEngine execPolicy;
    private final HookDispatcher hooks;
    private final JobManager jobManager;

    public Runtime(ConfigToml config, ModelRegistry modelRegistry,
                   ThreadManager threadManager, ToolRegistry toolRegistry,
                   McpManager mcpManager, ExecPolicyEngine execPolicy,
                   HookDispatcher hooks, JobManager jobManager) {
        this.config = config;
        this.modelRegistry = modelRegistry;
        this.threadManager = threadManager;
        this.toolRegistry = toolRegistry;
        this.mcpManager = mcpManager;
        this.execPolicy = execPolicy;
        this.hooks = hooks;
        this.jobManager = jobManager;
        jobManager.resumePending();
    }

    // ======== Thread request dispatch (14 variants) ============

    public ThreadResponse handleThread(ThreadRequest request) {
        return switch (request) {
            case ThreadRequest.Create c    -> handleCreate(c);
            case ThreadRequest.Start s     -> handleStart(s);
            case ThreadRequest.Resume r   -> handleResume(r);
            case ThreadRequest.Fork f     -> handleFork(f);
            case ThreadRequest.List l     -> handleList(l);
            case ThreadRequest.Read r     -> handleRead(r);
            case ThreadRequest.SetName s  -> handleSetName(s);
            case ThreadRequest.GoalSet g  -> handleGoalSet(g);
            case ThreadRequest.GoalGet g  -> handleGoalGet(g);
            case ThreadRequest.GoalClear g -> handleGoalClear(g);
            case ThreadRequest.GoalRecordProgress g -> handleGoalProgress(g);
            case ThreadRequest.Archive a  -> handleArchive(a);
            case ThreadRequest.Unarchive u -> handleUnarchive(u);
            case ThreadRequest.Message m  -> handleMessage(m);
        };
    }

    // ======== Prompt handling ========

    public PromptResponse handlePrompt(PromptRequest prompt, CliRuntimeOverrides overrides) {
        var responseId = "prompt-" + UUID.randomUUID();
        hooks.emit(new HookEvent.ResponseStart(responseId));

        ObjectNode payload = mapper.createObjectNode();
        payload.put("prompt", prompt.prompt());
        payload.put("model", prompt.model() != null ? prompt.model() : "default");
        hooks.emit(new HookEvent.ResponseDelta(responseId, payload.toString()));

        if (prompt.threadId() != null) {
            threadManager.touchMessage(prompt.threadId(), prompt.prompt());
        }

        hooks.emit(new HookEvent.ResponseEnd(responseId));
        return new PromptResponse(payload.toString(), prompt.model() != null ? prompt.model() : "default", List.of());
    }

    // ======== Tool invocation (policy + approval flow) ========

    public ToolResult invokeTool(ToolCall call, AskForApproval approvalMode, String cwd) {
        var subject = call.executionSubject(cwd);
        String policyTool = call.payload() instanceof ToolPayload.LocalShell
            ? "exec_shell" : call.name();
        String callId = "call-" + UUID.randomUUID();

        // Policy check
        var ctx = new ExecPolicyContext(subject.command(), cwd, policyTool,
            null, approvalMode, null);
        var decision = execPolicy.check(ctx);

        hooks.emit(new HookEvent.ToolLifecycle(callId, call.name(), "precheck",
            mapper.createObjectNode().put("tool", call.name()).put("phase", "precheck")));

        if (!decision.allow()) {
            hooks.emit(new HookEvent.ApprovalLifecycle(callId, "denied", decision.requirement().reason()));
            return ToolResult.error("Tool denied by policy: " + decision.requirement().reason());
        }

        if (decision.requiresApproval()) {
            hooks.emit(new HookEvent.ApprovalLifecycle(callId, "requested", null));
            return ToolResult.error("Approval required");
        }

        hooks.emit(new HookEvent.ToolLifecycle(callId, call.name(), "dispatching",
            mapper.createObjectNode().put("tool", call.name()).put("phase", "dispatching")));

        try {
            ToolOutput output = toolRegistry.dispatch(call, true);
            hooks.emit(new HookEvent.ToolLifecycle(callId, call.name(), "completed",
                mapper.createObjectNode().put("tool", call.name()).put("phase", "completed")));
            return ToolResult.json(output);
        } catch (Exception e) {
            hooks.emit(new HookEvent.ToolLifecycle(callId, call.name(), "failed",
                mapper.createObjectNode().put("tool", call.name())
                    .put("phase", "failed").put("error", e.getMessage())));
            return ToolResult.error(e.getMessage());
        }
    }

    // ======== MCP startup ========

    public McpStartupCompleteEvent mcpStartup() {
        return mcpManager.startAll(update ->
            hooks.emit(new HookEvent.GenericEventFrame(
                new EventFrame.McpStartupUpdate(null))));
    }

    // ── Thread handlers ──────────────────────────────────────

    private ThreadResponse handleCreate(ThreadRequest.Create req) {
        var nt = threadManager.spawnThreadWithHistory("deepseek", System.getProperty("user.dir"),
            new InitialHistory.New());
        return buildThreadResponse(nt.id(), "ok", null);
    }

    private ThreadResponse handleStart(ThreadRequest.Start req) {
        var nt = threadManager.spawnThreadWithHistory(
            req.params().modelProvider() != null ? req.params().modelProvider() : "deepseek",
            req.params().cwd() != null ? req.params().cwd() : System.getProperty("user.dir"),
            new InitialHistory.New());
        return buildThreadResponse(nt.id(), "ok", req.params().model());
    }

    private ThreadResponse handleResume(ThreadRequest.Resume req) {
        var thread = threadManager.resumeThreadWithHistory(req.params(), System.getProperty("user.dir"));
        return buildThreadResponse(thread.id(), "ok", req.params().model());
    }

    private ThreadResponse handleFork(ThreadRequest.Fork req) {
        var nt = threadManager.forkThread(req.params(), System.getProperty("user.dir"));
        return buildThreadResponse(nt.id(), "ok", null);
    }

    private ThreadResponse handleList(ThreadRequest.List req) {
        var params = req.params() != null ? req.params() : new ThreadListParams(false, 50);
        var threads = threadManager.listThreads(params);
        return new ThreadResponse(null, "ok", null, threads, null, null, null,
            null, null, null, null, mapper.createObjectNode());
    }

    private ThreadResponse handleRead(ThreadRequest.Read req) {
        var thread = threadManager.readThread(req.params().threadId());
        if (thread.isPresent()) {
            var t = thread.get();
            return new ThreadResponse(t.id(), "ok", t, List.of(), null, null, null,
                null, null, null, null, mapper.createObjectNode());
        }
        return new ThreadResponse(req.params().threadId(), "error", null, List.of(),
            null, null, null, null, null, null, null, mapper.createObjectNode());
    }

    private ThreadResponse handleSetName(ThreadRequest.SetName req) {
        threadManager.setThreadName(req.params().threadId(), req.params().name());
        return buildThreadResponse(req.params().threadId(), "ok", null);
    }

    private ThreadResponse handleGoalSet(ThreadRequest.GoalSet req) {
        threadManager.upsertThreadGoal(req.params().threadId(),
            req.params().objective(), req.params().tokenBudget());
        return buildThreadResponse(req.params().threadId(), "ok", null);
    }

    private ThreadResponse handleGoalGet(ThreadRequest.GoalGet req) {
        return buildThreadResponse(req.params().threadId(), "ok", null);
    }

    private ThreadResponse handleGoalClear(ThreadRequest.GoalClear req) {
        threadManager.clearThreadGoal(req.params().threadId());
        return buildThreadResponse(req.params().threadId(), "ok", null);
    }

    private ThreadResponse handleGoalProgress(ThreadRequest.GoalRecordProgress req) {
        var p = req.params();
        threadManager.recordGoalUsage(p.threadId(), p.tokenDelta(), p.timeDeltaSeconds());
        if (p.recordContinuation()) threadManager.recordGoalContinuation(p.threadId());
        return buildThreadResponse(p.threadId(), "ok", null);
    }

    private ThreadResponse handleArchive(ThreadRequest.Archive req) {
        threadManager.archiveThread(req.threadId());
        return buildThreadResponse(req.threadId(), "ok", null);
    }

    private ThreadResponse handleUnarchive(ThreadRequest.Unarchive req) {
        threadManager.unarchiveThread(req.threadId());
        return buildThreadResponse(req.threadId(), "ok", null);
    }

    private ThreadResponse handleMessage(ThreadRequest.Message req) {
        hooks.emit(new HookEvent.ResponseStart(req.threadId()));
        threadManager.touchMessage(req.threadId(), req.input());
        hooks.emit(new HookEvent.ResponseEnd(req.threadId()));
        return buildThreadResponse(req.threadId(), "ok", null);
    }

    // ── Helpers ──────────────────────────────────────────────

    private ThreadResponse buildThreadResponse(String threadId, String status, String model) {
        return new ThreadResponse(threadId, status, null, List.of(), null, model, null,
            null, null, null, List.of(), mapper.createObjectNode());
    }
}
