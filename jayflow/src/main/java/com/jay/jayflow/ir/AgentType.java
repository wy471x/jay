package com.jay.jayflow.ir;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AgentType {
    @JsonProperty("general") GENERAL,
    @JsonProperty("explore") EXPLORE,
    @JsonProperty("plan") PLAN,
    @JsonProperty("review") REVIEW,
    @JsonProperty("implementer") IMPLEMENTER,
    @JsonProperty("verifier") VERIFIER
}
