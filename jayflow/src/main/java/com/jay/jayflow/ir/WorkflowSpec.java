package com.jay.jayflow.ir;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Top-level workflow specification. Equivalent to Rust's WorkflowSpec.
 */
@JsonInclude(NON_NULL)
public record WorkflowSpec(
        String id, String goal, String description,
        WorkflowConfig.BudgetSpec budget,
        WorkflowConfig.PermissionSpec permissions,
        WorkflowConfig.ModelPolicy modelPolicy,
        WorkflowConfig.PromotionPolicy promotionPolicy,
        List<WorkflowNode> nodes) { }
