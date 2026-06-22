package com.jay.tui.core;

import com.jay.agent.ProviderKind;
import com.jay.protocol.core.ResponseChannel;
import com.jay.state.model.MessageEntity;

import java.util.List;

/**
 * Events flowing from the application engine up to the TUI renderer.
 * Produced by TuiEngine (or its HookSink adapter), consumed by the render-loop
 * thread to update AppState via {@link com.jay.tui.state.AppState#apply(TuiEvent)}.
 *
 * <p>Equivalent to Rust's UiEvent enum + streaming event projections.
 */
public sealed interface TuiEvent {

    /** Engine initialization complete. Carries current thread list and active thread. */
    record Initialized(List<com.jay.protocol.core.Thread> threads, String currentThreadId,
                       ProviderKind provider, String modelName) implements TuiEvent {}

    /** Graceful shutdown requested (e.g. /exit command processed). */
    record ShutdownRequested(String reason) implements TuiEvent {}

    /** A text delta arrived for the current response stream. */
    record ResponseDelta(String threadId, String delta, ResponseChannel channel,
                         String responseId) implements TuiEvent {}

    /** Current response stream finished successfully. */
    record ResponseEnd(String threadId, String responseId) implements TuiEvent {}

    /** Tool call started. Arguments serialized as JSON string. */
    record ToolCallBegin(String toolName, String arguments) implements TuiEvent {}

    /** Tool call finished (success or failure). */
    record ToolCallEnd(String toolName, boolean success, String summary) implements TuiEvent {}

    /** LLM turn started (new thinking cycle). */
    record TurnStarted(String turnId) implements TuiEvent {}

    /** LLM turn completed. */
    record TurnComplete(String turnId) implements TuiEvent {}

    /** LLM turn aborted with reason. */
    record TurnAborted(String turnId, String reason) implements TuiEvent {}

    /** Thread list changed (created, archived, renamed, etc.). */
    record ThreadListUpdated(List<com.jay.protocol.core.Thread> threads) implements TuiEvent {}

    /** Active thread changed. Carries message history for the new thread. */
    record ThreadSwitched(String threadId, List<MessageEntity> messages) implements TuiEvent {}

    /** Error from engine or runtime. */
    record Error(String message) implements TuiEvent {}

    /** Status bar message (auto-dismissed after ~5 seconds). severity: info, warn, error */
    record StatusMessage(String text, String severity) implements TuiEvent {}

    /** Result of a slash command execution. */
    record SlashResult(String command, boolean success, String output) implements TuiEvent {}

    /** Model or provider was changed. */
    record ModelChanged(String modelName, ProviderKind provider) implements TuiEvent {}

    /** Terminal was resized. */
    record Resized(int columns, int rows) implements TuiEvent {}
}
