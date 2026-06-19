package com.jay.protocol.runtime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RuntimeEventEnvelope(
        @JsonProperty("schema_version") int schemaVersion,
        long seq, String event, String kind,
        @JsonProperty("thread_id") String threadId,
        @JsonProperty("turn_id") String turnId,
        @JsonProperty("item_id") String itemId,
        @JsonInclude(NON_NULL) String timestamp,
        @JsonInclude(NON_NULL) @JsonProperty("created_at") String createdAt,
        JsonNode payload,
        @JsonInclude(NON_NULL) Map<String, JsonNode> extra) {
    public RuntimeEventEnvelope {
        if (schemaVersion == 0) schemaVersion = 1;
        if (extra == null) extra = Map.of();
    }
}
