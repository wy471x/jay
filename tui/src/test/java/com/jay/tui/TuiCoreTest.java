package com.jay.tui;

import com.jay.agent.ProviderKind;
import com.jay.config.model.CliRuntimeOverrides;
import com.jay.tui.core.TuiAction;
import com.jay.tui.core.TuiEvent;
import com.jay.tui.core.TuiSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TuiCoreTest {

    // ── TuiSession ────────────────────────────────────────────────────

    @Test
    void sessionDefaultConstructorGeneratesIdAndTime() {
        var session = new TuiSession();
        assertNotNull(session.sessionId());
        assertTrue(session.sessionId().startsWith("tui-"));
        assertNotNull(session.startTime());
        assertFalse(session.startTime().isAfter(Instant.now()));
    }

    @Test
    void sessionCustomValuesRetained() {
        var now = Instant.now();
        var overrides = new CliRuntimeOverrides();
        var session = new TuiSession("my-session", now, "thread-1", overrides);
        assertEquals("my-session", session.sessionId());
        assertEquals(now, session.startTime());
        assertEquals("thread-1", session.initialThreadId());
        assertSame(overrides, session.overrides());
    }

    @Test
    void sessionCompactCanonicalConstructorDefaultsNullValues() {
        var session = new TuiSession(null, null, null, new CliRuntimeOverrides());
        assertNotNull(session.sessionId());
        assertTrue(session.sessionId().startsWith("tui-"));
        assertNotNull(session.startTime());
    }

    // ── TuiAction variants ────────────────────────────────────────────

    @Test
    void actionQuit() {
        TuiAction.Quit q = new TuiAction.Quit();
        assertInstanceOf(TuiAction.class, q);
    }

    @Test
    void actionSendMessage() {
        TuiAction.SendMessage m = new TuiAction.SendMessage("hello");
        assertEquals("hello", m.text());
    }

    @Test
    void actionCancelResponse() {
        TuiAction.CancelResponse c = new TuiAction.CancelResponse();
        assertInstanceOf(TuiAction.class, c);
    }

    @Test
    void actionScrollUp() {
        TuiAction.ScrollUp s = new TuiAction.ScrollUp(3);
        assertEquals(3, s.lines());
    }

    @Test
    void actionScrollDown() {
        TuiAction.ScrollDown s = new TuiAction.ScrollDown(5);
        assertEquals(5, s.lines());
    }

    @Test
    void actionScrollToTop() {
        assertInstanceOf(TuiAction.class, new TuiAction.ScrollToTop());
    }

    @Test
    void actionScrollToBottom() {
        assertInstanceOf(TuiAction.class, new TuiAction.ScrollToBottom());
    }

    @Test
    void actionSwitchThread() {
        TuiAction.SwitchThread s = new TuiAction.SwitchThread("t42");
        assertEquals("t42", s.threadId());
    }

    @Test
    void actionSwitchProvider() {
        TuiAction.SwitchProvider s = new TuiAction.SwitchProvider(ProviderKind.OPENAI);
        assertEquals(ProviderKind.OPENAI, s.provider());
    }

    @Test
    void actionSwitchModel() {
        TuiAction.SwitchModel s = new TuiAction.SwitchModel("gpt-5");
        assertEquals("gpt-5", s.modelName());
    }

    @Test
    void actionToggleSidebar() {
        assertInstanceOf(TuiAction.class, new TuiAction.ToggleSidebar());
    }

    @Test
    void actionOpenSlashMenu() {
        assertInstanceOf(TuiAction.class, new TuiAction.OpenSlashMenu());
    }

    @Test
    void actionExecuteSlashCommand() {
        TuiAction.ExecuteSlashCommand e = new TuiAction.ExecuteSlashCommand("help", List.of("a", "b"));
        assertEquals("help", e.command());
        assertEquals(List.of("a", "b"), e.args());
    }

    @Test
    void actionCopySelection() {
        TuiAction.CopySelection c = new TuiAction.CopySelection("text");
        assertEquals("text", c.text());
    }

    @Test
    void actionResize() {
        TuiAction.Resize r = new TuiAction.Resize(80, 24);
        assertEquals(80, r.columns());
        assertEquals(24, r.rows());
    }

    // ── TuiEvent variants ─────────────────────────────────────────────

    @Test
    void eventInitialized() {
        var event = new TuiEvent.Initialized(
                List.of(), "thread-1", ProviderKind.DEEPSEEK, "model-x");
        assertTrue(event.threads().isEmpty());
        assertEquals("thread-1", event.currentThreadId());
        assertEquals(ProviderKind.DEEPSEEK, event.provider());
        assertEquals("model-x", event.modelName());
    }

    @Test
    void eventShutdownRequested() {
        var event = new TuiEvent.ShutdownRequested("bye");
        assertEquals("bye", event.reason());
    }

    @Test
    void eventResponseDelta() {
        var event = new TuiEvent.ResponseDelta("t1", "hello", null, "r1");
        assertEquals("t1", event.threadId());
        assertEquals("hello", event.delta());
    }

    @Test
    void eventResponseEnd() {
        var event = new TuiEvent.ResponseEnd("t1", "r1");
        assertEquals("t1", event.threadId());
        assertEquals("r1", event.responseId());
    }

    @Test
    void eventToolCallBegin() {
        var event = new TuiEvent.ToolCallBegin("bash", "{\"cmd\":\"ls\"}");
        assertEquals("bash", event.toolName());
        assertEquals("{\"cmd\":\"ls\"}", event.arguments());
    }

    @Test
    void eventToolCallEndSuccess() {
        var event = new TuiEvent.ToolCallEnd("bash", true, "OK");
        assertEquals("bash", event.toolName());
        assertTrue(event.success());
        assertEquals("OK", event.summary());
    }

    @Test
    void eventToolCallEndFailure() {
        var event = new TuiEvent.ToolCallEnd("bash", false, "Command failed");
        assertEquals("bash", event.toolName());
        assertFalse(event.success());
        assertEquals("Command failed", event.summary());
    }

    @Test
    void eventTurnStarted() {
        var event = new TuiEvent.TurnStarted("turn-1");
        assertEquals("turn-1", event.turnId());
    }

    @Test
    void eventTurnComplete() {
        var event = new TuiEvent.TurnComplete("turn-1");
        assertEquals("turn-1", event.turnId());
    }

    @Test
    void eventTurnAborted() {
        var event = new TuiEvent.TurnAborted("turn-1", "cancelled");
        assertEquals("turn-1", event.turnId());
        assertEquals("cancelled", event.reason());
    }

    @Test
    void eventThreadListUpdated() {
        var event = new TuiEvent.ThreadListUpdated(List.of());
        assertNotNull(event.threads());
    }

    @Test
    void eventThreadSwitched() {
        var event = new TuiEvent.ThreadSwitched("t1", List.of());
        assertEquals("t1", event.threadId());
        assertNotNull(event.messages());
    }

    @Test
    void eventError() {
        var event = new TuiEvent.Error("boom");
        assertEquals("boom", event.message());
    }

    @Test
    void eventStatusMessage() {
        var event = new TuiEvent.StatusMessage("Saved", "info");
        assertEquals("Saved", event.text());
        assertEquals("info", event.severity());
    }

    @Test
    void eventSlashResult() {
        var event = new TuiEvent.SlashResult("help", true, "OK");
        assertEquals("help", event.command());
        assertTrue(event.success());
        assertEquals("OK", event.output());
    }

    @Test
    void eventModelChanged() {
        var event = new TuiEvent.ModelChanged("gpt-5", ProviderKind.OPENAI);
        assertEquals("gpt-5", event.modelName());
        assertEquals(ProviderKind.OPENAI, event.provider());
    }

    @Test
    void eventResized() {
        var event = new TuiEvent.Resized(120, 40);
        assertEquals(120, event.columns());
        assertEquals(40, event.rows());
    }

    // ── TuiProperties ──────────────────────────────────────────────────

    @Test
    void tuiPropertiesDefaults() {
        var props = new TuiProperties();
        assertTrue(props.altScreen());
        assertTrue(props.mouseCapture());
        assertEquals("default", props.theme());
        assertEquals(30, props.sidebarWidth());
        assertEquals(50, props.frameDelayMs());
        assertTrue(props.keybindings().isEmpty());
    }

    @Test
    void tuiPropertiesSetters() {
        var props = new TuiProperties();
        props.setAltScreen(false);
        props.setMouseCapture(false);
        props.setTheme("dark");
        props.setSidebarWidth(25);
        props.setFrameDelayMs(100);
        props.setKeybindings(java.util.Map.of("ctrl+x", "quit"));

        assertFalse(props.altScreen());
        assertFalse(props.mouseCapture());
        assertEquals("dark", props.theme());
        assertEquals(25, props.sidebarWidth());
        assertEquals(100, props.frameDelayMs());
        assertEquals(java.util.Map.of("ctrl+x", "quit"), props.keybindings());
    }
}
