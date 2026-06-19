package com.jay.jayflow.ir;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum WorkflowRunStatus {
    @JsonProperty("pending") PENDING,
    @JsonProperty("running") RUNNING,
    @JsonProperty("succeeded") SUCCEEDED,
    @JsonProperty("failed") FAILED,
    @JsonProperty("cancelled") CANCELLED,
    @JsonProperty("budget_exceeded") BUDGET_EXCEEDED,
    @JsonProperty("replay_diverged") REPLAY_DIVERGED
}
