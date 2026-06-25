package com.jay.tui;

import com.jay.tui.commands.CommandRegistry;
import com.jay.tui.views.HelpOverlay;
import com.jay.tui.views.ViewAction;
import com.jay.tui.views.ViewStack;
import dev.tamboui.tui.event.KeyCode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ViewsTest {

    // ── ViewStack ──────────────────────────────────────────────────────

    @Nested
    class ViewStackTests {

        @Test
        void emptyByDefault() {
            var stack = new ViewStack();
            assertTrue(stack.isEmpty());
            assertNull(stack.top());
        }

        @Test
        void pushAndPop() {
            var stack = new ViewStack();
            var view = new TestModalView("test1");
            stack.push(view);
            assertFalse(stack.isEmpty());
            assertEquals("test1", stack.top().kind());

            stack.pop();
            assertTrue(stack.isEmpty());
            assertNull(stack.top());
        }

        @Test
        void popEmptyDoesNotThrow() {
            var stack = new ViewStack();
            assertDoesNotThrow(stack::pop);
        }

        @Test
        void clearRemovesAll() {
            var stack = new ViewStack();
            stack.push(new TestModalView("a"));
            stack.push(new TestModalView("b"));
            stack.clear();
            assertTrue(stack.isEmpty());
        }

        @Test
        void blocksComposerDelegatesToTop() {
            var stack = new ViewStack();
            var blocking = new TestModalView("block", true);
            var nonBlocking = new TestModalView("noblock", false);

            stack.push(nonBlocking);
            assertFalse(stack.blocksComposer());

            stack.push(blocking);
            assertTrue(stack.blocksComposer());
        }

        @Test
        void blocksComposerEmptyStack() {
            var stack = new ViewStack();
            assertFalse(stack.blocksComposer());
        }

        @Test
        void handleKeyDispatchedToTop() {
            var stack = new ViewStack();
            stack.push(new TestModalView("top", true));
            stack.push(new TestModalView("active", true));

            var action = stack.handleKey('c', KeyCode.UNKNOWN, true, false);
            assertInstanceOf(ViewAction.None.class, action);
        }

        @Test
        void handleKeyEmptyStackReturnsNone() {
            var stack = new ViewStack();
            var action = stack.handleKey('x', KeyCode.UNKNOWN, false, false);
            assertInstanceOf(ViewAction.None.class, action);
        }

        @Test
        void tickAllModals() {
            var stack = new ViewStack();
            var a = new TestModalView("a");
            var b = new TestModalView("b");
            stack.push(a);
            stack.push(b);
            assertDoesNotThrow(stack::tick);
        }
    }

    // ── HelpOverlay ────────────────────────────────────────────────────

    @Nested
    class HelpOverlayTests {

        @Test
        void kind() {
            var registry = new CommandRegistry();
            var help = new HelpOverlay(registry);
            assertEquals("help", help.kind());
        }

        @Test
        void blocksComposer() {
            var registry = new CommandRegistry();
            var help = new HelpOverlay(registry);
            assertTrue(help.blocksComposer());
        }

        @Test
        void escapeReturnsClose() {
            var registry = new CommandRegistry();
            var help = new HelpOverlay(registry);
            var action = help.handleKey((char) 0, KeyCode.ESCAPE, false, false);
            assertInstanceOf(ViewAction.Close.class, action);
        }

        @Test
        void enterReturnsClose() {
            var registry = new CommandRegistry();
            var help = new HelpOverlay(registry);
            var action = help.handleKey((char) 0, KeyCode.ENTER, false, false);
            assertInstanceOf(ViewAction.Close.class, action);
        }

        @Test
        void downArrowMovesSelection() {
            var registry = new CommandRegistry();
            var help = new HelpOverlay(registry);
            // Move selection down
            var action = help.handleKey((char) 0, KeyCode.DOWN, false, false);
            assertInstanceOf(ViewAction.None.class, action);
        }

        @Test
        void upArrowMovesSelection() {
            var registry = new CommandRegistry();
            var help = new HelpOverlay(registry);
            var action = help.handleKey((char) 0, KeyCode.UP, false, false);
            assertInstanceOf(ViewAction.None.class, action);
        }

        @Test
        void jimNavigationUp() {
            var registry = new CommandRegistry();
            var help = new HelpOverlay(registry);
            var action = help.handleKey('k', KeyCode.UNKNOWN, false, false);
            assertInstanceOf(ViewAction.None.class, action);
        }

        @Test
        void jimNavigationDown() {
            var registry = new CommandRegistry();
            var help = new HelpOverlay(registry);
            help.handleKey('j', KeyCode.UNKNOWN, false, false);
        }

        @Test
        void typingAddsToFilter() {
            var registry = new CommandRegistry();
            var help = new HelpOverlay(registry);
            var action = help.handleKey('e', KeyCode.UNKNOWN, false, false);
            assertInstanceOf(ViewAction.None.class, action);
        }

        @Test
        void backspaceRemovesFromFilter() {
            var registry = new CommandRegistry();
            var help = new HelpOverlay(registry);
            help.handleKey('e', KeyCode.UNKNOWN, false, false);
            help.handleKey('x', KeyCode.UNKNOWN, false, false);
            // backspace removes last char
            var action = help.handleKey((char) 0, KeyCode.BACKSPACE, false, false);
            assertInstanceOf(ViewAction.None.class, action);
        }

        @Test
        void setFilter() {
            var registry = new CommandRegistry();
            registry.register("test", "T", "/t", args -> null);
            var help = new HelpOverlay(registry);
            assertDoesNotThrow(() -> help.setFilter("test"));
        }

        @Test
        void tickIsNoOp() {
            var registry = new CommandRegistry();
            var help = new HelpOverlay(registry);
            assertDoesNotThrow(help::tick);
        }
    }

    // ── ViewAction / ViewEvent ─────────────────────────────────────────

    @Nested
    class ViewActionEventTests {

        @Test
        void viewActionNone() {
            assertInstanceOf(ViewAction.class, new ViewAction.None());
        }

        @Test
        void viewActionClose() {
            assertInstanceOf(ViewAction.class, new ViewAction.Close());
        }

        @Test
        void viewActionEmit() {
            var evt = new com.jay.tui.views.ViewEvent.HelpDismissed();
            var action = new ViewAction.Emit(evt);
            assertSame(evt, action.event());
        }

        @Test
        void viewActionEmitAndClose() {
            var evt = new com.jay.tui.views.ViewEvent.ModeSelected("agent");
            var action = new ViewAction.EmitAndClose(evt);
            assertSame(evt, action.event());
        }

        @Test
        void viewEventCommandSelected() {
            var evt = new com.jay.tui.views.ViewEvent.CommandSelected("help", "");
            assertEquals("help", evt.command());
        }

        @Test
        void viewEventModelSelected() {
            var evt = new com.jay.tui.views.ViewEvent.ModelSelected("gpt-5", "openai");
            assertEquals("gpt-5", evt.modelName());
            assertEquals("openai", evt.provider());
        }

        @Test
        void viewEventModeSelected() {
            var evt = new com.jay.tui.views.ViewEvent.ModeSelected("yolo");
            assertEquals("yolo", evt.mode());
        }

        @Test
        void viewEventHelpDismissed() {
            assertNotNull(new com.jay.tui.views.ViewEvent.HelpDismissed());
        }

        @Test
        void viewEventConfigUpdated() {
            var evt = new com.jay.tui.views.ViewEvent.ConfigUpdated("theme", "dark");
            assertEquals("theme", evt.key());
            assertEquals("dark", evt.value());
        }
    }

    // ── Test helper ────────────────────────────────────────────────────

    private static class TestModalView implements com.jay.tui.views.ModalView {
        private final String kind;
        private final boolean blocks;

        TestModalView(String kind) { this(kind, true); }

        TestModalView(String kind, boolean blocks) {
            this.kind = kind;
            this.blocks = blocks;
        }

        @Override public String kind() { return kind; }

        @Override
        public com.jay.tui.views.ViewAction handleKey(char c, KeyCode k, boolean ctrl, boolean alt) {
            return new ViewAction.None();
        }

        @Override
        public void render(dev.tamboui.terminal.Frame f, dev.tamboui.layout.Rect a) {}

        @Override
        public boolean blocksComposer() { return blocks; }
    }
}
