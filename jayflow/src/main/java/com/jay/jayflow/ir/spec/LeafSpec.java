package com.jay.jayflow.ir.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jay.jayflow.ir.*;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/** Equivalent to Rust's LeafSpec. */
@JsonInclude(NON_NULL)
public record LeafSpec(String id, String prompt, AgentType agentType, TaskMode mode,
        IsolationMode isolation, @JsonProperty("file_scope") List<String> fileScope,
        @JsonProperty("depends_on_results") List<String> dependsOnResults,
        WorkflowConfig.BudgetSpec budget, WorkflowConfig.PermissionSpec permissions,
        WorkflowConfig.ModelPolicy modelPolicy) {}
