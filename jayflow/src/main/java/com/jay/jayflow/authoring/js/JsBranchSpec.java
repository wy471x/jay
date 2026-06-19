package com.jay.jayflow.authoring.js;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jay.jayflow.ir.*;
import com.jay.jayflow.ir.spec.BranchSpec;
import java.util.List;

/** Equivalent to Rust's JsBranchSpec. Converts children from JsWorkflowNode to WorkflowNode. */
public class JsBranchSpec {
    public String id;
    public String description;
    public boolean parallel = true;
    @JsonProperty("budget") public WorkflowConfig.BudgetSpec budget = new WorkflowConfig.BudgetSpec(null, null, null);
    @JsonProperty("permissions") public WorkflowConfig.PermissionSpec permissions = new WorkflowConfig.PermissionSpec(false, false, List.of(), List.of());
    @JsonProperty("model_policy") public WorkflowConfig.ModelPolicy modelPolicy = new WorkflowConfig.ModelPolicy(null, null, List.of());
    @JsonProperty("children") public List<JsWorkflowNode> children = List.of();

    public BranchSpec toBranch() {
        return new BranchSpec(id, description, parallel, budget, permissions, modelPolicy,
                children.stream().map(JsWorkflowNode::toNode).toList());
    }
}
