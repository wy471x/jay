package com.jay.tui.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Parses and repairs streaming tool-call inputs.
 * Mirrors Rust {@code tool_parser.rs}.
 *
 * <p>LLMs sometimes produce malformed JSON in tool-call arguments.
 * This parser applies repair strategies and code-fence stripping.
 */
public class ToolParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Try to parse the raw JSON input, applying repairs if needed. */
    public static JsonNode parseToolInput(String raw) {
        if (raw == null || raw.isBlank()) {
            return MAPPER.createObjectNode();
        }

        // 1. Try direct parse
        try {
            return MAPPER.readTree(raw);
        } catch (IOException e) {
            // fall through to repairs
        }

        // 2. Strip code fences (```json ... ```)
        String cleaned = stripCodeFences(raw);
        try {
            return MAPPER.readTree(cleaned);
        } catch (IOException e) {
            // fall through
        }

        // 3. Extract JSON object from surrounding text
        cleaned = extractJsonObject(cleaned);
        if (!cleaned.isEmpty()) {
            try {
                return MAPPER.readTree(cleaned);
            } catch (IOException e) {
                // fall through
            }
        }

        // 4. Fallback: wrap raw text
        return MAPPER.createObjectNode().put("_raw", raw);
    }

    /** Strip markdown code fences from JSON. */
    static String stripCodeFences(String raw) {
        String s = raw.strip();
        if (s.startsWith("```")) {
            int end = s.indexOf('\n');
            if (end < 0) end = 3;
            s = s.substring(end).strip();
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3).strip();
        }
        return s;
    }

    /** Extract the first complete JSON object from text. */
    static String extractJsonObject(String text) {
        int start = text.indexOf('{');
        if (start < 0) {
            // No object found — try array
            start = text.indexOf('[');
            if (start < 0) return "";
            return extractBalanced(text, start, '[', ']');
        }
        return extractBalanced(text, start, '{', '}');
    }

    /** Extract balanced brackets from text. */
    private static String extractBalanced(String text, int start, char open, char close) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (inString) {
                if (c == '\\') escaped = true;
                if (c == '"') inString = false;
                continue;
            }
            if (c == '"') { inString = true; continue; }
            if (c == open) { depth++; continue; }
            if (c == close) {
                depth--;
                if (depth == 0) return text.substring(start, i + 1);
            }
        }
        return "";
    }

    /** Finalize a streaming input buffer — called when ContentBlockStop arrives. */
    public static JsonNode finalizeStreamingInput(String raw, String toolName) {
        return parseToolInput(raw);
    }
}
