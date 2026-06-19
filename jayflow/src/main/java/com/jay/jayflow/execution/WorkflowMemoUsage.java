package com.jay.jayflow.execution;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Prompt cache memoization stats. Equivalent to Rust's WorkflowMemoUsage. */
public record WorkflowMemoUsage(
        @JsonProperty("armh_hits") long armhHits,
        @JsonProperty("armh_misses") long armhMisses,
        @JsonProperty("armh_saved_estimated_tokens") long armhSavedEstimatedTokens,
        @JsonProperty("provider_prompt_cache_hits") long providerPromptCacheHits,
        @JsonProperty("provider_prompt_cache_misses") long providerPromptCacheMisses) {

    public WorkflowMemoUsage() { this(0, 0, 0, 0, 0); }

    public WorkflowMemoUsage add(WorkflowMemoUsage other) {
        return new WorkflowMemoUsage(armhHits + other.armhHits, armhMisses + other.armhMisses,
                armhSavedEstimatedTokens + other.armhSavedEstimatedTokens,
                providerPromptCacheHits + other.providerPromptCacheHits,
                providerPromptCacheMisses + other.providerPromptCacheMisses);
    }
}
