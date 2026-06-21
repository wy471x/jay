package com.jay.config.provider;

import com.jay.agent.ProviderKind;
import java.util.List;

/**
 * Static metadata for each built-in provider.
 * Equivalent to Rust's Provider trait + provider! macro entries.
 */
public record ProviderMetadata(
    ProviderKind kind,
    String id,
    String displayName,
    String defaultBaseUrl,
    String defaultModel,
    List<String> envVars,
    List<String> aliases,
    WireFormat wireFormat
) {
    public ProviderMetadata {
        if (envVars == null) envVars = List.of();
        if (aliases == null) aliases = List.of();
    }
}
