package com.jay.tui.input;

import com.jay.tui.core.TuiAction;

import java.util.Optional;

/**
 * Keyboard dispatcher. Maps key characters and escape sequences to TuiActions.
 * Uses simple char/int-based key representation (terminal-agnostic).
 */
public class KeyDispatcher {

    /** Dispatch a character key to a TuiAction. Returns empty for regular text. */
    public Optional<TuiAction> dispatch(int keyCode, char character, boolean ctrl, boolean alt) {
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
        if (keyCode == 27) {
            return Optional.of(new TuiAction.CancelResponse());
        }
        // Enter → Send
        if (keyCode == '\r' || keyCode == '\n') {
            return Optional.of(new TuiAction.SendMessage(""));
        }
        // Up arrow
        if (keyCode == 65517 || keyCode == 65) { // CSI A
            return Optional.of(new TuiAction.ScrollUp(1));
        }
        // Down arrow
        if (keyCode == 65518 || keyCode == 66) { // CSI B
            return Optional.of(new TuiAction.ScrollDown(1));
        }
        // PageUp
        if (keyCode == 65519) {
            return Optional.of(new TuiAction.ScrollUp(10));
        }
        // PageDown
        if (keyCode == 65520) {
            return Optional.of(new TuiAction.ScrollDown(10));
        }
        // Tab → toggle sidebar
        if (keyCode == '\t') {
            return Optional.of(new TuiAction.ToggleSidebar());
        }
        return Optional.empty();
    }

    /** Check if a character is printable. */
    public static boolean isPrintable(char c) {
        return c >= 32 && c < 127;
    }
}
