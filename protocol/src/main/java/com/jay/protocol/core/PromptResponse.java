package com.jay.protocol.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

public record PromptResponse(String output, String model, @JsonInclude(NON_EMPTY) List<EventFrame> events) {
    public PromptResponse {
        if (events == null) events = List.of();
    }
}
