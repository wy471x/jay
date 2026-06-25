package com.jay.tui.core;

import com.jay.hooks.HookDispatcher;
import com.jay.hooks.HookEvent;
import com.jay.hooks.HookSink;
import com.jay.tui.client.ChatMessage;

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
                if (op == null) continue;
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

        // Add user message to session
        session.addMessage(ChatMessage.user(op.content()));

        // Create turn context
        var turn = new TurnContext(config.maxSteps());
        emit(new TuiEngineEvent.TurnStarted(turn.turnId()));
        emit(new TuiEngineEvent.StatusMessage("Turn started — " + turn.turnId(), "info"));

        // The actual SSE turn loop will be connected in Phase 3.
        // For now, emit a placeholder response.
        emit(new TuiEngineEvent.MessageStarted(0, "text"));
        emit(new TuiEngineEvent.MessageDelta(0, "[Engine ready — SSE turn loop (Phase 3)]"));
        emit(new TuiEngineEvent.MessageComplete(0, "text"));

        session.addMessage(ChatMessage.assistant("[Engine ready — SSE turn loop (Phase 3)]"));

        emit(new TuiEngineEvent.TurnComplete(
                turn.turnId(), 0, 0,
                TuiEngineEvent.TurnOutcomeStatus.COMPLETED, null
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
        emit(new TuiEngineEvent.CompactionCompleted(
                "manual", false, "Compaction done", before, before));
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
