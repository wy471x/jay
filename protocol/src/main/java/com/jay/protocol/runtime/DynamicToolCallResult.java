package com.jay.protocol.runtime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

public record DynamicToolCallResult(boolean success,
        @JsonInclude(NON_EMPTY) List<DynamicToolCallContent> content) {
    public DynamicToolCallResult {
        if (content == null) content = List.of();
    }
}
