package com.jay.jayflow.policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jay.jayflow.execution.WorkflowUsage;
import com.jay.jayflow.ir.WorkflowConfig;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public interface ModelProvider {
    String provider();
    String model();
    ModelCapabilities capabilities();
    CompletionResponse complete(CompletionRequest request) throws ModelProviderError;
}

/** Request to a model provider. Equivalent to Rust's CompletionRequest. */
