package com.jay.protocol.params;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record ThreadGoalSetParams(@JsonProperty("thread_id") String threadId, String objective,
        @JsonProperty("token_budget") Long tokenBudget) { }
