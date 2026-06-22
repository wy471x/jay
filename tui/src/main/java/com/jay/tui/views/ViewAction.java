package com.jay.tui.views;

/**
 * Actions a modal view can return from its key handler.
 * Equivalent to Rust's ViewAction enum.
 */
public sealed interface ViewAction {
    /** No action — continue processing. */
    record None() implements ViewAction {}
    /** Close this modal and remove it from the stack. */
    record Close() implements ViewAction {}
    /** Emit an event to the host and stay open. */
    record Emit(ViewEvent event) implements ViewAction {}
    /** Emit an event and close this modal. */
    record EmitAndClose(ViewEvent event) implements ViewAction {}
}
