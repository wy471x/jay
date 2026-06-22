package com.jay.jayflow.ir.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jay.jayflow.ir.WorkflowConfig;
import com.jay.jayflow.ir.WorkflowNode;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/** Equivalent to Rust's BranchSpec. */
@JsonInclude(NON_NULL)
public record BranchSpec(String id, String description, boolean parallel,
        WorkflowConfig.BudgetSpec budget, WorkflowConfig.PermissionSpec permissions,
        WorkflowConfig.ModelPolicy modelPolicy, List<WorkflowNode> children) { }
