package com.jay.jayflow.authoring.js;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jay.jayflow.ir.*;
import com.jay.jayflow.ir.spec.*;
import java.util.List;

/**
 * Intermediate deserialization type for JS-workflow-authored JSON.
 * Equivalent to Rust's JsWorkflowSpec. Converts to WorkflowSpec via {@link #toWorkflow()}.
 */
public class JsWorkflowSpec {
    public String id;
    public String goal;
    public String description;
    @JsonProperty("budget") public WorkflowConfig.BudgetSpec budget = new WorkflowConfig.BudgetSpec(null, null, null);
    @JsonProperty("permissions") public WorkflowConfig.PermissionSpec permissions = new WorkflowConfig.PermissionSpec(false, false, List.of(), List.of());
    @JsonProperty("model_policy") public WorkflowConfig.ModelPolicy modelPolicy = new WorkflowConfig.ModelPolicy(null, null, List.of());
    @JsonProperty("promotion_policy") public WorkflowConfig.PromotionPolicy promotionPolicy = new WorkflowConfig.PromotionPolicy(PromotionStrategy.ALL, false, null, new WorkflowConfig.PromotionGateSpec());
    @JsonProperty("nodes") public List<JsWorkflowNode> nodes = List.of();

    public JsWorkflowSpec() {}

    public WorkflowSpec toWorkflow() {
        return new WorkflowSpec(id, goal, description, budget, permissions, modelPolicy, promotionPolicy,
                nodes.stream().map(JsWorkflowNode::toNode).toList());
    }
}
