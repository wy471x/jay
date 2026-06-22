package com.jay.jayflow.execution;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import com.jay.jayflow.ir.WorkflowRunStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BranchResult(
        @JsonProperty("branch_id") String branchId,
        @JsonProperty("task_id") String taskId,
        WorkflowRunStatus status,
        WorkflowUsage usage,
        @JsonProperty("memo_usage") WorkflowMemoUsage memoUsage,
        @JsonInclude(NON_EMPTY) List<String> artifacts,
        String notes) { }
