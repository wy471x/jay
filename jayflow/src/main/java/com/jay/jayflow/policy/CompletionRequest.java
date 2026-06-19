package com.jay.jayflow.policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jay.jayflow.execution.WorkflowUsage;
import com.jay.jayflow.ir.WorkflowConfig;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record CompletionRequest(ModelRole role, String prompt,
        @JsonProperty("require_json") boolean requireJson,
        @JsonProperty("model_policy") WorkflowConfig.ModelPolicy modelPolicy) {
    // simplified: WorkflowConfig.ModelPolicy is in ir package
}

/** Response from a model provider. Equivalent to Rust's CompletionResponse. */
