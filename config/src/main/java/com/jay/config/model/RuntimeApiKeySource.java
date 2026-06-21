package com.jay.config.model;

public enum RuntimeApiKeySource {
    CLI("cli"),
    CONFIG_FILE("config"),
    KEYRING("keyring"),
    ENV("env");

    private final String envValue;

    RuntimeApiKeySource(String envValue) { this.envValue = envValue; }

    public String asEnvValue() { return envValue; }
}
