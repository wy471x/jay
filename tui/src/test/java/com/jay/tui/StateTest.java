package com.jay.tui;

import com.jay.agent.ProviderKind;
import com.jay.protocol.core.SessionSource;
import com.jay.protocol.core.Thread;
import com.jay.protocol.core.ThreadStatus;
import com.jay.state.model.MessageEntity;
import com.jay.tui.core.TuiEvent;
import com.jay.tui.state.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StateTest {

    /** Minimal Thread for tests. */
    private static Thread thread(String id, String preview) {
        return new Thread(id, preview, false, null,
                0L, 0L, ThreadStatus.IDLE, null, null,
                null, SessionSource.UNKNOWN, null);
    }

    /** Minimal MessageEntity for tests. */
    private static MessageEntity message(String role, String content, String threadId) {
        return new MessageEntity(null, threadId, role, content, null,
                System.currentTimeMillis(), null);
    }

    // ── AppState ──────────────────────────────────────────────────────

    @Nested
    class AppStateTests {

        @Test
        void initialStateNotInitialized() {
            var state = new AppState();
            assertFalse(state.initialized());
            assertFalse(state.shuttingDown());
        }

        @Test
        void applyInitializedSetsUpState() {
            var state = new AppState();
            var thread = thread("t1", "Test");
            state.apply(new TuiEvent.Initialized(
                    List.of(thread), "t1", ProviderKind.OPENAI, "gpt-5"));
            assertTrue(state.initialized());
            assertEquals("t1", state.activeThreadId());
            assertEquals("gpt-5", state.currentModel());
            assertEquals(ProviderKind.OPENAI, state.currentProvider());
        }

        @Test
        void applyShutdownRequested() {
            var state = new AppState();
            state.apply(new TuiEvent.ShutdownRequested("quit"));
            assertTrue(state.shuttingDown());
        }

        @Test
        void applyToolCallBeginStartsSpinner() {
            var state = new AppState();
            state.apply(new TuiEvent.ToolCallBegin("bash", "{}"));
            assertTrue(state.statusBar().spinner());
        }

        @Test
        void applyToolCallEndStopsSpinner() {
            var state = new AppState();
            state.apply(new TuiEvent.ToolCallBegin("bash", "{}"));
            state.apply(new TuiEvent.ToolCallEnd("bash", true, null));
            assertFalse(state.statusBar().spinner());
        }

        @Test
        void applyTurnStartedStartsSpinner() {
            var state = new AppState();
            state.apply(new TuiEvent.TurnStarted("turn-1"));
            assertTrue(state.statusBar().spinner());
        }

        @Test
        void applyTurnCompleteStopsSpinner() {
            var state = new AppState();
            state.apply(new TuiEvent.TurnStarted("turn-1"));
            state.apply(new TuiEvent.TurnComplete("turn-1"));
            assertFalse(state.statusBar().spinner());
        }

        @Test
        void applyTurnAbortedStopsSpinner() {
            var state = new AppState();
            state.apply(new TuiEvent.TurnStarted("turn-1"));
            state.apply(new TuiEvent.TurnAborted("turn-1", "cancelled"));
            assertFalse(state.statusBar().spinner());
        }

        @Test
        void applyThreadListUpdatedReplacesThreads() {
            var state = new AppState();
            var t1 = thread("t1", "One");
            var t2 = thread("t2", "Two");
            state.apply(new TuiEvent.ThreadListUpdated(List.of(t1, t2)));
            assertEquals(2, state.threads().size());
        }

        @Test
        void applyThreadSwitchedUpdatesActiveAndMessages() {
            var state = new AppState();
            var msg = message("user", "hello", "t1");
            state.apply(new TuiEvent.ThreadSwitched("t1", List.of(msg)));
            assertEquals("t1", state.activeThreadId());
            assertEquals(1, state.currentMessages().size());
            assertEquals("hello", state.currentMessages().get(0).content());
        }

        @Test
        void applyErrorSetsStatusBar() {
            var state = new AppState();
            state.apply(new TuiEvent.Error("Something went wrong"));
            assertEquals("Something went wrong", state.statusBar().statusMessage());
        }

        @Test
        void applyStatusMessageSetsStatusBar() {
            var state = new AppState();
            state.apply(new TuiEvent.StatusMessage("Saved", "info"));
            assertEquals("Saved", state.statusBar().statusMessage());
        }

        @Test
        void applyModelChangedUpdatesModelAndProvider() {
            var state = new AppState();
            state.apply(new TuiEvent.ModelChanged("claude-4", ProviderKind.ANTHROPIC));
            assertEquals("claude-4", state.currentModel());
            assertEquals(ProviderKind.ANTHROPIC, state.currentProvider());
        }

        @Test
        void applyResizedDoesNotThrow() {
            var state = new AppState();
            assertDoesNotThrow(() -> state.apply(new TuiEvent.Resized(80, 24)));
        }

        @Test
        void currentMessagesEmptyForUnknownThread() {
            var state = new AppState();
            assertTrue(state.currentMessages().isEmpty());
        }

        @Test
        void threadsReturnsDefensiveCopy() {
            var state = new AppState();
            assertThrows(UnsupportedOperationException.class,
                    () -> state.threads().add(thread("x", "X")));
        }
    }

    // ── ComposerState ─────────────────────────────────────────────────

    @Nested
    class ComposerStateTests {

        @Test
        void insertCharAppendsToBuffer() {
            var cs = new ComposerState();
            cs.insertChar('a');
            cs.insertChar('b');
            assertEquals("ab", cs.text());
            assertEquals(2, cs.cursor());
        }

        @Test
        void insertCharAtCursorPosition() {
            var cs = new ComposerState();
            cs.insertChar('a');
            cs.insertChar('c');
            cs.moveCursorLeft();
            cs.insertChar('b');
            assertEquals("abc", cs.text());
            assertEquals(2, cs.cursor());
        }

        @Test
        void deleteBeforeRemovesPreviousChar() {
            var cs = new ComposerState();
            cs.insertChar('a');
            cs.insertChar('b');
            cs.deleteBefore();
            assertEquals("a", cs.text());
            assertEquals(1, cs.cursor());
        }

        @Test
        void deleteBeforeAtStartDoesNothing() {
            var cs = new ComposerState();
            cs.insertChar('a');
            cs.moveCursorHome();
            cs.deleteBefore();
            assertEquals("a", cs.text());
        }

        @Test
        void deleteAfterRemovesCharAtCursor() {
            var cs = new ComposerState();
            cs.insertChar('a');
            cs.insertChar('b');
            cs.moveCursorHome();
            cs.deleteAfter();
            assertEquals("b", cs.text());
        }

        @Test
        void deleteAfterAtEndDoesNothing() {
            var cs = new ComposerState();
            cs.insertChar('a');
            cs.deleteAfter();
            assertEquals("a", cs.text());
        }

        @Test
        void cursorMovement() {
            var cs = new ComposerState();
            cs.insertChar('a');
            cs.insertChar('b');
            cs.insertChar('c');
            cs.moveCursorLeft();
            assertEquals(2, cs.cursor());
            cs.moveCursorRight();
            assertEquals(3, cs.cursor());
            cs.moveCursorHome();
            assertEquals(0, cs.cursor());
            cs.moveCursorEnd();
            assertEquals(3, cs.cursor());
        }

        @Test
        void commitClearsBufferAndReturnsText() {
            var cs = new ComposerState();
            cs.insertChar('h');
            cs.insertChar('i');
            String result = cs.commit();
            assertEquals("hi", result);
            assertEquals("", cs.text());
            assertEquals(0, cs.cursor());
        }

        @Test
        void commitBlankTextNotAddedToHistory() {
            var cs = new ComposerState();
            cs.insertChar(' ');
            cs.commit();
            String prev = cs.historyPrev();
            assertEquals("", prev);
        }

        @Test
        void historyNavigation() {
            var cs = new ComposerState();
            cs.insertChar('f'); cs.insertChar('o'); cs.insertChar('o');
            cs.commit();
            cs.insertChar('b'); cs.insertChar('a'); cs.insertChar('r');
            cs.commit();

            assertEquals("bar", cs.historyPrev());
            assertEquals("foo", cs.historyPrev());
            // At last entry, historyPrev returns same
            String last = cs.historyPrev();
            assertEquals("foo", last);
        }

        @Test
        void historyNextReturnsToBuffer() {
            var cs = new ComposerState();
            cs.insertChar('f'); cs.insertChar('o'); cs.insertChar('o');
            cs.commit();

            cs.historyPrev(); // go to "foo"
            assertEquals("foo", cs.text());
            cs.historyNext(); // back to empty
            assertEquals("", cs.text());
        }

        @Test
        void historyLimitEnforced() {
            var cs = new ComposerState();
            for (int i = 0; i < 150; i++) {
                cs.insertChar((char) ('a' + (i % 26)));
                cs.commit();
            }
            // Should not throw; max 100 entries
            assertDoesNotThrow(() -> {
                for (int i = 0; i < 120; i++) cs.historyPrev();
            });
        }

        @Test
        void slashDetectedOnFirstChar() {
            var cs = new ComposerState();
            cs.insertChar('/');
            assertTrue(cs.slashActive());
            assertEquals("", cs.slashFilter());
        }

        @Test
        void slashFilterUpdatedOnSetText() {
            var cs = new ComposerState();
            cs.setText("/he"); // setText triggers updateSlashState
            assertTrue(cs.slashActive());
            assertEquals("he", cs.slashFilter());
        }

        @Test
        void slashDeactivatedOnDelete() {
            var cs = new ComposerState();
            cs.insertChar('/');
            cs.insertChar('x');
            cs.deleteBefore();
            assertTrue(cs.slashActive());
            cs.deleteBefore();
            assertFalse(cs.slashActive());
        }

        @Test
        void historySearchActive() {
            var cs = new ComposerState();
            assertFalse(cs.historySearchActive());
            cs.startHistorySearch();
            assertTrue(cs.historySearchActive());
        }

        @Test
        void historySearchFindsMatch() {
            var cs = new ComposerState();
            cs.insertChar('h'); cs.insertChar('e'); cs.insertChar('l'); cs.insertChar('l'); cs.insertChar('o');
            cs.commit();

            cs.startHistorySearch();
            cs.historySearchAppend('h');
            cs.historySearchAppend('e');
            assertEquals("hello", cs.findNextMatch());
        }

        @Test
        void historySearchBackspace() {
            var cs = new ComposerState();
            cs.insertChar('a'); cs.insertChar('a'); cs.insertChar('a');
            cs.commit();
            cs.insertChar('b'); cs.insertChar('b'); cs.insertChar('b');
            cs.commit();

            cs.startHistorySearch();
            cs.historySearchAppend('z');
            assertEquals("", cs.findNextMatch());
            cs.historySearchBackspace();
            // After backspace, re-searches
        }

        @Test
        void acceptHistorySearchClearsSearchMode() {
            var cs = new ComposerState();
            cs.insertChar('h'); cs.insertChar('i');
            cs.commit();

            cs.startHistorySearch();
            cs.historySearchAppend('h');
            cs.acceptHistorySearch();
            assertFalse(cs.historySearchActive());
        }

        @Test
        void cancelHistorySearchClearsState() {
            var cs = new ComposerState();
            cs.startHistorySearch();
            cs.cancelHistorySearch();
            assertFalse(cs.historySearchActive());
        }

        @Test
        void escapeClosesSlashMenu() {
            var cs = new ComposerState();
            cs.insertChar('/');
            cs.insertChar('h');
            var result = cs.handleEscape();
            assertEquals(ComposerState.EscapeAction.CLOSE_SLASH_MENU, result);
            assertFalse(cs.slashActive());
        }

        @Test
        void escapeDiscardsDraft() {
            var cs = new ComposerState();
            cs.insertChar('a');
            var result = cs.handleEscape();
            assertEquals(ComposerState.EscapeAction.DISCARD_DRAFT, result);
            assertEquals("", cs.text());
        }

        @Test
        void escapeNoOpWhenEmpty() {
            var cs = new ComposerState();
            assertEquals(ComposerState.EscapeAction.NOOP, cs.handleEscape());
        }

        @Test
        void insertNewline() {
            var cs = new ComposerState();
            cs.insertChar('a');
            cs.insertNewline();
            cs.insertChar('b');
            assertEquals("a\nb", cs.text());
            assertTrue(cs.isMultiline());
            assertEquals(3, cs.cursor());
        }

        @Test
        void setTextReplacesBuffer() {
            var cs = new ComposerState();
            cs.insertChar('o'); cs.insertChar('l'); cs.insertChar('d');
            cs.setText("new");
            assertEquals("new", cs.text());
            assertEquals(3, cs.cursor());
        }

        @Test
        void composingFlag() {
            var cs = new ComposerState();
            assertFalse(cs.composing());
            cs.composing(true);
            assertTrue(cs.composing());
        }
    }

    // ── SidebarState ──────────────────────────────────────────────────

    @Nested
    class SidebarStateTests {

        @Test
        void toggleVisibility() {
            var sb = new SidebarState();
            assertFalse(sb.visible());
            sb.toggle();
            assertTrue(sb.visible());
            sb.toggle();
            assertFalse(sb.visible());
        }

        @Test
        void showAndHide() {
            var sb = new SidebarState();
            sb.hide();
            assertFalse(sb.visible());
            sb.show();
            assertTrue(sb.visible());
        }

        @Test
        void widthClamped() {
            var sb = new SidebarState();
            sb.width(10);
            assertEquals(20, sb.width());
            sb.width(60);
            assertEquals(50, sb.width());
            sb.width(30);
            assertEquals(30, sb.width());
        }

        @Test
        void selectNextAndPrev() {
            var sb = new SidebarState();
            var t1 = thread("t1", "A");
            var t2 = thread("t2", "B");
            sb.setThreads(List.of(t1, t2));

            assertEquals(0, sb.selectedIndex());
            sb.selectNext();
            assertEquals(1, sb.selectedIndex());
            sb.selectNext();
            assertEquals(1, sb.selectedIndex()); // clamped
            sb.selectPrev();
            assertEquals(0, sb.selectedIndex());
            sb.selectPrev();
            assertEquals(0, sb.selectedIndex()); // clamped
        }

        @Test
        void selectedThreadIdReturnsNullWhenEmpty() {
            var sb = new SidebarState();
            assertNull(sb.selectedThreadId());
        }

        @Test
        void selectedThreadIdMatches() {
            var sb = new SidebarState();
            var t = thread("t42", "Hi");
            sb.setThreads(List.of(t));
            assertEquals("t42", sb.selectedThreadId());
        }

        @Test
        void scrollOffset() {
            var sb = new SidebarState();
            sb.scrollOffset(5);
            assertEquals(5, sb.scrollOffset());
            sb.scrollOffset(-1);
            assertEquals(0, sb.scrollOffset());
        }
    }

    // ── StatusBarState ────────────────────────────────────────────────

    @Nested
    class StatusBarStateTests {

        @Test
        void setModelInfo() {
            var sbs = new StatusBarState();
            sbs.setModelInfo("claude-4", ProviderKind.ANTHROPIC);
            assertEquals("claude-4", sbs.modelName());
            assertEquals(ProviderKind.ANTHROPIC, sbs.provider());
        }

        @Test
        void setStatusWithSeverity() {
            var sbs = new StatusBarState();
            sbs.setStatus("hello", StatusBarState.Severity.WARN);
            assertEquals("hello", sbs.statusMessage());
            assertEquals(StatusBarState.Severity.WARN, sbs.severity());
        }

        @Test
        void statusNotImmediatelyExpired() {
            var sbs = new StatusBarState();
            sbs.setStatus("hi", StatusBarState.Severity.INFO);
            assertFalse(sbs.hasExpired());
        }

        @Test
        void clearStatus() {
            var sbs = new StatusBarState();
            sbs.setStatus("hi", StatusBarState.Severity.INFO);
            sbs.clearStatus();
            assertEquals("", sbs.statusMessage());
            assertFalse(sbs.hasExpired());
        }

        @Test
        void spinnerToggle() {
            var sbs = new StatusBarState();
            assertFalse(sbs.spinner());
            sbs.setSpinner(true);
            assertTrue(sbs.spinner());
            sbs.setSpinner(false);
            assertFalse(sbs.spinner());
        }

        @Test
        void messageCount() {
            var sbs = new StatusBarState();
            sbs.setMessageCount(5);
            assertEquals(5, sbs.messageCount());
        }
    }

    // ── ViewportState ─────────────────────────────────────────────────

    @Nested
    class ViewportStateTests {

        @Test
        void scrollUp() {
            var vs = new ViewportState();
            vs.setContentHeight(100);
            vs.setViewportHeight(20);
            vs.scrollTo(10);
            vs.scrollUp(5);
            assertEquals(5, vs.scrollOffset());
            assertFalse(vs.followBottom());
        }

        @Test
        void scrollUpClampedToZero() {
            var vs = new ViewportState();
            vs.scrollTo(2);
            vs.scrollUp(10);
            assertEquals(0, vs.scrollOffset());
        }

        @Test
        void scrollDown() {
            var vs = new ViewportState();
            vs.setContentHeight(100);
            vs.setViewportHeight(20);
            vs.scrollDown(10);
            assertEquals(10, vs.scrollOffset());
        }

        @Test
        void scrollDownClampedToMax() {
            var vs = new ViewportState();
            vs.setContentHeight(100);
            vs.setViewportHeight(20);
            vs.scrollDown(200);
            assertEquals(80, vs.scrollOffset());
        }

        @Test
        void scrollDownToBottomEnablesFollow() {
            var vs = new ViewportState();
            vs.setContentHeight(100);
            vs.setViewportHeight(20);
            vs.scrollUp(5);
            vs.scrollDown(200); // hits bottom
            assertTrue(vs.followBottom());
        }

        @Test
        void scrollToTop() {
            var vs = new ViewportState();
            vs.scrollTo(50);
            vs.scrollToTop();
            assertEquals(0, vs.scrollOffset());
            assertFalse(vs.followBottom());
        }

        @Test
        void scrollToBottom() {
            var vs = new ViewportState();
            vs.setContentHeight(100);
            vs.setViewportHeight(20);
            vs.scrollToBottom();
            assertEquals(80, vs.scrollOffset());
            assertTrue(vs.followBottom());
        }

        @Test
        void onContentChangedFollowsBottom() {
            var vs = new ViewportState();
            vs.setContentHeight(50);
            vs.setViewportHeight(20);
            vs.scrollToBottom();
            vs.onContentChanged(100, 20);
            assertEquals(80, vs.scrollOffset());
        }

        @Test
        void onContentChangedDoesNotFollowWhenScrolledUp() {
            var vs = new ViewportState();
            vs.setContentHeight(100);
            vs.setViewportHeight(20);
            vs.scrollUp(10);
            assertFalse(vs.followBottom());
            vs.onContentChanged(200, 20);
            // offset unchanged since not following
            assertEquals(0, vs.scrollOffset()); // was scrolled up from 10 to 0
        }

        @Test
        void scrollTo() {
            var vs = new ViewportState();
            vs.scrollTo(42);
            assertEquals(42, vs.scrollOffset());
        }
    }
}
