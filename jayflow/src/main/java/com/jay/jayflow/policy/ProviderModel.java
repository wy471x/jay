package com.jay.jayflow.policy;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Provider + model pair with capabilities. Equivalent to Rust's ProviderModel. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProviderModel(String provider, String model, ModelCapabilities capabilities) {
    public ProviderModel {
        if (capabilities == null) capabilities = new ModelCapabilities();
    }
}
