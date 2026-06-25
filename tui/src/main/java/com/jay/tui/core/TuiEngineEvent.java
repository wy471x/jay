package com.jay.tui.core;

import com.jay.agent.ProviderKind;

import java.util.List;

/**
 * Events flowing from Engine to UI via a {@link java.util.concurrent.BlockingQueue}.
 * Mirrors Rust {@code Event} enum — ~28 variants covering streaming, tools,
 * turn lifecycle, compaction, purging, and system events.
 *
 * <p>Produced by the engine virtual thread, consumed by the render thread
 * each frame via {@link EngineHandle#drainEvents()}.
 */
public sealed interface TuiEngineEvent {

    // ── Streaming events ──────────────────────────────────────────────

    /** A new content block (text/thinking/tool) has started. */
    record MessageStarted(int index, String kind) implements TuiEngineEvent {}

    /** Incremental text delta for streaming content. */
    record MessageDelta(int index, String content) implements TuiEngineEvent {}

    /** A content block has completed. */
    record MessageComplete(int index, String kind) implements TuiEngineEvent {}

    /** Thinking/content block started. */
    record ThinkingStarted(int index) implements TuiEngineEvent {}

    /** Incremental thinking delta. */
    record ThinkingDelta(int index, String content) implements TuiEngineEvent {}

    /** Thinking block completed. */
    record ThinkingComplete(int index) implements TuiEngineEvent {}

    // ── Tool events ───────────────────────────────────────────────────

    /** Tool call initiated. Arguments serialized as JSON. */
    record ToolCallStarted(String id, String name, String inputJson) implements TuiEngineEvent {}

    /** Tool call completed with result or error. */
    record ToolCallComplete(String id, String name, ToolResult result) implements TuiEngineEvent {}

    /** Model requested user approval for a tool call. */
    record ApprovalRequired(
            String id,
            String toolName,
            String description,
            String intentSummary
    ) implements TuiEngineEvent {}

    // ── Turn lifecycle ────────────────────────────────────────────────

    /** A new turn has started. */
    record TurnStarted(String turnId) implements TuiEngineEvent {}

    /** Turn completed with usage info. */
    record TurnComplete(
            String turnId,
            int inputTokens,
            int outputTokens,
            TurnOutcomeStatus status,
            String error
    ) implements TuiEngineEvent {}

    /** Turn was aborted. */
    record TurnAborted(String turnId, String reason) implements TuiEngineEvent {}

    enum TurnOutcomeStatus { COMPLETED, INTERRUPTED, FAILED }

    // ── Compaction & purging ──────────────────────────────────────────

    record CompactionStarted(String id, boolean auto, String message) implements TuiEngineEvent {}
    record CompactionCompleted(
            String id, boolean auto, String message,
            int messagesBefore, int messagesAfter
    ) implements TuiEngineEvent {}
    record CompactionFailed(String id, boolean auto, String message) implements TuiEngineEvent {}

    record PurgeStarted(String message) implements TuiEngineEvent {}
    record PurgeCompleted(
            int messagesBefore, int messagesAfter,
            int removedCount, int replacedCount, String message
    ) implements TuiEngineEvent {}
    record PurgeFailed(String message) implements TuiEngineEvent {}

    // ── System events ─────────────────────────────────────────────────

    /** Categorized engine error. */
    record EngineError(String message, String category, boolean recoverable) implements TuiEngineEvent {}

    /** Transient status for the UI status bar. */
    record StatusMessage(String text, String severity) implements TuiEngineEvent {}

    /** Engine initialization complete. */
    record EngineInitialized(
            String model, String workspaceName, List<String> threadIds, String activeThreadId
    ) implements TuiEngineEvent {}

    /** Authoritative session state sync from engine. */
    record SessionUpdated(String sessionId, int messageCount, long tokensUsed) implements TuiEngineEvent {}

    /** Prefix cache stability changed between turns. */
    record PrefixCacheChange(
            String description, boolean systemPromptChanged, boolean toolsChanged,
            double stabilityPercent
    ) implements TuiEngineEvent {}

    /** Model or provider was changed. */
    record ModelChanged(String modelName, ProviderKind provider) implements TuiEngineEvent {}

    // ── Input/output events ───────────────────────────────────────────

    /** Engine is requesting user input from the UI. */
    record UserInputRequired(String id, String prompt, String defaultValue) implements TuiEngineEvent {}

    /** Terminal input should be paused (for interactive subprocesses). */
    record PauseEvents() implements TuiEngineEvent {}

    /** Resume terminal input after interactive subprocess. */
    record ResumeEvents() implements TuiEngineEvent {}

    /** Tool result wrapper carried in events. */
    record ToolResult(String id, String name, String content, boolean success, String error) {
        public static ToolResult success(String id, String name, String content) {
            return new ToolResult(id, name, content, true, null);
        }

        public static ToolResult failure(String id, String name, String error) {
            return new ToolResult(id, name, null, false, error);
        }
    }
}
