package com.jay.tui.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Content blocks produced by streaming LLM responses.
 * Mirrors Rust ContentBlock / ContentBlockStart / ContentBlockStop types.
 */
public sealed interface ContentBlock {

    /** Plain text content. */
    record Text(String content) implements ContentBlock {}

    /** Thinking/reasoning content (optional signature). */
    record Thinking(String content, String signature) implements ContentBlock {
        public Thinking(String content) { this(content, null); }
    }

    /** A tool-use request from the model. */
    record ToolUse(String id, String name, JsonNode input) implements ContentBlock {}

    /** Result of a tool execution. */
    record ToolResult(String toolUseId, String content, boolean isError)
            implements ContentBlock {}

    // ── Factory helpers ─────────────────────────────────────────────

    static Text text(String content) { return new Text(content); }
    static Thinking thinking(String content) { return new Thinking(content); }
    static ToolUse toolUse(String id, String name, JsonNode input) {
        return new ToolUse(id, name, input);
    }
    static ToolResult toolResult(String id, String content, boolean isError) {
        return new ToolResult(id, content, isError);
    }

    // ── Visitor ──────────────────────────────────────────────────────

    /** The kind of content block for state tracking during streaming. */
    enum Kind { TEXT, THINKING, TOOL_USE, NONE }
}
