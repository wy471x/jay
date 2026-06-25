package com.jay.tui.core;

import java.util.List;

/**
 * Operations flowing from UI to Engine via {@link java.util.concurrent.BlockingQueue}.
 * Mirrors Rust {@code Op} enum — 11 variants for the 7-step main path.
 *
 * <p>Unlike {@link TuiAction} (render-local shortcuts), these ops are consumed
 * by the engine virtual thread and may trigger LLM turns, tool execution,
 * or session mutations.
 */
public sealed interface TuiEngineOp {

    /** Submit a user message. Carries mode, model routing, and approval settings. */
    record SendMessage(
            String content,
            String mode,
            String model,
            boolean allowShell,
            boolean trustMode,
            boolean autoApprove,
            boolean showThinking,
            List<String> allowedTools
    ) implements TuiEngineOp {}

    /** Execute a shell command without a model turn (the {@code !} prefix). */
    record RunShellCommand(
            String command,
            String mode,
            boolean allowShell,
            boolean trustMode,
            boolean autoApprove
    ) implements TuiEngineOp {}

    /** Cancel the current in-flight API request. */
    record CancelRequest() implements TuiEngineOp {}

    /** Approve a specific tool call that required permission. */
    record ApproveToolCall(String id, String toolName) implements TuiEngineOp {}

    /** Deny a specific tool call. */
    record DenyToolCall(String id) implements TuiEngineOp {}

    /** Switch the operating mode (agent / plan / yolo). */
    record ChangeMode(String mode) implements TuiEngineOp {}

    /** Update the model ID and refresh system prompt context. */
    record SetModel(String modelName, String mode) implements TuiEngineOp {}

    /** Set compaction configuration at runtime (JSON-serializable config). */
    record SetCompaction(boolean enabled, int tokenThreshold, String model) implements TuiEngineOp {}

    /** Run context compaction immediately. */
    record CompactContext() implements TuiEngineOp {}

    /** Remove the last user + assistant exchange and re-send with new content. */
    record EditLastTurn(String newMessage) implements TuiEngineOp {}

    /** Graceful engine shutdown. */
    record Shutdown() implements TuiEngineOp {}
}
