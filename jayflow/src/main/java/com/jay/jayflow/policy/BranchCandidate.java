package com.jay.jayflow.policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jay.jayflow.ir.WorkflowRunStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BranchCandidate(
        @JsonProperty("branch_id") String branchId,
        WorkflowRunStatus status,
        int score,
        long cost,
        @JsonProperty("diversity_key") String diversityKey) { }
