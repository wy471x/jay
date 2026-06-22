package com.jay.jayflow.policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jay.jayflow.execution.WorkflowUsage;
import com.jay.jayflow.ir.WorkflowConfig;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public record CompletionResponse(String text, WorkflowUsage usage) { }

/** Error from a model provider. Equivalent to Rust's ModelProviderError. */
