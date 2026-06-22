package com.jay.tui.views;

import dev.tamboui.terminal.Frame;
import dev.tamboui.layout.Rect;

/**
 * Trait for modal overlay views. Each modal handles its own keyboard
 * input and rendering within an allocated area.
 *
 * <p>Equivalent to Rust's ModalView trait.
 */
public interface ModalView {

    /** Unique identifier for this modal kind. */
    String kind();

    /** Handle a keyboard character + modifiers. Return the resulting action. */
    ViewAction handleKey(char character, int keyCode, boolean ctrl, boolean alt);

    /** Render this modal into the given frame area. */
    void render(Frame frame, Rect area);

    /** Called each tick while this modal is active. */
    default void tick() {}

    /** Whether this modal should prevent input from reaching the composer. */
    default boolean blocksComposer() { return true; }
}
