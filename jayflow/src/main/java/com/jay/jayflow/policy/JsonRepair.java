package com.jay.jayflow.policy;

import com.fasterxml.jackson.databind.ObjectMapper;

/** JSON repair utilities. Equivalent to Rust's repair_json_text_once + parse_json_with_repair. */
public final class JsonRepair {
    private static final ObjectMapper mapper = new ObjectMapper();
    private JsonRepair() {}

    public static <T> T parseWithRepair(String raw, Class<T> type) {
        try { return mapper.readValue(raw, type); } catch (Exception first) {
            try { return mapper.readValue(repairOnce(raw), type); } catch (Exception second) {
                throw new JsonRepairError("parse: " + first.getMessage() + "; repair failed: " + second.getMessage());
            }
        }
    }

    public static String repairOnce(String raw) {
        var trimmed = raw.trim();
        if (trimmed.startsWith("```json")) trimmed = trimmed.substring(7);
        else if (trimmed.startsWith("```")) trimmed = trimmed.substring(3);
        if (trimmed.endsWith("```")) trimmed = trimmed.substring(0, trimmed.length() - 3);
        trimmed = trimmed.trim();
        int brace = trimmed.indexOf('{'), bracket = trimmed.indexOf('[');
        if (brace >= 0) { int close = trimmed.lastIndexOf('}'); if (close > brace) trimmed = trimmed.substring(brace, close + 1); }
        else if (bracket >= 0) { int close = trimmed.lastIndexOf(']'); if (close > bracket) trimmed = trimmed.substring(bracket, close + 1); }
        return trimmed;
    }

    public static class JsonRepairError extends RuntimeException {
        public JsonRepairError(String msg) { super(msg); }
    }
}
