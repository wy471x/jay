package com.jay.config.subconfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class FleetRolePreset {
    private String description;
    @JsonProperty("tool_profile") private String toolProfile;
    private List<String> tools = List.of();
    private List<String> capabilities = List.of();
    @JsonProperty("timeout_seconds") private int timeoutSeconds;
    @JsonProperty("trust_level") private String trustLevel;

    public String description() { return description; }

    public void description(String v) { description = v; }

    public String toolProfile() { return toolProfile; }

    public void toolProfile(String v) { toolProfile = v; }

    public List<String> tools() { return tools; }

    public void tools(List<String> v) { tools = v; }

    public List<String> capabilities() { return capabilities; }

    public void capabilities(List<String> v) { capabilities = v; }

    public int timeoutSeconds() { return timeoutSeconds; }

    public void timeoutSeconds(int v) { timeoutSeconds = v; }

    public String trustLevel() { return trustLevel; }

    public void trustLevel(String v) { trustLevel = v; }
}
