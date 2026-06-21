package com.jay.protocol.core;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ThreadGoalStatus {
    @JsonProperty("active") ACTIVE,
    @JsonProperty("paused") PAUSED,
    @JsonProperty("blocked") BLOCKED,
    @JsonProperty("usage_limited") USAGE_LIMITED,
    @JsonProperty("budget_limited") BUDGET_LIMITED,
    @JsonProperty("complete") COMPLETE
}
