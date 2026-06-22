package com.jay.tui.state;

import com.jay.agent.ProviderKind;
import com.jay.protocol.core.Thread;
import com.jay.state.model.MessageEntity;
import com.jay.tui.core.TuiEvent;
import com.jay.tui.core.TuiSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized mutable state tree for the TUI. All application state that the
 * renderer needs to draw lives here, organized into composable sub-states.
 *
 * <p>The ONLY mutation entry point is {@link #apply(TuiEvent)}. Renderers read
 * state through getters and MUST NOT write directly.
 *
 * <p>Thread safety: state is read and written exclusively on the render thread
 * (single-threaded, lock-free). Engine events are drained and applied in batch
 * before each frame render.
 *
 * <p>Equivalent to Rust's {@code App} struct.
 */
public class AppState {

    private final ComposerState composer = new ComposerState();
    private final ViewportState viewport = new ViewportState();
    private final SidebarState sidebar = new SidebarState();
    private final StatusBarState statusBar = new StatusBarState();

    private TuiSession session = new TuiSession();
    private boolean initialized;
    private boolean shuttingDown;
    private String activeThreadId;
    private final List<Thread> threads = new ArrayList<>();
    private final Map<String, List<MessageEntity>> messages = new HashMap<>();
    private String currentModel = "deepseek-v4-flash";
    private ProviderKind currentProvider = ProviderKind.DEEPSEEK;

    /** The ONLY mutation entry point. */
    public void apply(TuiEvent event) {
        switch (event) {
            case TuiEvent.Initialized e       -> onInitialized(e);
            case TuiEvent.ShutdownRequested e -> shuttingDown = true;
            case TuiEvent.ResponseDelta e     -> {} // append to streaming buffer
            case TuiEvent.ResponseEnd e       -> {} // commit streaming buffer
            case TuiEvent.ToolCallBegin e     -> statusBar.setSpinner(true);
            case TuiEvent.ToolCallEnd e       -> statusBar.setSpinner(false);
            case TuiEvent.TurnStarted e       -> statusBar.setSpinner(true);
            case TuiEvent.TurnComplete e      -> statusBar.setSpinner(false);
            case TuiEvent.TurnAborted e       -> statusBar.setSpinner(false);
            case TuiEvent.ThreadListUpdated e -> onThreadListUpdated(e);
            case TuiEvent.ThreadSwitched e    -> onThreadSwitched(e);
            case TuiEvent.Error e             -> statusBar.setStatus(e.message(), StatusBarState.Severity.ERROR);
            case TuiEvent.StatusMessage e     -> statusBar.setStatus(e.text(), StatusBarState.Severity.valueOf(e.severity().toUpperCase()));
            case TuiEvent.SlashResult e       -> {} // rendered by SlashMenu
            case TuiEvent.ModelChanged e      -> { currentModel = e.modelName(); currentProvider = e.provider(); }
            case TuiEvent.Resized e           -> {}
        }
    }

    private void onInitialized(TuiEvent.Initialized e) {
        initialized = true;
        threads.clear();
        threads.addAll(e.threads());
        activeThreadId = e.currentThreadId();
        currentProvider = e.provider();
        currentModel = e.modelName();
        sidebar.setThreads(threads);
        statusBar.setModelInfo(e.modelName(), e.provider());
    }

    private void onThreadListUpdated(TuiEvent.ThreadListUpdated e) {
        threads.clear();
        threads.addAll(e.threads());
        sidebar.setThreads(threads);
    }

    private void onThreadSwitched(TuiEvent.ThreadSwitched e) {
        activeThreadId = e.threadId();
        messages.put(e.threadId(), new ArrayList<>(e.messages()));
    }

    // ── Getters ───────────────────────────────────────────────────

    public ComposerState composer() { return composer; }
    public ViewportState viewport() { return viewport; }
    public SidebarState sidebar()   { return sidebar; }
    public StatusBarState statusBar() { return statusBar; }
    public TuiSession session()     { return session; }
    public boolean initialized()    { return initialized; }
    public boolean shuttingDown()   { return shuttingDown; }
    public String activeThreadId()  { return activeThreadId; }
    public String currentModel()    { return currentModel; }
    public ProviderKind currentProvider() { return currentProvider; }
    public List<Thread> threads()   { return List.copyOf(threads); }

    /** Messages for the currently active thread, or empty list. */
    public List<MessageEntity> currentMessages() {
        return messages.getOrDefault(activeThreadId, List.of());
    }
}
