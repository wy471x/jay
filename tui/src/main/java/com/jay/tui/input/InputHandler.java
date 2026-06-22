package com.jay.tui.input;

import com.jay.tui.core.TuiAction;
import com.jay.tui.state.ComposerState;

import java.util.Optional;

/**
 * Processes raw terminal input and produces TuiActions.
 * Terminal-agnostic — takes key codes and characters directly.
 */
public class InputHandler {

    private final KeyDispatcher keyDispatcher;
    private final ComposerState composer;

    public InputHandler(ComposerState composer) {
        this.keyDispatcher = new KeyDispatcher();
        this.composer = composer;
    }

    /** Process an input event. Returns an optional TuiAction. */
    public Optional<TuiAction> handleInput(int keyCode, char character, boolean ctrl, boolean alt) {
        // Try dispatch
        var action = keyDispatcher.dispatch(keyCode, character, ctrl, alt);
        if (action.isPresent()) {
            if (action.get() instanceof TuiAction.SendMessage) {
                String text = composer.commit();
                if (text.isEmpty()) return Optional.empty();
                return Optional.of(new TuiAction.SendMessage(text));
            }
            return action;
        }

        // Backspace
        if (keyCode == 127 || keyCode == '\b') {
            composer.deleteBefore();
            return Optional.empty();
        }

        // Printable character
        if (KeyDispatcher.isPrintable(character) && !ctrl && !alt) {
            composer.insertChar(character);
            return Optional.empty();
        }

        return Optional.empty();
    }
}
