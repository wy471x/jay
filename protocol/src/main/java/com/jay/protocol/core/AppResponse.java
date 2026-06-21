package com.jay.protocol.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

public record AppResponse(boolean ok, JsonNode data, @JsonInclude(NON_EMPTY) List<EventFrame> events) {
    public AppResponse {
        if (events == null) events = List.of();
    }
}
