package com.jay.tui.views;

/**
 * Events that modals can emit back to the host application.
 * Equivalent to Rust's ViewEvent enum.
 */
public sealed interface ViewEvent {
    /** A command was selected from the palette/slash menu. */
    record CommandSelected(String command, String args) implements ViewEvent {}
    /** A model was selected from the model picker. */
    record ModelSelected(String modelName, String provider) implements ViewEvent {}
    /** A mode was selected (agent/yolo/plan). */
    record ModeSelected(String mode) implements ViewEvent {}
    /** Help was dismissed. */
    record HelpDismissed() implements ViewEvent {}
    /** Configuration was updated. */
    record ConfigUpdated(String key, String value) implements ViewEvent {}
}
