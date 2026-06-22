package com.jay.config.model;

import com.jay.agent.ProviderKind;

/**
 * CLI flag values carried into runtime config resolution.
 * Equivalent to Rust's CliRuntimeOverrides.
 */
public class CliRuntimeOverrides {
    private ProviderKind provider;
    private String model;
    private String apiKey;
    private String baseUrl;
    private String authMode;
    private String outputMode;
    private String logLevel;
    private Boolean telemetry;
    private String approvalPolicy;
    private String sandboxMode;
    private Boolean yolo;
    private String verbosity;

    public ProviderKind provider() { return provider; }

    public CliRuntimeOverrides provider(ProviderKind v) {
            provider = v;
            return this;
        }

    public String model() { return model; }

    public CliRuntimeOverrides model(String v) {
            model = v;
            return this;
        }

    public String apiKey() { return apiKey; }

    public CliRuntimeOverrides apiKey(String v) {
            apiKey = v;
            return this;
        }

    public String baseUrl() { return baseUrl; }

    public CliRuntimeOverrides baseUrl(String v) {
            baseUrl = v;
            return this;
        }

    public String authMode() { return authMode; }

    public CliRuntimeOverrides authMode(String v) {
            authMode = v;
            return this;
        }

    public String outputMode() { return outputMode; }

    public CliRuntimeOverrides outputMode(String v) {
            outputMode = v;
            return this;
        }

    public String logLevel() { return logLevel; }

    public CliRuntimeOverrides logLevel(String v) {
            logLevel = v;
            return this;
        }

    public Boolean telemetry() { return telemetry; }

    public CliRuntimeOverrides telemetry(Boolean v) {
            telemetry = v;
            return this;
        }

    public String approvalPolicy() { return approvalPolicy; }

    public CliRuntimeOverrides approvalPolicy(String v) {
            approvalPolicy = v;
            return this;
        }

    public String sandboxMode() { return sandboxMode; }

    public CliRuntimeOverrides sandboxMode(String v) {
            sandboxMode = v;
            return this;
        }

    public Boolean yolo() { return yolo; }

    public CliRuntimeOverrides yolo(Boolean v) {
            yolo = v;
            return this;
        }

    public String verbosity() { return verbosity; }

    public CliRuntimeOverrides verbosity(String v) {
            verbosity = v;
            return this;
        }
}
