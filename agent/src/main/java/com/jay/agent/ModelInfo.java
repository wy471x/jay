package com.jay.agent;

import java.util.List;

/**
 * Metadata for a single model entry in the registry.
 * Equivalent to Rust's ModelInfo struct.
 */
public record ModelInfo(
        String id,
        ProviderKind provider,
        List<String> aliases,
        boolean supportsTools,
        boolean supportsReasoning
) {
    public ModelInfo withId(String newId) {
        return new ModelInfo(newId, provider, aliases, supportsTools, supportsReasoning);
    }
}
