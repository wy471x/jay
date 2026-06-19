package com.jay.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public record McpStartupCompleteEvent(List<String> ready, List<McpStartupFailure> failed, List<String> cancelled) {
    public McpStartupCompleteEvent {
        if (ready == null) ready = List.of();
        if (failed == null) failed = List.of();
        if (cancelled == null) cancelled = List.of();
    }
}
