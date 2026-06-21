package com.jay.config.subconfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FleetConfig {
    @JsonProperty("default_trust_level") private String defaultTrustLevel;
    @JsonProperty("require_identity_verification") private boolean requireIdentityVerification = true;
    @JsonProperty("max_trust_level") private String maxTrustLevel;
    private Map<String, FleetRolePreset> roles = new LinkedHashMap<>();
    private FleetExecConfig exec;
    public String defaultTrustLevel() { return defaultTrustLevel; }
    public void defaultTrustLevel(String v) { defaultTrustLevel = v; }
    public boolean requireIdentityVerification() { return requireIdentityVerification; }
    public void requireIdentityVerification(boolean v) { requireIdentityVerification = v; }
    public String maxTrustLevel() { return maxTrustLevel; }
    public void maxTrustLevel(String v) { maxTrustLevel = v; }
    public Map<String, FleetRolePreset> roles() { return roles; }
    public void roles(Map<String, FleetRolePreset> v) { roles = v; }
    public FleetExecConfig exec() { return exec; }
    public void exec(FleetExecConfig v) { exec = v; }
}
