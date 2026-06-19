package com.jay.jayflow.execution;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jay.jayflow.ir.WorkflowRunStatus;
import java.util.ArrayList;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

/** Execution state with mutable collections for in-place updates. Equivalent to Rust's WorkflowExecution. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WorkflowExecution(
        WorkflowRunStatus status,
        WorkflowUsage usage,
        WorkflowMemoUsage memoUsage,
        @JsonInclude(NON_EMPTY) @JsonProperty("leaf_results") List<LeafResult> leafResults,
        @JsonInclude(NON_EMPTY) @JsonProperty("branch_results") List<BranchResult> branchResults,
        @JsonInclude(NON_EMPTY) @JsonProperty("control_node_results") List<ControlNodeResult> controlNodeResults) {

    public WorkflowExecution {
        if (leafResults == null) leafResults = new ArrayList<>();
        if (branchResults == null) branchResults = new ArrayList<>();
        if (controlNodeResults == null) controlNodeResults = new ArrayList<>();
    }

    public WorkflowExecution() { this(WorkflowRunStatus.SUCCEEDED, new WorkflowUsage(), new WorkflowMemoUsage(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()); }
    public WorkflowExecution withStatus(WorkflowRunStatus s) { return new WorkflowExecution(s, usage, memoUsage, leafResults, branchResults, controlNodeResults); }
    public WorkflowExecution withUsage(WorkflowUsage u) { return new WorkflowExecution(status, u, memoUsage, leafResults, branchResults, controlNodeResults); }
    public WorkflowExecution withMemoUsage(WorkflowMemoUsage m) { return new WorkflowExecution(status, usage, m, leafResults, branchResults, controlNodeResults); }
}
