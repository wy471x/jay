package com.jay.tui.input;

import com.jay.tui.core.TuiAction;
import com.jay.tui.state.ComposerState;
import dev.tamboui.tui.event.KeyCode;

import java.util.Optional;

/**
 * Processes raw terminal input and produces TuiActions.
 * Terminal-agnostic — takes key codes and characters directly.
 */
public class InputHandler {

    private final KeyDispatcher keyDispatcher;
    private final ComposerState composer;
    private String lastTextInserted = "";
    private long lastTextInsertedAt;

    public InputHandler(ComposerState composer) {
        this.keyDispatcher = new KeyDispatcher();
        this.composer = composer;
    }

    /** Process an input event. Returns an optional TuiAction. */
    public Optional<TuiAction> handleInput(KeyCode keyCode, char character, boolean ctrl, boolean alt) {
        // Try dispatch
        var action = keyDispatcher.dispatch(keyCode, character, ctrl, alt);
        if (action.isPresent()) {
            if (action.get() instanceof TuiAction.SendMessage) {
                String text = composer.commit();
                if (text.isEmpty()) return Optional.empty();
                return Optional.of(new TuiAction.SendMessage(text));
            }
            lastTextInserted = "";
            return action;
        }

        // Backspace
        if (keyCode == KeyCode.BACKSPACE) {
            composer.deleteBefore();
            lastTextInserted = "";
            return Optional.empty();
        }

        // Printable character — skip if same as last IME text (dedup)
        if (KeyDispatcher.isPrintable(character) && !ctrl && !alt) {
            long now = System.currentTimeMillis();
            String charStr = String.valueOf(character);
            if (now - lastTextInsertedAt < 100 && charStr.equals(lastTextInserted)) {
                return Optional.empty(); // duplicate IME event
            }
            composer.insertChar(character);
            lastTextInserted = "";
            return Optional.empty();
        }

        lastTextInserted = "";
        return Optional.empty();
    }

    /** Process text input (IME result or multi-character string). */
    public Optional<TuiAction> handleTextInput(String text, boolean ctrl, boolean alt) {
        if (text == null || text.isEmpty() || ctrl || alt) {
            return Optional.empty();
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (KeyDispatcher.isPrintable(c)) {
                composer.insertChar(c);
            }
        }
        lastTextInserted = text;
        lastTextInsertedAt = System.currentTimeMillis();
        return Optional.empty();
    }
}
