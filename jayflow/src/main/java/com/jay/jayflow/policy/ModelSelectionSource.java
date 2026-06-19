package com.jay.jayflow.policy;

import com.fasterxml.jackson.annotation.JsonProperty;

/** How a model was selected during resolution. Equivalent to Rust's ModelSelectionSource. */
public enum ModelSelectionSource {
    @JsonProperty("primary") PRIMARY,
    @JsonProperty("fallback") FALLBACK,
    @JsonProperty("role_default") ROLE_DEFAULT
}
