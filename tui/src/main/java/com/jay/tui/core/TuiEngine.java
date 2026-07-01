package com.jay.tui.core;

import com.jay.hooks.HookDispatcher;
import com.jay.hooks.HookEvent;
import com.jay.hooks.HookSink;
import com.jay.tui.client.ChatMessage;
import com.jay.tui.core.compaction.CompactionConfig;
import com.jay.tui.core.compaction.CompactionExecutor;
import com.jay.tui.core.seam.SeamConfig;
import com.jay.tui.core.seam.SeamManager;
import com.jay.tui.core.turn.SseTurnLoop;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Virtual-thread orchestrator bridging the TUI render loop with the LLM API.
 * Consumes {@link TuiEngineOp} from the op queue and produces
 * {@link TuiEngineEvent} into the event queue.
 *
 * <p>Architecture mirrors Rust {@code Engine::run()}:
 * <ul>
 *   <li>Virtual thread polls {@code opQueue} in a loop</li>
 *   <li>Each op is dispatched to a handler</li>
 *   <li>Events are produced into {@code eventQueue} for the render thread</li>
 *   <li>A {@link Session} tracks conversation state and token usage</li>
 * </ul>
 *
 * <p>Use {@link #spawn(EngineConfig)} to start the engine and get an
 * {@link EngineHandle} for the render thread.
 */
public class TuiEngine {

    private final EngineConfig config;
    private final EngineHandle handle;
    private final Session session;
    private final AtomicBoolean running;
    private final HookDispatcher hookDispatcher;
    private final SeamManager seamManager;
    private long lastSeamCheckMs;

    /** Create the engine, create the handle, but don't start yet. */
    public TuiEngine(EngineConfig config) {
        this(config, null);
    }

    public TuiEngine(EngineConfig config, HookDispatcher hookDispatcher) {
        this.config = config;
        this.handle = new EngineHandle();
        this.session = new Session(
                config.model(), config.workspace(),
                config.allowShell(), config.trustMode()
        );
        this.running = new AtomicBoolean(false);
        this.hookDispatcher = hookDispatcher;
        this.seamManager = new SeamManager(null, // Flash client wired when available
                config.seam() != null ? config.seam() : SeamConfig.disabled(),
                handle.eventQueue());
        this.lastSeamCheckMs = System.currentTimeMillis();
    }

    /**
     * Start the engine on a new virtual thread and return the handle.
     * Mirrors Rust {@code spawn_engine()}.
     */
    public EngineHandle spawn() {
        running.set(true);

        if (hookDispatcher != null) {
            hookDispatcher.addSink(new EngineHookSink(handle.eventQueue()));
        }

        Thread thread = Thread.ofVirtual()
                .name("tui-engine")
                .start(this::eventLoop);
        handle.markRunning(thread);
        return handle;
    }

    /** Convenience: create and spawn in one call. */
    public static EngineHandle spawn(EngineConfig config) {
        return new TuiEngine(config).spawn();
    }

    // ── Main event loop ────────────────────────────────────────────────

    private void eventLoop() {
        emit(new TuiEngineEvent.EngineInitialized(
                config.model(),
                config.workspace().getFileName().toString(),
                List.of(), ""
        ));

        while (running.get()) {
            try {
                TuiEngineOp op = handle.opQueue().poll(100, TimeUnit.MILLISECONDS);
                if (op == null) {
                    maybeProduceSeam();
                    continue;
                }
                processOp(op);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                emit(new TuiEngineEvent.EngineError(
                        e.getMessage(), "internal", false));
            }
        }
        handle.markStopped();
    }

    // ── Op dispatcher ──────────────────────────────────────────────────

    private void processOp(TuiEngineOp op) {
        switch (op) {
            case TuiEngineOp.SendMessage m   -> handleSendMessage(m);
            case TuiEngineOp.RunShellCommand c -> handleRunShellCommand(c);
            case TuiEngineOp.CancelRequest __ -> handleCancelRequest();
            case TuiEngineOp.ApproveToolCall a -> handleApproveToolCall(a);
            case TuiEngineOp.DenyToolCall d   -> handleDenyToolCall(d);
            case TuiEngineOp.ChangeMode m     -> handleChangeMode(m);
            case TuiEngineOp.SetModel m       -> handleSetModel(m);
            case TuiEngineOp.SetCompaction c  -> handleSetCompaction(c);
            case TuiEngineOp.CompactContext __ -> handleCompactContext();
            case TuiEngineOp.EditLastTurn e   -> handleEditLastTurn(e);
            case TuiEngineOp.Shutdown __      -> handleShutdown();
        }
    }

    // ── Op handlers ────────────────────────────────────────────────────

    private void handleSendMessage(TuiEngineOp.SendMessage op) {
        if (!config.allowShell()) {
            session.setAllowShell(op.allowShell());
        }
        session.setTrustMode(op.trustMode());
        session.setAutoApprove(op.autoApprove());

        // Create turn context and emit turn lifecycle start
        var turn = new TurnContext(config.maxSteps());
        emit(new TuiEngineEvent.TurnStarted(turn.turnId()));
        emit(new TuiEngineEvent.StatusMessage("Turn started — " + turn.turnId(), "info"));

        // Wire SseTurnLoop with session messages, event queue, cancel token.
        // SseTurnLoop mutates the list in-place; use the internal mutable list.
        var compactionCfg = config.compaction() != null
                ? config.compaction() : CompactionConfig.disabled();
        var messages = session.mutableMessages();
        var turnLoop = new SseTurnLoop(
                handle.eventQueue(),
                handle.cancelRequested(),
                compactionCfg,
                messages
        );

        int steps = turnLoop.executeTurn(turn, op.content());

        // Accumulate turn usage and bump revision after messages mutated by SseTurnLoop
        session.addUsage(turn.inputTokens(), turn.outputTokens(), null, null);
        session.bumpMessagesRevision();

        if (turn.isCancelled()) {
            emit(new TuiEngineEvent.TurnAborted(turn.turnId(), "cancelled"));
        }
        // Note: SseTurnLoop already emits TurnComplete — engine only emits extra context
        emit(new TuiEngineEvent.SessionUpdated(
                session.id(), session.messages().size(),
                session.totalUsage().totalTokens()
        ));
    }

    private void handleRunShellCommand(TuiEngineOp.RunShellCommand op) {
        emit(new TuiEngineEvent.StatusMessage(
                "Shell: " + op.command(), "info"));
    }

    private void handleCancelRequest() {
        handle.cancelRequested().set(true);
        emit(new TuiEngineEvent.StatusMessage("Request cancelled", "warn"));
    }

    private void handleApproveToolCall(TuiEngineOp.ApproveToolCall op) {
        emit(new TuiEngineEvent.StatusMessage(
                "Approved: " + op.toolName(), "info"));
    }

    private void handleDenyToolCall(TuiEngineOp.DenyToolCall op) {
        emit(new TuiEngineEvent.StatusMessage(
                "Denied: " + op.id(), "warn"));
    }

    private void handleChangeMode(TuiEngineOp.ChangeMode op) {
        emit(new TuiEngineEvent.StatusMessage(
                "Mode changed to: " + op.mode(), "info"));
    }

    private void handleSetModel(TuiEngineOp.SetModel op) {
        session.setModel(op.modelName());
        emit(new TuiEngineEvent.ModelChanged(op.modelName(), null));
        emit(new TuiEngineEvent.SessionUpdated(
                session.id(), session.messages().size(),
                session.totalUsage().totalTokens()
        ));
    }

    private void handleSetCompaction(TuiEngineOp.SetCompaction op) {
        emit(new TuiEngineEvent.StatusMessage(
                "Compaction: enabled=" + op.enabled()
                        + " threshold=" + op.tokenThreshold(), "info"));
    }

    private void handleCompactContext() {
        emit(new TuiEngineEvent.CompactionStarted("manual", false, "Manual compaction"));
        int before = session.messages().size();

        var compactionCfg = config.compaction() != null
                ? config.compaction() : CompactionConfig.disabled();

        if (!compactionCfg.enabled()) {
            emit(new TuiEngineEvent.StatusMessage(
                    "Compaction is disabled. Enable with --compaction flag or /compact command.",
                    "warn"));
            return;
        }

        try {
            var executor = new CompactionExecutor(null, compactionCfg,
                    handle.eventQueue());
            var result = executor.compactMessagesSafe(session.mutableMessages());

            if (result.compacted()) {
                session.replaceMessages(result.messages());
                emit(new TuiEngineEvent.CompactionCompleted(
                        "manual", false,
                        "Compacted " + result.removedMessages().size() + " messages"
                                + (result.retriesUsed() > 0 ? " (retries: " + result.retriesUsed() + ")" : ""),
                        before, result.messages().size()));
            } else if (result.retriesUsed() > 0) {
                emit(new TuiEngineEvent.CompactionFailed(
                        "manual", false,
                        "Compaction failed after " + result.retriesUsed() + " retries"));
            } else {
                emit(new TuiEngineEvent.CompactionCompleted(
                        "manual", false, "No compaction needed (below threshold)",
                        before, before));
            }
        } catch (Exception e) {
            emit(new TuiEngineEvent.CompactionFailed(
                    "manual", false, e.getMessage()));
        }
    }

    private void handleEditLastTurn(TuiEngineOp.EditLastTurn op) {
        session.trimLast(2); // remove last user + assistant
        session.addMessage(ChatMessage.user(op.newMessage()));
        session.bumpMessagesRevision();
        emit(new TuiEngineEvent.StatusMessage("Last turn edited", "info"));
    }

    private void handleShutdown() {
        running.set(false);
        emit(new TuiEngineEvent.StatusMessage("Engine shutting down", "info"));
    }

    // ── Seam detection ────────────────────────────────────────────────

    /**
     * Auto-detect and produce soft seams based on active input token estimate.
     * Called from the event loop tick when idle. Mirrors Rust
     * {@code maybe_produce_seam()}.
     *
     * <p>Checks every ~5 seconds to avoid excessive computation.
     */
    private void maybeProduceSeam() {
        if (!seamManager.config().enabled()) return;

        // Throttle checks to every 5 seconds
        long now = System.currentTimeMillis();
        if (now - lastSeamCheckMs < 5000) return;
        lastSeamCheckMs = now;

        var messages = session.mutableMessages();
        if (messages.isEmpty()) return;

        // Estimate active input tokens
        int activeTokens = com.jay.tui.core.compaction.CompactionPlanner
                .estimateTokensConservative(messages);

        Integer level = seamManager.seamLevelFor(activeTokens);
        if (level == null) return;

        // Determine the message range to summarize: everything before verbatim window
        int verbatimStart = seamManager.verbatimWindowStart(messages.size());
        if (verbatimStart == 0) return; // nothing to summarize

        // For levels > 1, check if we should recompact prior seams
        if (level > 1) {
            var existingSeams = SeamManager.collectSeamTexts(messages);
            if (!existingSeams.isEmpty()) {
                // Messages between prior seam coverage and verbatim start
                var newMsgs = new ArrayList<ChatMessage>();
                for (int i = existingSeams.size(); i < verbatimStart && i < messages.size(); i++) {
                    newMsgs.add(messages.get(i));
                }
                try {
                    String seamBlock = seamManager.recompact(
                            existingSeams, newMsgs, level, 0, verbatimStart);
                    if (!seamBlock.isEmpty()) {
                        session.mutableMessages().add(ChatMessage.assistant(seamBlock));
                        session.bumpMessagesRevision();
                        emit(new TuiEngineEvent.SeamRecompacted(
                                level, existingSeams.size(),
                                seamBlock.length() / 4));
                        emit(new TuiEngineEvent.StatusMessage(
                                "Soft seam L" + level + " recompacted ("
                                        + existingSeams.size() + " prior seams)",
                                "info"));
                    }
                } catch (Exception e) {
                    emit(new TuiEngineEvent.StatusMessage(
                            "Seam recompact failed: " + e.getMessage(), "warn"));
                }
                return;
            }
        }

        // First-time seam at this level
        try {
            String seamBlock = seamManager.produceSoftSeam(
                    messages, level, 0, verbatimStart);
            if (!seamBlock.isEmpty()) {
                session.mutableMessages().add(ChatMessage.assistant(seamBlock));
                session.bumpMessagesRevision();
                emit(new TuiEngineEvent.SeamProduced(
                        level, 0, verbatimStart,
                        seamBlock.length() / 4, seamManager.config().seamModel()));
                emit(new TuiEngineEvent.StatusMessage(
                        "Soft seam L" + level + " produced (msgs 0-"
                                + verbatimStart + ")", "info"));
            }
        } catch (Exception e) {
            // Seam failure is non-fatal — continue without seam
            emit(new TuiEngineEvent.StatusMessage(
                    "Seam production failed: " + e.getMessage(), "warn"));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private void emit(TuiEngineEvent event) {
        handle.eventQueue().offer(event);
    }

    // ── HookSink adapter (legacy compat) ─────────────────────────────

    private record EngineHookSink(
            java.util.concurrent.BlockingQueue<TuiEngineEvent> queue
    ) implements HookSink {
        @Override
        public void emit(HookEvent event) {
            switch (event) {
                case HookEvent.ResponseDelta d ->
                        queue.offer(new TuiEngineEvent.MessageDelta(0, d.delta()));
                case HookEvent.ResponseEnd e ->
                        queue.offer(new TuiEngineEvent.MessageComplete(0, "text"));
                case HookEvent.ToolLifecycle t -> {
                    switch (t.phase()) {
                        case "dispatching" ->
                                queue.offer(new TuiEngineEvent.ToolCallStarted(
                                        "hook", t.toolName(),
                                        t.payload() != null ? t.payload().toString() : "{}"));
                        case "completed" ->
                                queue.offer(new TuiEngineEvent.ToolCallComplete(
                                        "hook", t.toolName(),
                                        TuiEngineEvent.ToolResult.success("hook", t.toolName(), "OK")));
                        case "failed" ->
                                queue.offer(new TuiEngineEvent.ToolCallComplete(
                                        "hook", t.toolName(),
                                        TuiEngineEvent.ToolResult.failure("hook", t.toolName(),
                                                t.payload() != null ? t.payload().toString() : "error")));
                        default -> {} // precheck, etc. — not rendered
                    }
                }
                default -> {} // other HookEvent variants
            }
        }
    }
}
