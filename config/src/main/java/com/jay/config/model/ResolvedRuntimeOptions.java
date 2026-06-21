package com.jay.config.model;

import com.jay.agent.ProviderKind;
import com.jay.config.provider.ProviderSource;
import java.util.Map;

/**
 * Output of full config resolution (CLI > env > config file > secret store > defaults).
 * Equivalent to Rust's ResolvedRuntimeOptions.
 */
public record ResolvedRuntimeOptions(
    ProviderKind provider,
    ProviderSource providerSource,
    String model,
    String apiKey,
    RuntimeApiKeySource apiKeySource,
    String baseUrl,
    String authMode,
    boolean insecureSkipTlsVerify,
    String outputMode,
    String logLevel,
    boolean telemetry,
    String approvalPolicy,
    String sandboxMode,
    Boolean yolo,
    String verbosity,
    Map<String, String> httpHeaders
) {}
