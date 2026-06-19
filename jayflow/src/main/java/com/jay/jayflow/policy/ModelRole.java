package com.jay.jayflow.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jay.jayflow.ir.AgentType;

/** Model role for workflow task assignment. Equivalent to Rust's ModelRole. */
public enum ModelRole {
    @JsonProperty("planner") PLANNER,
    @JsonProperty("leaf_reasoner") LEAF_REASONER,
    @JsonProperty("implementer") IMPLEMENTER,
    @JsonProperty("reviewer") REVIEWER,
    @JsonProperty("teacher") TEACHER,
    @JsonProperty("student") STUDENT,
    @JsonProperty("json_extractor") JSON_EXTRACTOR,
    @JsonProperty("starlark_repair") STARLARK_REPAIR;

    public static ModelRole fromAgentType(AgentType type) {
        return switch (type) {
            case GENERAL, EXPLORE -> LEAF_REASONER;
            case PLAN -> PLANNER;
            case REVIEW, VERIFIER -> REVIEWER;
            case IMPLEMENTER -> IMPLEMENTER;
        };
    }
}
