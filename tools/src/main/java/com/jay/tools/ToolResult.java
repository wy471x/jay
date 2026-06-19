package com.jay.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Result of a tool execution. Equivalent to Rust's ToolResult.
 */
@JsonInclude(NON_NULL)
public record ToolResult(String content, boolean success, JsonNode metadata) {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static ToolResult success(String content) {
        return new ToolResult(content, true, null);
    }

    public static ToolResult error(String message) {
        return new ToolResult(message, false, null);
    }

    public static ToolResult json(Object value) {
        try {
            return new ToolResult(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value),
                    true, null);
        } catch (JsonProcessingException e) {
            return new ToolResult("{\"error\":\"serialization failed\"}", false, null);
        }
    }

    public ToolResult withMetadata(JsonNode metadata) {
        return new ToolResult(content, success, metadata);
    }
}
