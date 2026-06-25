package com.jay.tui;

import com.jay.tui.commands.CommandRegistry;
import com.jay.tui.core.TuiAction;
import com.jay.tui.input.CommandPalette;
import com.jay.tui.input.InputHandler;
import com.jay.tui.input.KeyDispatcher;
import com.jay.tui.input.SlashMenu;
import com.jay.tui.state.AppState;
import com.jay.tui.state.ComposerState;
import dev.tamboui.tui.event.KeyCode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InputTest {

    // ── KeyDispatcher ──────────────────────────────────────────────────

    @Nested
    class KeyDispatcherTests {

        private final KeyDispatcher dispatcher = new KeyDispatcher();

        @Test
        void ctrlC_isQuit() {
            var action = dispatcher.dispatch(KeyCode.UNKNOWN, 'c', true, false);
            assertTrue(action.isPresent());
            assertInstanceOf(TuiAction.Quit.class, action.get());
        }

        @Test
        void ctrlD_isQuit() {
            var action = dispatcher.dispatch(KeyCode.UNKNOWN, 'd', true, false);
            assertTrue(action.isPresent());
            assertInstanceOf(TuiAction.Quit.class, action.get());
        }

        @Test
        void ctrlL_isScrollToBottom() {
            var action = dispatcher.dispatch(KeyCode.UNKNOWN, 'l', true, false);
            assertTrue(action.isPresent());
            assertInstanceOf(TuiAction.ScrollToBottom.class, action.get());
        }

        @Test
        void escape_isCancelResponse() {
            var action = dispatcher.dispatch(KeyCode.ESCAPE, (char) 0, false, false);
            assertTrue(action.isPresent());
            assertInstanceOf(TuiAction.CancelResponse.class, action.get());
        }

        @Test
        void enter_isSendMessage() {
            var action = dispatcher.dispatch(KeyCode.ENTER, (char) 0, false, false);
            assertTrue(action.isPresent());
            assertInstanceOf(TuiAction.SendMessage.class, action.get());
        }

        @Test
        void up_isScrollUp() {
            var action = dispatcher.dispatch(KeyCode.UP, (char) 0, false, false);
            assertTrue(action.isPresent());
            var scroll = (TuiAction.ScrollUp) action.get();
            assertEquals(1, scroll.lines());
        }

        @Test
        void down_isScrollDown() {
            var action = dispatcher.dispatch(KeyCode.DOWN, (char) 0, false, false);
            assertTrue(action.isPresent());
            var scroll = (TuiAction.ScrollDown) action.get();
            assertEquals(1, scroll.lines());
        }

        @Test
        void pageUp_isScrollUp10() {
            var action = dispatcher.dispatch(KeyCode.PAGE_UP, (char) 0, false, false);
            assertTrue(action.isPresent());
            var scroll = (TuiAction.ScrollUp) action.get();
            assertEquals(10, scroll.lines());
        }

        @Test
        void pageDown_isScrollDown10() {
            var action = dispatcher.dispatch(KeyCode.PAGE_DOWN, (char) 0, false, false);
            assertTrue(action.isPresent());
            var scroll = (TuiAction.ScrollDown) action.get();
            assertEquals(10, scroll.lines());
        }

        @Test
        void tab_isToggleSidebar() {
            var action = dispatcher.dispatch(KeyCode.TAB, (char) 0, false, false);
            assertTrue(action.isPresent());
            assertInstanceOf(TuiAction.ToggleSidebar.class, action.get());
        }

        @Test
        void regularCharacterReturnsEmpty() {
            var action = dispatcher.dispatch(KeyCode.UNKNOWN, 'x', false, false);
            assertFalse(action.isPresent());
        }

        @Test
        void isPrintableBoundaries() {
            assertFalse(KeyDispatcher.isPrintable((char) 31)); // control char
            assertTrue(KeyDispatcher.isPrintable('A'));
            assertTrue(KeyDispatcher.isPrintable((char) 126)); // ~
            assertFalse(KeyDispatcher.isPrintable((char) 127)); // DEL
            assertFalse(KeyDispatcher.isPrintable((char) 10)); // LF
        }
    }

    // ── InputHandler ───────────────────────────────────────────────────

    @Nested
    class InputHandlerTests {

        private final ComposerState composer = new ComposerState();
        private final InputHandler handler = new InputHandler(composer);

        @Test
        void typeCharactersAppendToComposer() {
            handler.handleInput(KeyCode.UNKNOWN, 'h', false, false);
            handler.handleInput(KeyCode.UNKNOWN, 'i', false, false);
            assertEquals("hi", composer.text());
        }

        @Test
        void enterCommitsAndReturnsSendMessage() {
            handler.handleInput(KeyCode.UNKNOWN, 'h', false, false);
            handler.handleInput(KeyCode.UNKNOWN, 'i', false, false);
            var action = handler.handleInput(KeyCode.ENTER, (char) 0, false, false);
            assertTrue(action.isPresent());
            var msg = (TuiAction.SendMessage) action.get();
            assertEquals("hi", msg.text());
            assertEquals("", composer.text()); // committed
        }

        @Test
        void enterOnEmptyReturnsEmpty() {
            var action = handler.handleInput(KeyCode.ENTER, (char) 0, false, false);
            assertFalse(action.isPresent());
        }

        @Test
        void backspaceDeletesChar() {
            handler.handleInput(KeyCode.UNKNOWN, 'a', false, false);
            handler.handleInput(KeyCode.UNKNOWN, 'b', false, false);
            handler.handleInput(KeyCode.BACKSPACE, (char) 0, false, false);
            assertEquals("a", composer.text());
        }

        @Test
        void ctrlCReturnsQuit() {
            var action = handler.handleInput(KeyCode.UNKNOWN, 'c', true, false);
            assertTrue(action.isPresent());
            assertInstanceOf(TuiAction.Quit.class, action.get());
        }

        @Test
        void tabReturnsToggleSidebar() {
            var action = handler.handleInput(KeyCode.TAB, (char) 0, false, false);
            assertTrue(action.isPresent());
            assertInstanceOf(TuiAction.ToggleSidebar.class, action.get());
        }

        @Test
        void ctrlCharactersNotTreatedAsTyping() {
            handler.handleInput(KeyCode.UNKNOWN, 'x', true, false);
            assertEquals("", composer.text()); // not appended
        }
    }

    // ── SlashMenu ──────────────────────────────────────────────────────

    @Nested
    class SlashMenuTests {

        @Test
        void updateFilterFiltersCommands() {
            var registry = new CommandRegistry();
            registry.register("help", "Show help", "/help", args -> null);
            registry.register("exit", "Exit", "/exit", args -> null);
            registry.register("history", "History", "/history", args -> null);

            var menu = new SlashMenu(registry);
            var state = new AppState();
            state.composer().setText("/h"); // setText triggers updateSlashState

            menu.updateFilter(state);
            var filtered = menu.getFilteredCommands();
            assertEquals(2, filtered.size()); // help, history
            assertTrue(filtered.contains("help"));
            assertTrue(filtered.contains("history"));
        }

        @Test
        void updateFilterEmptyWhenNotSlashActive() {
            var registry = new CommandRegistry();
            registry.register("help", "Help", "/help", args -> null);

            var menu = new SlashMenu(registry);
            var state = new AppState();
            // no slash
            menu.updateFilter(state);
            assertTrue(menu.getFilteredCommands().isEmpty());
        }

        @Test
        void selectNextAndPrevCycle() {
            var registry = new CommandRegistry();
            registry.register("a", "A", "/a", args -> null);
            registry.register("b", "B", "/b", args -> null);

            var menu = new SlashMenu(registry);
            var state = new AppState();
            state.composer().insertChar('/');
            menu.updateFilter(state);

            assertEquals(0, menu.selectedIndex());
            menu.selectNext();
            assertEquals(1, menu.selectedIndex());
            menu.selectNext();
            assertEquals(0, menu.selectedIndex()); // wraps
            menu.selectPrev();
            assertEquals(1, menu.selectedIndex());
        }

        @Test
        void getSelectedActionReturnsExecuteSlashCommand() {
            var registry = new CommandRegistry();
            registry.register("help", "Help", "/help", args -> null);

            var menu = new SlashMenu(registry);
            var state = new AppState();
            state.composer().insertChar('/');
            menu.updateFilter(state);

            var action = menu.getSelectedAction();
            assertTrue(action.isPresent());
            assertInstanceOf(TuiAction.ExecuteSlashCommand.class, action.get());
            assertEquals("help", ((TuiAction.ExecuteSlashCommand) action.get()).command());
        }

        @Test
        void getSelectedActionEmptyWhenNoMatch() {
            var registry = new CommandRegistry();
            var menu = new SlashMenu(registry);
            // no updateFilter call
            assertFalse(menu.getSelectedAction().isPresent());
        }

        @Test
        void selectedIndexClampedWhenFilterShrinks() {
            var registry = new CommandRegistry();
            registry.register("a", "A", "/a", args -> null);
            registry.register("b", "B", "/b", args -> null);
            registry.register("c", "C", "/c", args -> null);

            var menu = new SlashMenu(registry);
            var state = new AppState();
            state.composer().setText("/"); // setText triggers updateSlashState
            menu.updateFilter(state);
            menu.selectNext();
            menu.selectNext(); // selectedIndex = 2

            // Now filter to "/z" — none match, so filtered is empty
            state.composer().setText("/z");
            menu.updateFilter(state);
            assertEquals(0, menu.selectedIndex());
        }

        @Test
        void isActiveWhenCommandsMatch() {
            var registry = new CommandRegistry();
            registry.register("test", "T", "/t", args -> null);
            var menu = new SlashMenu(registry);
            var state = new AppState();
            state.composer().setText("/t"); // setText triggers updateSlashState
            menu.updateFilter(state);
            assertTrue(menu.isActive());
        }
    }

    // ── CommandPalette ──────────────────────────────────────────────────

    @Nested
    class CommandPaletteTests {

        @Test
        void showMakesVisible() {
            var registry = new CommandRegistry();
            registry.register("test", "T", "/t", args -> null);
            var palette = new CommandPalette(registry);
            assertFalse(palette.isVisible());
            palette.show();
            assertTrue(palette.isVisible());
        }

        @Test
        void hideClearsVisibility() {
            var registry = new CommandRegistry();
            var palette = new CommandPalette(registry);
            palette.show();
            palette.hide();
            assertFalse(palette.isVisible());
        }

        @Test
        void appendQueryFilters() {
            var registry = new CommandRegistry();
            registry.register("help", "Help", "/help", args -> null);
            registry.register("exit", "Exit", "/exit", args -> null);

            var palette = new CommandPalette(registry);
            palette.show();
            palette.appendQuery('h');
            var entry = palette.selectedEntry();
            assertNotNull(entry);
            assertEquals("help", entry.label());
        }

        @Test
        void escapeHides() {
            var registry = new CommandRegistry();
            var palette = new CommandPalette(registry);
            palette.show();
            palette.handleKey((char) 0, KeyCode.ESCAPE, false, false);
            assertFalse(palette.isVisible());
        }

        @Test
        void enterHides() {
            var registry = new CommandRegistry();
            registry.register("a", "A", "/a", args -> null);
            var palette = new CommandPalette(registry);
            palette.show();
            palette.handleKey((char) 0, KeyCode.ENTER, false, false);
            assertFalse(palette.isVisible());
        }

        @Test
        void upDownNavigate() {
            var registry = new CommandRegistry();
            registry.register("a", "A", "/a", args -> null);
            registry.register("b", "B", "/b", args -> null);
            var palette = new CommandPalette(registry);
            palette.show();

            palette.handleKey((char) 0, KeyCode.DOWN, false, false);
            assertEquals("b", palette.selectedEntry().label());

            palette.handleKey((char) 0, KeyCode.UP, false, false);
            assertEquals("a", palette.selectedEntry().label());
        }

        @Test
        void backspaceRemovesQueryChar() {
            var registry = new CommandRegistry();
            registry.register("help", "H", "/h", args -> null);
            registry.register("exit", "E", "/e", args -> null);

            var palette = new CommandPalette(registry);
            palette.show();
            palette.appendQuery('h');
            palette.appendQuery('e');
            palette.backspaceQuery();

            var entry = palette.selectedEntry();
            assertNotNull(entry);
            assertEquals("help", entry.label());
        }

        @Test
        void selectedEntryNullWhenEmpty() {
            var registry = new CommandRegistry();
            var palette = new CommandPalette(registry);
            palette.show();
            palette.appendQuery('z');
            assertNull(palette.selectedEntry());
        }
    }
}
