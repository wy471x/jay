package com.jay.jayflow.ir;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PromotionStrategy {
    @JsonProperty("all") ALL,
    @JsonProperty("first_success") FIRST_SUCCESS,
    @JsonProperty("best_score") BEST_SCORE,
    @JsonProperty("teacher_selected") TEACHER_SELECTED
}

/** Workflow execution status. Equivalent to Rust's WorkflowRunStatus. */
