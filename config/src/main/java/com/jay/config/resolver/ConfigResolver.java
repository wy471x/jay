package com.jay.config.resolver;

import com.jay.agent.ProviderKind;
import com.jay.config.model.CliRuntimeOverrides;
import com.jay.config.model.ResolvedRuntimeOptions;
import com.jay.config.model.RuntimeApiKeySource;
import com.jay.config.provider.ProviderDefaults;
import com.jay.config.provider.ProviderMetadata;
import com.jay.config.provider.ProviderRegistry;
import com.jay.config.provider.ProviderSource;
import com.jay.config.store.ConfigStore;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Four-tier precedence resolution: CLI flag → Config file → Secret store → Environment.
 * Equivalent to Rust's ConfigToml::resolve_runtime_options_with_secrets().
 */
public class ConfigResolver {

    private final ConfigStore store;

    public ConfigResolver(ConfigStore store) {
        this.store = store;
    }

    /**
     * Full resolution with optional secret store lookup.
     *
     * @param cli             CLI flag overrides (may be null)
     * @param secretResolver  secret store lookup (may be null), keyed by provider name
     */
    public ResolvedRuntimeOptions resolve(CliRuntimeOverrides cli,
                                    Function<String, Optional<String>> secretResolver) {
        ResolutionTracker tracker = new ResolutionTracker();

        ProviderKind provider = resolveProvider(cli, tracker);
        ProviderMetadata meta = ProviderRegistry.get(provider);

        String apiKey = resolveApiKey(cli, provider, tracker, secretResolver);
        String baseUrl = resolveBaseUrl(cli, provider, meta);
        String model = resolveModel(cli, provider, meta);

        return new ResolvedRuntimeOptions(
            provider, tracker.providerSource,
            model, apiKey, tracker.apiKeySource,
            baseUrl,
            first(cli != null ? cli.authMode() : null,
                env("DEEPSEEK_AUTH_MODE"), store.getValue("auth_mode")),
            false, // insecureSkipTlsVerify
            first(cli != null ? cli.outputMode() : null,
                env("DEEPSEEK_OUTPUT_MODE"), store.getValue("output_mode")),
            first(cli != null && cli.logLevel() != null ? cli.logLevel() : null,
                env("DEEPSEEK_LOG_LEVEL"), store.getValue("log_level")),
            cli != null && cli.telemetry() != null ? cli.telemetry()
                : "true".equalsIgnoreCase(env("DEEPSEEK_TELEMETRY"))
                  || "true".equalsIgnoreCase(store.getValue("telemetry")),
            first(cli != null ? cli.approvalPolicy() : null,
                env("DEEPSEEK_APPROVAL_POLICY"), store.getValue("approval_policy")),
            first(cli != null ? cli.sandboxMode() : null,
                env("DEEPSEEK_SANDBOX_MODE"), store.getValue("sandbox_mode")),
            cli != null ? cli.yolo() : "true".equalsIgnoreCase(env("DEEPSEEK_YOLO")) ? true : null,
            first(cli != null ? cli.verbosity() : null,
                env("CODEWHALE_VERBOSITY"), env("DEEPSEEK_VERBOSITY"),
                store.getValue("verbosity")),
            Map.of()
        );
    }

    // ── Per-field resolution ──────────────────────────────────────

    private ProviderKind resolveProvider(CliRuntimeOverrides cli, ResolutionTracker t) {
        if (cli != null && cli.provider() != null) {
            t.providerSource = new ProviderSource.Cli();
            return cli.provider();
        }
        String envProv = first(env("CODEWHALE_PROVIDER"), env("DEEPSEEK_PROVIDER"));
        if (envProv != null) {
            t.providerSource = new ProviderSource.Env("CODEWHALE_PROVIDER");
            return ProviderKind.parse(envProv);
        }
        String cfgProv = store.getValue("provider");
        if (cfgProv != null) {
            t.providerSource = new ProviderSource.Config();
            ProviderKind pk = ProviderKind.parse(cfgProv);
            if (pk != null) return pk;
        }
        t.providerSource = new ProviderSource.Config();
        return ProviderKind.DEEPSEEK;
    }

    private String resolveApiKey(CliRuntimeOverrides cli, ProviderKind provider,
                                  ResolutionTracker t,
                                  Function<String, Optional<String>> secretResolver) {
        // CLI
        if (cli != null && cli.apiKey() != null && !cli.apiKey().isBlank()) {
            t.apiKeySource = RuntimeApiKeySource.CLI;
            return cli.apiKey();
        }
        // Config file (provider-specific, then root)
        String key = store.getValue("providers." + ConfigStore.providerKey(provider) + ".api_key");
        if (key == null) key = store.getValue("api_key");
        if (key != null) {
            t.apiKeySource = RuntimeApiKeySource.CONFIG_FILE;
            return key;
        }
        // Secret store (skip for local providers)
        if (!ProviderDefaults.shouldSkipSecretStore(provider) && secretResolver != null) {
            Optional<String> secret = secretResolver.apply(provider.name().toLowerCase());
            if (secret.isPresent()) {
                t.apiKeySource = RuntimeApiKeySource.KEYRING;
                return secret.get();
            }
        }
        // Environment — try provider-specific env vars
        ProviderMetadata pm = ProviderRegistry.get(provider);
        if (pm != null) {
            for (String var : pm.envVars()) {
                String val = env(var);
                if (val != null && !val.isBlank()) {
                    t.apiKeySource = RuntimeApiKeySource.ENV;
                    return val;
                }
            }
        }
        t.apiKeySource = null;
        return null;
    }

    private String resolveBaseUrl(CliRuntimeOverrides cli, ProviderKind provider, ProviderMetadata meta) {
        if (cli != null && cli.baseUrl() != null) return cli.baseUrl();
        String envKey = provider.name().toUpperCase() + "_BASE_URL";
        String envVal = env(envKey);
        if (envVal == null) envVal = env("CODEWHALE_BASE_URL");
        if (envVal != null) return envVal;
        String cfg = store.getValue("providers." + ConfigStore.providerKey(provider) + ".base_url");
        if (cfg == null) cfg = store.getValue("base_url");
        if (cfg != null) return cfg;
        if (meta != null) return meta.defaultBaseUrl();
        return "https://api.deepseek.com/beta";
    }

    private String resolveModel(CliRuntimeOverrides cli, ProviderKind provider, ProviderMetadata meta) {
        if (cli != null && cli.model() != null) return cli.model();
        String envVal = env(provider.name().toUpperCase() + "_MODEL");
        if (envVal == null) envVal = env("CODEWHALE_MODEL");
        if (envVal != null) return envVal;
        String cfg = store.getValue("model");
        if (cfg != null) return cfg;
        if (meta != null) return meta.defaultModel();
        return "deepseek-v4-pro";
    }

    // ── Helpers ───────────────────────────────────────────────────

    private static String env(String name) {
        if (name == null) return null;
        String val = System.getenv(name);
        return val != null && !val.isBlank() ? val : null;
    }

    @SafeVarargs
    private static <T> T first(T... values) {
        for (T v : values) if (v != null && !(v instanceof String s && s.isBlank())) return v;
        return null;
    }

    private static class ResolutionTracker {
        ProviderSource providerSource;
        RuntimeApiKeySource apiKeySource;
    }
}
