package com.jay.config.subconfig;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HarnessProfile {
    @JsonProperty("provider_route") private String providerRoute;
    @JsonProperty("model_pattern") private String modelPattern;
    private HarnessPosture posture;
    public String providerRoute() { return providerRoute; }
    public void providerRoute(String v) { providerRoute = v; }
    public String modelPattern() { return modelPattern; }
    public void modelPattern(String v) { modelPattern = v; }
    public HarnessPosture posture() { return posture; }
    public void posture(HarnessPosture v) { posture = v; }
}
