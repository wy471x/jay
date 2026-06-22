package com.jay.tui.client;

/**
 * A single SSE delta from the chat completions streaming endpoint.
 * Equivalent to Rust's StreamEvent / client delta types.
 */
public sealed interface StreamEvent {

    /** A text content delta for a message. */
    record ContentDelta(String content, String reasoning) implements StreamEvent {}

    /** A tool call delta (partial or complete). */
    record ToolCallDelta(String id, String name, String arguments) implements StreamEvent {}

    /** Stream finished normally. */
    record Done(String stopReason) implements StreamEvent {}

    /** Stream encountered an error. */
    record Error(String message) implements StreamEvent {}

    /** Usage statistics from final chunk. */
    record Usage(int promptTokens, int completionTokens) implements StreamEvent {}
}
