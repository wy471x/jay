package com.jay.config.subconfig;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HarnessPosture {
    private String kind;
    @JsonProperty("max_subagents") private int maxSubagents;
    @JsonProperty("prefer_codebase_search") private boolean preferCodebaseSearch;
    @JsonProperty("compaction_strategy") private String compactionStrategy;
    @JsonProperty("tool_surface") private String toolSurface;
    @JsonProperty("safety_posture") private String safetyPosture;
    public String kind() { return kind; }
    public void kind(String v) { kind = v; }
    public int maxSubagents() { return maxSubagents; }
    public void maxSubagents(int v) { maxSubagents = v; }
    public boolean preferCodebaseSearch() { return preferCodebaseSearch; }
    public void preferCodebaseSearch(boolean v) { preferCodebaseSearch = v; }
    public String compactionStrategy() { return compactionStrategy; }
    public void compactionStrategy(String v) { compactionStrategy = v; }
    public String toolSurface() { return toolSurface; }
    public void toolSurface(String v) { toolSurface = v; }
    public String safetyPosture() { return safetyPosture; }
    public void safetyPosture(String v) { safetyPosture = v; }
}
