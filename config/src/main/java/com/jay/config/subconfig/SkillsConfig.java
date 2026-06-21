package com.jay.config.subconfig;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SkillsConfig {
    @JsonProperty("registry_url") private String registryUrl;
    @JsonProperty("max_install_size_bytes") private Long maxInstallSizeBytes;
    public String registryUrl() { return registryUrl; }
    public void registryUrl(String v) { registryUrl = v; }
    public Long maxInstallSizeBytes() { return maxInstallSizeBytes; }
    public void maxInstallSizeBytes(Long v) { maxInstallSizeBytes = v; }
}
