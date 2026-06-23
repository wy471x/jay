package com.jay.tui.core;

import com.jay.hooks.HookDispatcher;
import com.jay.hooks.HookEvent;
import com.jay.hooks.HookSink;
import com.jay.protocol.core.EventFrame;
import com.jay.protocol.core.ResponseChannel;
import com.jay.protocol.core.ThreadRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Virtual-thread orchestrator bridging the TUI render loop with the
 * application Runtime. Runs on a dedicated virtual thread, consuming
 * TuiAction from the action queue and producing TuiEvent into the event queue.
 *
 * <p>Also implements {@link HookSink} to receive real-time engine events
 * (response deltas, tool calls, turn lifecycle) and translates them into
 * TuiEvents for the renderer.
 *
 * <p>Threading model:
 * <ul>
 *   <li>{@code actionQueue} — produced by render thread, consumed by engine thread</li>
 *   <li>{@code eventQueue} — produced by engine thread, consumed by render thread</li>
 * </ul>
 */
public class TuiEngine implements HookSink {

    private final BlockingQueue<TuiAction> actionQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<TuiEvent> eventQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final HookDispatcher hookDispatcher;
    private final ThreadLocal<String> currentThreadId = new ThreadLocal<>();
    private Thread engineThread;

    public TuiEngine(HookDispatcher hookDispatcher) {
        this.hookDispatcher = hookDispatcher;
    }

    /** Start the engine on a new virtual thread and register as a HookSink if dispatcher is available. */
    public void start() {
        running.set(true);
        if (hookDispatcher != null) {
            hookDispatcher.addSink(this);
        }
        engineThread = Thread.ofVirtual()
                .name("tui-engine")
                .start(this::eventLoop);
    }

    /** Request graceful shutdown. */
    public void shutdown() {
        running.set(false);
        if (hookDispatcher != null) {
            hookDispatcher.removeSink(this);
        }
        if (engineThread != null) {
            engineThread.interrupt();
        }
    }

    /** Enqueue an action from the input handler. Thread-safe. */
    public void submitAction(TuiAction action) {
        actionQueue.offer(action);
    }

    /** Drain all available events (non-blocking). Called each render tick. */
    public List<TuiEvent> drainEvents() {
        var events = new ArrayList<TuiEvent>();
        eventQueue.drainTo(events);
        return events;
    }

    /** Emit the Initialized event after engine startup. */
    public void onEngineReady() {
        enqueue(new TuiEvent.Initialized(
                List.of(), "", com.jay.agent.ProviderKind.DEEPSEEK, "deepseek-v4-flash"));
    }

    // ── HookSink ──────────────────────────────────────────────────

    @Override
    public void emit(HookEvent event) {
        translateHookEvent(event);
    }

    // ── Private ───────────────────────────────────────────────────

    private void eventLoop() {
        while (running.get()) {
            try {
                TuiAction action = actionQueue.poll(100, TimeUnit.MILLISECONDS);
                if (action == null) continue;
                processAction(action);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processAction(TuiAction action) {
        switch (action) {
            case TuiAction.Quit q -> handleQuit();
            case TuiAction.SendMessage m -> handleSendMessage(m);
            case TuiAction.SwitchThread t -> currentThreadId.set(t.threadId());
            case TuiAction.ExecuteSlashCommand c -> handleSlashCommand(c);
            default -> {
                // render-local actions (scroll, sidebar toggle) ignored here
            }
        }
    }

    private void handleQuit() {
        running.set(false);
        enqueue(new TuiEvent.ShutdownRequested("user quit"));
    }

    private void handleSendMessage(TuiAction.SendMessage action) {
        String threadId = currentThreadId.get();
        if (threadId == null || threadId.isEmpty()) {
            threadId = "default";
        }
        var message = new ThreadRequest.Message(threadId, action.text());
        enqueue(new TuiEvent.StatusMessage("Message sent: " + action.text(), "info"));
    }

    private void handleSlashCommand(TuiAction.ExecuteSlashCommand action) {
        enqueue(new TuiEvent.SlashResult(
                action.command(), true,
                "Command '" + action.command() + "' processed"));
    }

    private void translateHookEvent(HookEvent event) {
        switch (event) {
            case HookEvent.ResponseDelta d ->
                    enqueue(new TuiEvent.ResponseDelta(
                            d.responseId(), d.delta(),
                            ResponseChannel.TEXT, d.responseId()));
            case HookEvent.ResponseEnd e ->
                    enqueue(new TuiEvent.ResponseEnd(e.responseId(), e.responseId()));
            case HookEvent.ToolLifecycle t -> translateToolLifecycle(t);
            case HookEvent.GenericEventFrame g ->
                    translateEventFrame(g.frame());
            default -> {
                // other HookEvent variants ignored by TUI
            }
        }
    }

    private void translateToolLifecycle(HookEvent.ToolLifecycle t) {
        switch (t.phase()) {
            case "dispatching" ->
                    enqueue(new TuiEvent.ToolCallBegin(t.toolName(),
                            t.payload() != null ? t.payload().toString() : ""));
            case "completed" ->
                    enqueue(new TuiEvent.ToolCallEnd(t.toolName(), true, null));
            case "failed" -> {
                String error = t.payload() != null
                        ? t.payload().toString()
                        : "unknown error";
                enqueue(new TuiEvent.ToolCallEnd(t.toolName(), false, error));
            }
            default -> {
                // precheck, etc. — not rendered
            }
        }
    }

    private void translateEventFrame(EventFrame frame) {
        switch (frame) {
            case EventFrame.TurnStarted t ->
                    enqueue(new TuiEvent.TurnStarted(t.turnId()));
            case EventFrame.TurnComplete t ->
                    enqueue(new TuiEvent.TurnComplete(t.turnId()));
            case EventFrame.TurnAborted t ->
                    enqueue(new TuiEvent.TurnAborted(t.turnId(), t.reason()));
            default -> {
                // other frame types not rendered by TUI
            }
        }
    }

    private void enqueue(TuiEvent event) {
        eventQueue.offer(event);
    }
}
