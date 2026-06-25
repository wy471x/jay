package com.jay.tui.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Enhanced SSE stream events matching the Rust streaming model.
 * These represent parsed events from the Chat Completions SSE stream,
 * with content-block-level granularity (start/delta/stop per block).
 */
public sealed interface SseStreamEvent {

    ObjectMapper MAPPER = new ObjectMapper();

    /** Stream opened, message-level metadata. */
    record MessageStart(String model, String messageId) implements SseStreamEvent {}

    /** A new content block (text, thinking, tool_use) has started. */
    record ContentBlockStart(int index, ContentBlockInfo block) implements SseStreamEvent {}

    /** Incremental delta within the current content block. */
    record ContentBlockDelta(int index, Delta delta) implements SseStreamEvent {}

    /** Current content block finished. */
    record ContentBlockStop(int index) implements SseStreamEvent {}

    /** Message-level delta (usage, stop_reason). */
    record MessageDelta(Usage usage, String stopReason) implements SseStreamEvent {}

    /** Entire message stream finished. */
    record MessageStop() implements SseStreamEvent {}

    /** Parsed error from the stream. */
    record StreamError(String message, String code, boolean retryable) implements SseStreamEvent {}

    // ── Sub-types ──────────────────────────────────────────────────

    sealed interface ContentBlockInfo {
        record TextBlock() implements ContentBlockInfo {}
        record ThinkingBlock() implements ContentBlockInfo {}
        record ToolUseBlock(String id, String name) implements ContentBlockInfo {}
    }

    sealed interface Delta {
        record TextDelta(String text) implements Delta {}
        record ThinkingDelta(String text) implements Delta {}
        record InputJsonDelta(String partialJson) implements Delta {}
        record SignatureDelta(String signature) implements Delta {}
    }

    record Usage(int promptTokens, int completionTokens) {}

    // ── SSE Parsing ─────────────────────────────────────────────────

    /**
     * Parse a single "data:" line from the SSE stream.
     * Returns null for unparseable lines or ping/empty events.
     */
    static SseStreamEvent parseData(String data) {
        if (data == null || data.isEmpty() || data.equals("[DONE]")) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(data);

            // Error frame
            if (root.has("error")) {
                var err = root.get("error");
                return new StreamError(
                        err.has("message") ? err.get("message").asText() : "unknown",
                        err.has("code") ? err.get("code").asText() : null,
                        true
                );
            }

            var choices = root.get("choices");
            if (choices == null || choices.isEmpty()) return null;

            var choice = choices.get(0);
            int index = choice.has("index") ? choice.get("index").asInt() : 0;
            var delta = choice.get("delta");
            String finishReason = choice.has("finish_reason")
                    ? choice.get("finish_reason").asText() : null;

            // Content block start detection (role field in delta)
            if (delta != null && delta.has("role")) {
                // This is the first delta with role — synthetic MessageStart
                String model = root.has("model") ? root.get("model").asText() : null;
                String msgId = root.has("id") ? root.get("id").asText() : null;
                return new MessageStart(model, msgId);
            }

            // Content delta
            if (delta != null) {
                if (delta.has("content")) {
                    String text = delta.get("content").asText("");
                    if (!text.isEmpty()) {
                        return new ContentBlockDelta(index, new Delta.TextDelta(text));
                    }
                }
                if (delta.has("reasoning_content")) {
                    String text = delta.get("reasoning_content").asText("");
                    if (!text.isEmpty()) {
                        return new ContentBlockDelta(index, new Delta.ThinkingDelta(text));
                    }
                }
                if (delta.has("tool_calls")) {
                    for (var tc : delta.get("tool_calls")) {
                        String tcId = tc.has("id") ? tc.get("id").asText() : null;
                        String tcIndex = tc.has("index") ? String.valueOf(tc.get("index").asInt()) : "0";
                        var fn = tc.get("function");
                        if (fn != null) {
                            String name = fn.has("name") ? fn.get("name").asText() : null;
                            String args = fn.has("arguments") ? fn.get("arguments").asText() : null;
                            if (name != null) {
                                // Tool call starting
                                return new ContentBlockStart(
                                        tc.has("index") ? tc.get("index").asInt() : index,
                                        new ContentBlockInfo.ToolUseBlock(tcId, name)
                                );
                            }
                            if (args != null && !args.isEmpty()) {
                                return new ContentBlockDelta(index,
                                        new Delta.InputJsonDelta(args));
                            }
                        }
                    }
                }
            }

            // Finish reason → ContentBlockStop or MessageStop
            if (finishReason != null) {
                if ("stop".equals(finishReason) || "tool_calls".equals(finishReason)) {
                    // MessageStop with usage
                    var usage = parseUsage(root);
                    return new MessageStop();
                }
            }

            // Usage-only frame
            if (root.has("usage")) {
                var usage = parseUsage(root);
                return new MessageDelta(usage, finishReason);
            }

            return null;
        } catch (Exception e) {
            return new StreamError("Parse error: " + e.getMessage(), "parse", false);
        }
    }

    private static Usage parseUsage(JsonNode root) {
        var usage = root.get("usage");
        if (usage == null) return null;
        int prompt = usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt() : 0;
        int completion = usage.has("completion_tokens")
                ? usage.get("completion_tokens").asInt() : 0;
        return new Usage(prompt, completion);
    }
}
