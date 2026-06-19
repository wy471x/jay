package com.jay.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

public enum ThreadGoalStatus {
    @JsonProperty("active") ACTIVE,
    @JsonProperty("paused") PAUSED,
    @JsonProperty("blocked") BLOCKED,
    @JsonProperty("usage_limited") USAGE_LIMITED,
    @JsonProperty("budget_limited") BUDGET_LIMITED,
    @JsonProperty("complete") COMPLETE
}

// ---- Thread data ----
