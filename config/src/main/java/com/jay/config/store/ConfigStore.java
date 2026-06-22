package com.jay.config.store;

import com.fasterxml.jackson.annotation.JsonAnyGetter;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;

import com.jay.agent.ProviderKind;

import com.jay.config.model.CliRuntimeOverrides;

import com.jay.config.model.ResolvedRuntimeOptions;

import com.jay.config.model.RuntimeApiKeySource;

import com.jay.config.provider.ProviderDefaults;

import com.jay.config.provider.ProviderSource;

import com.jay.config.util.ConfigPathResolver;

import com.jay.execpolicy.ExecPolicyEngine;

import com.jay.execpolicy.Ruleset;

import com.jay.execpolicy.ToolAskRule;

import java.io.IOException;

import java.nio.file.Files;

import java.nio.file.Path;

import java.nio.file.StandardCopyOption;

import java.nio.file.attribute.PosixFilePermissions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**

 * File-based configuration store. Loads/saves a TOML config file with

 * Unix 0600 permissions, atomic writes, and backup creation.

 *

 * <p>Equivalent to Rust's ConfigStore.

 */

public class ConfigStore {

    private static final TomlMapper TOML_MAPPER = TomlMapper.builder().build();

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final Path path;

    private ConfigBlob config;

    private ConfigBlob permissions;

    private String lastSavedHash;

    public ConfigStore(Path path) {

        this.path = path.toAbsolutePath();

        this.config = new ConfigBlob();

        this.permissions = new ConfigBlob();

    }

    /** Load config from the resolved path, with migration if needed. */

    public static ConfigStore load(Path explicit) throws IOException {

        ConfigPathResolver.migrateIfNeeded();

        Path configPath = ConfigPathResolver.resolveConfigPath(explicit);

        Path permsPath = ConfigPathResolver.resolvePermissionsPath(configPath);

        ConfigStore store = new ConfigStore(configPath);

        if (Files.exists(configPath)) {

            try {

                String raw = Files.readString(configPath);

                store.config = TOML_MAPPER.readValue(raw, ConfigBlob.class);

            } catch (IOException e) {

                // Corrupt config — start fresh

                store.config = new ConfigBlob();

            }

        }

        if (Files.exists(permsPath)) {

            try {

                String raw = Files.readString(permsPath);

                store.permissions = TOML_MAPPER.readValue(raw, ConfigBlob.class);

            } catch (IOException e) {

                store.permissions = new ConfigBlob();

            }

        }

        store.lastSavedHash = hashOf(store.config);

        return store;

    }

    public Path path() { return path; }

    public ConfigBlob config() { return config; }

    public ConfigBlob permissions() { return permissions; }

    public Path permissionsPath() { return path.resolveSibling(ConfigPathResolver.PERMISSIONS_FILE_NAME); }

    // ── Save ───────────────────────────────────────────────────────

    /** Save config to disk. No-op if content unchanged. Creates .bak on first change. */

    public void save() throws IOException {

        String newHash = hashOf(config);

        if (newHash.equals(lastSavedHash)) return;

        Path dir = path.getParent();

        Files.createDirectories(dir);

        String toml = TOML_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(config);

        // Create backup on first change

        Path bak = path.resolveSibling(path.getFileName() + ".bak");

        if (!Files.exists(bak) && Files.exists(path)) {

            Files.copy(path, bak);

        }

        // Atomic write

        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");

        Files.writeString(tmp, toml);

        if (!ConfigPathResolver.isWindows()) {

            try {

                Files.setPosixFilePermissions(tmp,

                    PosixFilePermissions.fromString("rw-------"));

            } catch (IOException ignored) { }

        }

        Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

        if (!ConfigPathResolver.isWindows()) {

            try {

                Files.setPosixFilePermissions(dir,

                    PosixFilePermissions.fromString("rwx------"));

            } catch (IOException ignored) { }

        }

        lastSavedHash = newHash;

    }

    // ── Key-value access ──────────────────────────────────────────

    /** Get a raw config value by dotted key path. Returns null if not found. */

    public String getValue(String key) {

        return resolveKey(config.entries, key);

    }

    /**

     * Get a display-safe config value. Keys ending with {@code .api_key}

     * or exactly {@code api_key} are redacted: first 4 + last 4 chars for

     * keys longer than 16, otherwise {@code ********}.

     */

    public String getDisplayValue(String key) {

        String raw = resolveKey(config.entries, key);

        if (raw == null) return null;

        if (key.endsWith(".api_key") || key.equals("api_key")) {

            return redact(raw);

        }

        return raw;

    }

    /** Set a config value by dotted key path. Flattened keys are written as nested maps. */

    public void setValue(String key, String value) {

        setNestedKey(config.entries, key, value);

    }

    /** Remove a config value by dotted key path. */

    public void unsetValue(String key) {

        unsetNestedKey(config.entries, key);

    }

    /** List all config values with redacted secrets. */

    public Map<String, String> listValues() {

        Map<String, String> result = new TreeMap<>();

        flatten(config.entries, "", result);

        return result;

    }

    // ── Nested key resolution ─────────────────────────────────────

    @SuppressWarnings("unchecked")

    public static String resolveKey(Map<String, Object> root, String key) {

        String[] parts = key.split("\\.");

        Object current = root;

        for (int i = 0; i < parts.length; i++) {

            if (!(current instanceof Map)) return null;

            current = ((Map<String, Object>) current).get(parts[i]);

            if (current == null && i < parts.length - 1) return null;

        }

        return current instanceof String ? (String) current

            : current != null ? String.valueOf(current) : null;

    }

    @SuppressWarnings("unchecked")

    public static void setNestedKey(Map<String, Object> root, String key, String value) {

        String[] parts = key.split("\\.");

        Map<String, Object> current = root;

        for (int i = 0; i < parts.length - 1; i++) {

            Object next = current.get(parts[i]);

            if (!(next instanceof Map)) {

                next = new LinkedHashMap<String, Object>();

                current.put(parts[i], next);

            }

            current = (Map<String, Object>) next;

        }

        current.put(parts[parts.length - 1], value);

    }

    @SuppressWarnings("unchecked")

    public static void unsetNestedKey(Map<String, Object> root, String key) {

        String[] parts = key.split("\\.");

        Map<String, Object> current = root;

        for (int i = 0; i < parts.length - 1; i++) {

            Object next = current.get(parts[i]);

            if (!(next instanceof Map)) return;

            current = (Map<String, Object>) next;

        }

        current.remove(parts[parts.length - 1]);

    }

    @SuppressWarnings("unchecked")

    static void flatten(Map<String, Object> node, String prefix, Map<String, String> out) {

        for (var entry : node.entrySet()) {

            String fullKey = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();

            Object value = entry.getValue();

            if (value instanceof Map) {

                flatten((Map<String, Object>) value, fullKey, out);

            } else {

                String display = fullKey.endsWith(".api_key") || fullKey.equals("api_key")

                    ? redact(String.valueOf(value)) : String.valueOf(value);

                out.put(fullKey, display);

            }

        }

    }

    // ── Helpers ───────────────────────────────────────────────────

    public static String redact(String value) {

        if (value == null || value.length() <= 16) return "********";

        return value.substring(0, 4) + "********" + value.substring(value.length() - 4);

    }

    private static String hashOf(ConfigBlob blob) {

        try {

            String json = JSON_MAPPER.writeValueAsString(blob.entries);

            return Integer.toHexString(json.hashCode());

        } catch (Exception e) {

            return "";

        }

    }

    // ── Runtime resolution ────────────────────────────────────────

    /**

     * Full precedence resolution: CLI > env > config file > secret store > defaults.

     * Equivalent to Rust's ConfigToml::resolve_runtime_options_with_secrets().

     */

    public ResolvedRuntimeOptions resolveRuntimeOptions(CliRuntimeOverrides cli,

            java.util.function.Function<String, Optional<String>> secretResolver) {

        // Provider: CLI > env > config

        ProviderKind provider = cli != null ? cli.provider() : null;

        ProviderSource providerSource = null;

        if (provider != null) {

            providerSource = new ProviderSource.Cli();

        }

        if (provider == null) {

            String envProvider = System.getenv("CODEWHALE_PROVIDER");

            if (envProvider == null) envProvider = System.getenv("DEEPSEEK_PROVIDER");

            if (envProvider != null && !envProvider.isBlank()) {

                provider = ProviderKind.parse(envProvider);

                providerSource = new ProviderSource.Env("CODEWHALE_PROVIDER");

            }

        }

        if (provider == null) {

            String cfgProvider = getValue("provider");

            if (cfgProvider != null && !cfgProvider.isBlank()) {

                provider = ProviderKind.parse(cfgProvider);

                providerSource = new ProviderSource.Config();

            }

        }

        if (provider == null) {

            provider = ProviderKind.DEEPSEEK;

            providerSource = new ProviderSource.Config();

        }

        // Model: CLI > env > config > provider default

        String model = cli != null ? cli.model() : null;

        if (model == null) model = envForProvider(provider, "MODEL");

        if (model == null) model = getValue("model");

        if (model == null) model = ProviderDefaults.defaultModel(provider);

        // API key: CLI > config > secret store > env

        String apiKey = cli != null ? cli.apiKey() : null;

        RuntimeApiKeySource apiKeySource = null;

        if (apiKey != null) {

            apiKeySource = RuntimeApiKeySource.CLI;

        }

        if (apiKey == null) {

            apiKey = getValue("providers." + providerKey(provider) + ".api_key");

            if (apiKey == null) apiKey = getValue("api_key");

            if (apiKey != null) apiKeySource = RuntimeApiKeySource.CONFIG_FILE;

        }

        if (apiKey == null && !ProviderDefaults.shouldSkipSecretStore(provider)

                && secretResolver != null) {

            apiKey = secretResolver.apply(provider.name().toLowerCase()).orElse(null);

            if (apiKey != null) apiKeySource = RuntimeApiKeySource.KEYRING;

        }

        if (apiKey == null) {

            apiKey = envLookupApiKey(provider);

            if (apiKey != null) apiKeySource = RuntimeApiKeySource.ENV;

        }

        // Base URL: CLI > env > config > provider default

        String baseUrl = cli != null ? cli.baseUrl() : null;

        if (baseUrl == null) baseUrl = envForProvider(provider, "BASE_URL");

        if (baseUrl == null) baseUrl = getValue("providers." + providerKey(provider) + ".base_url");

        if (baseUrl == null) baseUrl = getValue("base_url");

        if (baseUrl == null) baseUrl = ProviderDefaults.defaultBaseUrl(provider);

        // Other fields: CLI > env > config

        String outputMode = first(cli != null ? cli.outputMode() : null,

            System.getenv("DEEPSEEK_OUTPUT_MODE"), getValue("output_mode"));

        String logLevel = first(cli != null && cli.logLevel() != null ? cli.logLevel() : null,

            System.getenv("DEEPSEEK_LOG_LEVEL"), getValue("log_level"));

        boolean telemetry = cli != null && cli.telemetry() != null ? cli.telemetry()

            : "true".equalsIgnoreCase(System.getenv("DEEPSEEK_TELEMETRY"))

            || "true".equalsIgnoreCase(getValue("telemetry"));

        String approvalPolicy = first(cli != null ? cli.approvalPolicy() : null,

            System.getenv("DEEPSEEK_APPROVAL_POLICY"), getValue("approval_policy"));

        String sandboxMode = first(cli != null ? cli.sandboxMode() : null,

            System.getenv("DEEPSEEK_SANDBOX_MODE"), getValue("sandbox_mode"));

        Boolean yolo = cli != null ? cli.yolo() : null;

        if (yolo == null && "true".equalsIgnoreCase(System.getenv("DEEPSEEK_YOLO"))) yolo = true;

        String verbosity = first(cli != null ? cli.verbosity() : null,

            System.getenv("CODEWHALE_VERBOSITY"), System.getenv("DEEPSEEK_VERBOSITY"),

            getValue("verbosity"));

        String authMode = first(cli != null ? cli.authMode() : null,

            System.getenv("DEEPSEEK_AUTH_MODE"), getValue("auth_mode"));

        return new ResolvedRuntimeOptions(provider, providerSource != null ? providerSource : new ProviderSource.Config(),

            model, apiKey, apiKeySource, baseUrl, authMode, false,

            outputMode, logLevel, telemetry, approvalPolicy, sandboxMode,

            yolo, verbosity, Map.of());

    }

    /**

     * Build an ExecPolicyEngine from the sibling permissions.toml.

     * Equivalent to Rust's ConfigStore::exec_policy_engine().

     */

    public ExecPolicyEngine execPolicyEngine() {

        if (permissions == null || permissions.entries.isEmpty()) {

            return new ExecPolicyEngine(List.of(), List.of());

        }

        List<ToolAskRule> rules = new ArrayList<>();

        @SuppressWarnings("unchecked")

        Object rulesObj = permissions.entries.get("rules");

        if (rulesObj instanceof List<?> list) {

            for (Object item : list) {

                if (item instanceof Map<?, ?> m) {

                    String tool = Objects.toString(m.get("tool"), null);

                    String command = Objects.toString(m.get("command"), null);

                    String path = Objects.toString(m.get("path"), null);

                    if (tool != null) rules.add(new ToolAskRule(tool, command, path));

                }

            }

        }

        return new ExecPolicyEngine(

            List.of(new Ruleset(com.jay.execpolicy.RulesetLayer.User, List.of(), List.of(), rules)),

            List.of(), List.of());

    }

    // ── Project overrides ──────────────────────────────────────────

    /**

     * Safely merge untrusted project-level config. Only non-sensitive

     * fields are merged; approval/sandbox policies are tighten-only.

     * Equivalent to Rust's ConfigToml::merge_project_overrides().

     */

    public void mergeProjectOverrides(ConfigStore project) {

        if (project == null) return;

        for (String key : List.of("model", "output_mode", "verbosity", "log_level")) {

            String val = project.getValue(key);

            if (val != null && !val.isBlank()) setValue(key, val);

        }

        // Approval/sandbox: only tighten (never loosen)

        String projApproval = project.getValue("approval_policy");

        if (projApproval != null && isStricter(projApproval, getValue("approval_policy"))) {

            setValue("approval_policy", projApproval);

        }

        String projSandbox = project.getValue("sandbox_mode");

        if (projSandbox != null && isStricter(projSandbox, getValue("sandbox_mode"))) {

            setValue("sandbox_mode", projSandbox);

        }

    }

    // ── Permissions management ───────────────────────────────────

    /**

     * Atomically append ask-only rules to the sibling permissions.toml.

     * Returns the number of new rules added (duplicates are skipped).

     * Equivalent to Rust's ConfigStore::append_ask_rules().

     */

    public int appendAskRules(List<ToolAskRule> rules) throws IOException {

        if (rules == null || rules.isEmpty()) return 0;

        Path permPath = permissionsPath();

        // Load existing rules

        var existing = new ArrayList<ToolAskRule>();

        if (Files.exists(permPath)) {

            try {

                String raw = Files.readString(permPath);

                ConfigBlob blob = TOML_MAPPER.readValue(raw, ConfigBlob.class);

                @SuppressWarnings("unchecked")

                Object rulesObj = blob.entries.get("rules");

                if (rulesObj instanceof List<?> list) {

                    for (Object item : list) {

                        if (item instanceof Map<?, ?> m) {

                            String tool = Objects.toString(m.get("tool"), null);

                            String command = Objects.toString(m.get("command"), null);

                            String path = Objects.toString(m.get("path"), null);

                            if (tool != null) existing.add(new ToolAskRule(tool, command, path));

                        }

                    }

                }

            } catch (IOException e) {

                // Corrupt permissions — start fresh

            }

        }

        // Deduplicate and append

        int added = 0;

        for (ToolAskRule rule : rules) {

            if (!containsRule(existing, rule)) {

                existing.add(rule);

                added++;

            }

        }

        if (added == 0) return 0;

        // Build new permissions blob

        ConfigBlob newPerms = new ConfigBlob();

        List<Map<String, Object>> ruleList = new ArrayList<>();

        for (ToolAskRule rule : existing) {

            Map<String, Object> entry = new LinkedHashMap<>();

            entry.put("tool", rule.tool());

            if (rule.command() != null) entry.put("command", rule.command());

            if (rule.path() != null) entry.put("path", rule.path());

            ruleList.add(entry);

        }

        newPerms.entries.put("rules", ruleList);

        // Atomic write

        Files.createDirectories(permPath.getParent());

        Path tmp = permPath.resolveSibling(permPath.getFileName() + ".tmp");

        String toml = TOML_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(newPerms);

        Files.writeString(tmp, toml);

        Files.move(tmp, permPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

        // Refresh in-memory snapshot

        this.permissions = newPerms;

        return added;

    }

    private static boolean containsRule(List<ToolAskRule> existing, ToolAskRule rule) {

        for (ToolAskRule r : existing) {

            if (Objects.equals(r.tool(), rule.tool())

                && Objects.equals(r.command(), rule.command())

                && Objects.equals(r.path(), rule.path())) {

                return true;

            }

        }

        return false;

    }

    // ── Internal helpers ──────────────────────────────────────────

    @SafeVarargs

    private static <T> T first(T... values) {

        for (T v : values) if (v != null && !(v instanceof String s && s.isBlank())) return v;

        return null;

    }

    /** Inline API key env lookup to avoid circular dep on :secrets. */

    private static String envLookupApiKey(ProviderKind kind) {

        return switch (kind) {

            case DEEPSEEK -> System.getenv("DEEPSEEK_API_KEY");

            case OPENROUTER -> System.getenv("OPENROUTER_API_KEY");

            case OPENAI -> System.getenv("OPENAI_API_KEY");

            case ANTHROPIC -> System.getenv("ANTHROPIC_API_KEY");

            case NVIDIA_NIM -> first(System.getenv("NVIDIA_API_KEY"), System.getenv("NVIDIA_NIM_API_KEY"));

            case FIREWORKS -> System.getenv("FIREWORKS_API_KEY");

            case SILICONFLOW, SILICONFLOW_CN -> System.getenv("SILICONFLOW_API_KEY");

            case ARCEE -> System.getenv("ARCEE_API_KEY");

            case MOONSHOT -> first(System.getenv("MOONSHOT_API_KEY"), System.getenv("KIMI_API_KEY"));

            case SGLANG -> System.getenv("SGLANG_API_KEY");

            case VLLM -> System.getenv("VLLM_API_KEY");

            case OLLAMA -> System.getenv("OLLAMA_API_KEY");

            case NOVITA -> System.getenv("NOVITA_API_KEY");

            case HUGGINGFACE -> first(System.getenv("HF_API_KEY"), System.getenv("HUGGINGFACE_API_KEY"));

            case TOGETHER -> System.getenv("TOGETHER_API_KEY");

            case ATLASCLOUD -> System.getenv("ATLASCLOUD_API_KEY");

            case VOLCENGINE -> first(System.getenv("VOLCENGINE_API_KEY"), System.getenv("VOLCENGINE_ARK_API_KEY"), System.getenv("ARK_API_KEY"));

            case XIAOMI_MIMO -> first(System.getenv("XIAOMI_MIMO_API_KEY"), System.getenv("XIAOMI_API_KEY"), System.getenv("MIMO_API_KEY"));

            case WANJIE_ARK -> first(System.getenv("WANJIE_ARK_API_KEY"), System.getenv("WANJIE_API_KEY"));

            case ZAI -> System.getenv("ZAI_API_KEY");

            case STEPFUN -> System.getenv("STEPFUN_API_KEY");

            case MINIMAX -> System.getenv("MINIMAX_API_KEY");

            case DEEPINFRA -> System.getenv("DEEPINFRA_API_KEY");

            case OPENAI_CODEX -> first(System.getenv("OPENAI_CODEX_API_KEY"), System.getenv("OPENAI_API_KEY"));

        };

    }

    private static String envForProvider(ProviderKind kind, String suffix) {

        String name = kind.name().toUpperCase();

        String val = System.getenv(name + "_" + suffix);

        if (val == null) val = System.getenv("CODEWHALE_" + suffix);

        return val;

    }

    /** Map ProviderKind to config key segment (e.g. DEEPSEEK → "deepseek"). */

    public static String providerKey(ProviderKind kind) {

        return kind.name().toLowerCase().replace('_', '-');

    }

    /** True if the proposed policy is strictly more restrictive than current. */

    static boolean isStricter(String proposed, String current) {

        if (current == null) return true;

        return rank(proposed) > rank(current);

    }

    private static int rank(String policy) {

        if (policy == null) return 0;

        return switch (policy.toLowerCase()) {

            case "auto", "yolo", "unless-trusted" -> 0;

            case "on-request" -> 1;

            case "on-failure" -> 2;

            case "never", "deny" -> 3;

            default -> 0;

        };

    }

    // ── Inner types ───────────────────────────────────────────────

    /** Flexible config blob that captures all TOML keys as a flattened map. */

    public static class ConfigBlob {

        @JsonAnySetter

        @JsonInclude(JsonInclude.Include.NON_EMPTY)

        private final Map<String, Object> entries = new LinkedHashMap<>();

        public ConfigBlob() { }

        @JsonAnyGetter

        public Map<String, Object> entries() { return entries; }

    }

}
