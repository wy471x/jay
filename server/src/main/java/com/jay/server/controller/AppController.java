package com.jay.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jay.agent.ModelRegistry;
import com.jay.execpolicy.ExecPolicyEngine;
import com.jay.execpolicy.ExecPolicyContext;
import com.jay.execpolicy.ExecPolicyDecision;
import com.jay.protocol.*;
import com.jay.protocol.approval.AskForApproval;
import com.jay.tools.ToolRegistry;
import com.jay.tools.ToolCall;
import com.jay.protocol.core.ToolOutput;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Main app-server REST controller — equivalent to Rust's protected route handlers.
 *
 * Routes: /healthz, /thread, /app, /prompt, /tool, /jobs, /mcp/startup
 */
@RestController
public class AppController {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final ModelRegistry modelRegistry;
    private final ToolRegistry toolRegistry;
    private final ExecPolicyEngine policyEngine;

    public AppController(ModelRegistry modelRegistry, ToolRegistry toolRegistry,
                         ExecPolicyEngine policyEngine) {
        this.modelRegistry = modelRegistry;
        this.toolRegistry = toolRegistry;
        this.policyEngine = policyEngine;
    }

    // ── Health ──────────────────────────────────────────────────────

    @GetMapping("/healthz")
    public ResponseEntity<Map<String, String>> healthz() {
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "protocol", "v2",
            "service", "jay-app-server"
        ));
    }

    // ── Thread ──────────────────────────────────────────────────────

    @PostMapping("/thread")
    public ResponseEntity<JsonNode> thread(@RequestBody ThreadRequest request) {
        try {
            ObjectNode result = mapper.createObjectNode();
            result.put("ok", true);
            result.put("kind", request.getClass().getSimpleName());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            ObjectNode error = mapper.createObjectNode();
            error.put("thread_id", "error");
            error.put("status", "error:" + e.getMessage());
            return ResponseEntity.ok(error);
        }
    }

    // ── App ─────────────────────────────────────────────────────────

    @PostMapping("/app")
    public ResponseEntity<JsonNode> app(@RequestBody AppRequest request) {
        return switch (request) {
            case AppRequest.Capabilities c -> capabilities();
            case AppRequest.ConfigGet g -> configGet(g.key());
            case AppRequest.ConfigSet s -> configSet(s.key(), s.value());
            case AppRequest.ConfigUnset u -> configUnset(u.key());
            case AppRequest.ConfigList l -> configList();
            case AppRequest.Models m -> models();
            case AppRequest.ThreadLoadedList t -> threadLoadedList();
        };
    }

    private ResponseEntity<JsonNode> capabilities() {
        ObjectNode result = mapper.createObjectNode();
        result.put("service", "jay-app-server");
        result.put("transport", "http");
        ArrayNode routes = mapper.createArrayNode();
        routes.add("/healthz").add("/v1/chat/completions")
            .add("/thread").add("/app").add("/prompt").add("/tool").add("/jobs")
            .add("/mcp/startup");
        result.set("routes", routes);
        ArrayNode events = mapper.createArrayNode();
        events.add("response_start").add("response_delta").add("response_end")
            .add("tool_call_start").add("tool_call_result")
            .add("mcp_startup_update").add("mcp_startup_complete");
        result.set("event_types", events);
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<JsonNode> configGet(String key) {
        ObjectNode result = mapper.createObjectNode();
        result.put("key", key);
        result.put("value", "");  // placeholder — real impl reads from ConfigStore
        result.put("redacted", true);
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<JsonNode> configSet(String key, String value) {
        ObjectNode result = mapper.createObjectNode();
        result.put("ok", true);
        result.put("key", key);
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<JsonNode> configUnset(String key) {
        ObjectNode result = mapper.createObjectNode();
        result.put("ok", true);
        result.put("key", key);
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<JsonNode> configList() {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode items = mapper.createArrayNode();
        items.addObject().put("key", "jay.default-model").put("value", "claude-sonnet-4-6");
        result.set("config", items);
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<JsonNode> models() {
        ObjectNode result = mapper.createObjectNode();
        result.set("models", mapper.valueToTree(modelRegistry.list()));
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<JsonNode> threadLoadedList() {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode threads = mapper.createArrayNode();
        result.set("threads", threads);
        return ResponseEntity.ok(result);
    }

    // ── Prompt ───────────────────────────────────────────────────────

    @PostMapping("/prompt")
    public ResponseEntity<PromptResponse> prompt(@RequestBody PromptRequest request) {
        return ResponseEntity.ok(new PromptResponse(
            "prompt processed",
            "claude-sonnet-4-6",
            List.of()
        ));
    }

    // ── Tool ─────────────────────────────────────────────────────────

    @PostMapping("/tool")
    public ResponseEntity<JsonNode> tool(@RequestBody JsonNode body) {
        try {
            JsonNode callNode = body.get("call");
            String cwd = body.has("cwd") ? body.get("cwd").asText() : System.getProperty("user.dir");

            ToolCall call = mapper.convertValue(callNode, ToolCall.class);
            if (call == null) {
                return ResponseEntity.badRequest().body(
                    mapper.createObjectNode().put("ok", false).put("error", "missing call"));
            }

            // Determine approval mode from config (placeholder: UnlessTrusted)
            var mode = new AskForApproval.UnlessTrusted();

            ToolOutput output;
            try {
                output = toolRegistry.dispatch(call, true);
            } catch (Exception e) {
                ObjectNode err = mapper.createObjectNode();
                err.put("ok", false);
                err.put("error", e.getMessage());
                return ResponseEntity.ok(err);
            }

            ObjectNode result = mapper.createObjectNode();
            result.put("ok", true);
            result.set("result", mapper.valueToTree(output));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            ObjectNode err = mapper.createObjectNode();
            err.put("ok", false);
            err.put("error", e.getMessage());
            return ResponseEntity.ok(err);
        }
    }

    // ── Jobs ─────────────────────────────────────────────────────────

    @GetMapping("/jobs")
    public ResponseEntity<JsonNode> jobs() {
        ObjectNode result = mapper.createObjectNode();
        result.put("ok", true);
        ArrayNode jobs = mapper.createArrayNode();
        result.set("jobs", jobs);
        return ResponseEntity.ok(result);
    }

    // ── MCP Startup ──────────────────────────────────────────────────

    @PostMapping("/mcp/startup")
    public ResponseEntity<JsonNode> mcpStartup() {
        ObjectNode result = mapper.createObjectNode();
        result.put("ok", true);
        ObjectNode summary = mapper.createObjectNode();
        ArrayNode ready = mapper.createArrayNode();
        summary.set("ready", ready);
        ArrayNode failed = mapper.createArrayNode();
        summary.set("failed", failed);
        result.set("summary", summary);
        return ResponseEntity.ok(result);
    }
}
