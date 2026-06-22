package com.jay.jayflow.policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Student replay baseline/candidate metrics. Equivalent to Rust's StudentReplayMetrics. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudentReplayMetrics(
        @JsonProperty("score") int score,
        @JsonProperty("cost_microusd") long costMicrousd) { }
