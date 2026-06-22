package com.jay.tools;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Helper extractors for JSON tool input. Equivalent to Rust's
 * required_str, optional_str, required_u64, optional_u64, optional_bool.
 */
public final class InputExtractors {
    private InputExtractors() { }

    public static String requiredStr(JsonNode input, String field) {
        var node = input.get(field);
        if (node == null || !node.isTextual()) {
            if (input.isObject() && !input.isEmpty()) {
                var names = new ArrayList<String>();
                input.fieldNames().forEachRemaining(names::add);
                ToolError.invalidInput(
                        "missing required field '" + field + "'. Input provided: "
                                + String.join(", ", names)).throwUnchecked();
            }
            ToolError.missingField(field).throwUnchecked();
        }
        return node.asText();
    }

    public static Optional<String> optionalStr(JsonNode input, String field) {
        var node = input.get(field);
        return node != null && node.isTextual() ? Optional.of(node.asText()) : Optional.empty();
    }

    public static long requiredU64(JsonNode input, String field) {
        var node = input.get(field);
        if (node == null || !node.isIntegralNumber() || node.asLong() < 0) {
            ToolError.missingField(field).throwUnchecked();
        }
        return node.asLong();
    }

    public static long optionalU64(JsonNode input, String field, long defaultValue) {
        var node = input.get(field);
        return node != null && node.isIntegralNumber() && node.asLong() >= 0
                ? node.asLong() : defaultValue;
    }

    public static boolean optionalBool(JsonNode input, String field, boolean defaultValue) {
        var node = input.get(field);
        return node != null && node.isBoolean() ? node.asBoolean() : defaultValue;
    }
}
