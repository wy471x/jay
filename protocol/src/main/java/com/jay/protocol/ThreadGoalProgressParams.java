package com.jay.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@JsonInclude(NON_NULL)
public record ThreadGoalProgressParams(@JsonProperty("thread_id") String threadId,
        @JsonProperty(value = "token_delta", defaultValue = "0") long tokenDelta,
        @JsonProperty(value = "time_delta_seconds", defaultValue = "0") long timeDeltaSeconds,
        @JsonProperty(value = "record_continuation", defaultValue = "false") boolean recordContinuation) {}

