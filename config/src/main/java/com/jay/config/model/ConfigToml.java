package com.jay.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jay.agent.ProviderKind;
import com.jay.config.subconfig.*;
import java.util.*;

/**
 * Root configuration data model — equivalent to Rust's ConfigToml.
 * Supports both TOML deserialization and Spring @ConfigurationProperties binding.
 */
public class ConfigToml {

    @JsonProperty("api_key") private String apiKey;
    @JsonProperty("base_url") private String baseUrl;
    @JsonProperty("http_headers") private Map<String, String> httpHeaders = new LinkedHashMap<>();
    @JsonProperty("default_text_model") private String defaultTextModel;

    private ProviderKind provider = ProviderKind.DEEPSEEK;
    private String model;
    @JsonProperty("auth_mode") private String authMode;
    @JsonProperty("output_mode") private String outputMode;
    private String verbosity;
    @JsonProperty("log_level") private String logLevel;
    private Boolean telemetry;
    @JsonProperty("approval_policy") private String approvalPolicy;
    @JsonProperty("sandbox_mode") private String sandboxMode;

    @JsonProperty("tools") private ToolsConfig tools;
    @JsonProperty("providers") private ProvidersConfig providers = new ProvidersConfig();
    @JsonProperty("fallback_providers") private List<ProviderKind> fallbackProviders = new ArrayList<>();
    @JsonProperty("network") private NetworkPolicyConfig network;
    @JsonProperty("skills") private SkillsConfig skills;
    @JsonProperty("snapshots") private SnapshotsConfig snapshots = new SnapshotsConfig();
    @JsonProperty("lsp") private LspConfig lsp;
    @JsonProperty("harness_profiles") private List<HarnessProfile> harnessProfiles = new ArrayList<>();
    @JsonProperty("hotbar") private List<HotbarBinding> hotbar;
    @JsonProperty("hook_sinks") private HookSinksConfig hookSinks;
    @JsonProperty("fleet") private FleetConfig fleet;

    // Extras: catch-all for backward-compatible unknown fields
    private Map<String, Object> extras = new LinkedHashMap<>();

    // ── Getters / Setters ─────────────────────────────────────────

    public String apiKey() { return apiKey; }
    public void apiKey(String v) { apiKey = v; }
    public String baseUrl() { return baseUrl; }
    public void baseUrl(String v) { baseUrl = v; }
    public Map<String, String> httpHeaders() { return httpHeaders; }
    public void httpHeaders(Map<String, String> v) { httpHeaders = v; }
    public String defaultTextModel() { return defaultTextModel; }
    public void defaultTextModel(String v) { defaultTextModel = v; }
    public ProviderKind provider() { return provider; }
    public void provider(ProviderKind v) { provider = v; }
    public String model() { return model; }
    public void model(String v) { model = v; }
    public String authMode() { return authMode; }
    public void authMode(String v) { authMode = v; }
    public String outputMode() { return outputMode; }
    public void outputMode(String v) { outputMode = v; }
    public String verbosity() { return verbosity; }
    public void verbosity(String v) { verbosity = v; }
    public String logLevel() { return logLevel; }
    public void logLevel(String v) { logLevel = v; }
    public Boolean telemetry() { return telemetry; }
    public void telemetry(Boolean v) { telemetry = v; }
    public String approvalPolicy() { return approvalPolicy; }
    public void approvalPolicy(String v) { approvalPolicy = v; }
    public String sandboxMode() { return sandboxMode; }
    public void sandboxMode(String v) { sandboxMode = v; }
    public ToolsConfig tools() { return tools; }
    public void tools(ToolsConfig v) { tools = v; }
    public ProvidersConfig providers() { return providers; }
    public void providers(ProvidersConfig v) { providers = v; }
    public List<ProviderKind> fallbackProviders() { return fallbackProviders; }
    public void fallbackProviders(List<ProviderKind> v) { fallbackProviders = v; }
    public NetworkPolicyConfig network() { return network; }
    public void network(NetworkPolicyConfig v) { network = v; }
    public SkillsConfig skills() { return skills; }
    public void skills(SkillsConfig v) { skills = v; }
    public SnapshotsConfig snapshots() { return snapshots; }
    public void snapshots(SnapshotsConfig v) { snapshots = v; }
    public LspConfig lsp() { return lsp; }
    public void lsp(LspConfig v) { lsp = v; }
    public List<HarnessProfile> harnessProfiles() { return harnessProfiles; }
    public void harnessProfiles(List<HarnessProfile> v) { harnessProfiles = v; }
    public List<HotbarBinding> hotbar() { return hotbar; }
    public void hotbar(List<HotbarBinding> v) { hotbar = v; }
    public HookSinksConfig hookSinks() { return hookSinks; }
    public void hookSinks(HookSinksConfig v) { hookSinks = v; }
    public FleetConfig fleet() { return fleet; }
    public void fleet(FleetConfig v) { fleet = v; }
    public Map<String, Object> extras() { return extras; }
    public void extras(Map<String, Object> v) { extras = v; }

    // ── Merge project overrides ───────────────────────────────────

    /** Safely merge untrusted project-level config. Only non-sensitive fields allowed. */
    public void mergeProjectOverrides(ConfigToml project) {
        if (project == null) return;
        if (project.model != null) this.model = project.model;
        if (project.verbosity != null) this.verbosity = project.verbosity;
        if (project.outputMode != null) this.outputMode = project.outputMode;
        if (project.logLevel != null) this.logLevel = project.logLevel;
        if (project.tools != null) this.tools = project.tools;
        if (project.harnessProfiles != null && !project.harnessProfiles.isEmpty())
            this.harnessProfiles = project.harnessProfiles;
        if (project.hotbar != null) this.hotbar = project.hotbar;
        if (project.extras != null && !project.extras.isEmpty())
            this.extras.putAll(project.extras);
        // Only merge provider models (not credentials)
        if (project.providers != null) {
            this.providers.mergeNonSensitive(project.providers);
        }
    }
}
