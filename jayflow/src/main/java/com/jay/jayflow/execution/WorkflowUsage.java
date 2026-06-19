package com.jay.jayflow.execution;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Token/cost usage for a workflow element. Equivalent to Rust's WorkflowUsage. */
public record WorkflowUsage(
        @JsonProperty("input_tokens") long inputTokens,
        @JsonProperty("output_tokens") long outputTokens,
        @JsonProperty("cost_microusd") long costMicrousd) {

    public WorkflowUsage() { this(0, 0, 0); }

    public long totalTokens() { return inputTokens + outputTokens; }

    public WorkflowUsage add(WorkflowUsage other) {
        return new WorkflowUsage(inputTokens + other.inputTokens,
                outputTokens + other.outputTokens, costMicrousd + other.costMicrousd);
    }
}
