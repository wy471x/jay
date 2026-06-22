package com.jay.protocol.params;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record ThreadGoalProgressParams(@JsonProperty("thread_id") String threadId,
        @JsonProperty(value = "token_delta", defaultValue = "0") long tokenDelta,
        @JsonProperty(value = "time_delta_seconds", defaultValue = "0") long timeDeltaSeconds,
        @JsonProperty(value = "record_continuation", defaultValue = "false") boolean recordContinuation) { }
