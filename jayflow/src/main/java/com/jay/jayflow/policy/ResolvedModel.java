package com.jay.jayflow.policy;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Resolved model for a workflow role. Equivalent to Rust's ResolvedModel. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResolvedModel(
        ModelRole role, String provider, String model,
        ModelCapabilities capabilities, ModelSelectionSource source) { }
