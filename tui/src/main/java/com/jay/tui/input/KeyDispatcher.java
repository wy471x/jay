package com.jay.tui.input;

import com.jay.tui.core.TuiAction;
import dev.tamboui.tui.event.KeyCode;

import java.util.Optional;

/**
 * Keyboard dispatcher. Maps key characters and escape sequences to TuiActions.
 */
public class KeyDispatcher {

    /** Dispatch a key event to a TuiAction. Returns empty for regular text. */
    public Optional<TuiAction> dispatch(KeyCode keyCode, char character, boolean ctrl, boolean alt) {
        // Ctrl+C / Ctrl+D → Quit
        if (ctrl && (character == 'c' || character == 'C'
                || character == 'd' || character == 'D')) {
            return Optional.of(new TuiAction.Quit());
        }
        // Ctrl+L → Scroll to bottom
        if (ctrl && (character == 'l' || character == 'L')) {
            return Optional.of(new TuiAction.ScrollToBottom());
        }
        // Escape → Cancel
        if (keyCode == KeyCode.ESCAPE) {
            return Optional.of(new TuiAction.CancelResponse());
        }
        // Enter → Send
        if (keyCode == KeyCode.ENTER) {
            return Optional.of(new TuiAction.SendMessage(""));
        }
        // Up arrow
        if (keyCode == KeyCode.UP) {
            return Optional.of(new TuiAction.ScrollUp(1));
        }
        // Down arrow
        if (keyCode == KeyCode.DOWN) {
            return Optional.of(new TuiAction.ScrollDown(1));
        }
        // PageUp
        if (keyCode == KeyCode.PAGE_UP) {
            return Optional.of(new TuiAction.ScrollUp(10));
        }
        // PageDown
        if (keyCode == KeyCode.PAGE_DOWN) {
            return Optional.of(new TuiAction.ScrollDown(10));
        }
        // Tab → toggle sidebar
        if (keyCode == KeyCode.TAB) {
            return Optional.of(new TuiAction.ToggleSidebar());
        }
        return Optional.empty();
    }

    /** Check if a character is printable (includes Unicode/CJK). */
    public static boolean isPrintable(char c) {
        return c >= 32 && !Character.isISOControl(c);
    }
}
