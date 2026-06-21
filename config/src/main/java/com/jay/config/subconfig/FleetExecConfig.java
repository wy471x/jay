package com.jay.config.subconfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class FleetExecConfig {
    @JsonProperty("allowed_tools") private List<String> allowedTools = List.of();
    @JsonProperty("disallowed_tools") private List<String> disallowedTools = List.of();
    @JsonProperty("max_turns") private int maxTurns;
    @JsonProperty("max_spawn_depth") private int maxSpawnDepth;
    @JsonProperty("append_system_prompt") private String appendSystemPrompt;
    @JsonProperty("output_format") private String outputFormat;
    public List<String> allowedTools() { return allowedTools; }
    public void allowedTools(List<String> v) { allowedTools = v; }
    public List<String> disallowedTools() { return disallowedTools; }
    public void disallowedTools(List<String> v) { disallowedTools = v; }
    public int maxTurns() { return maxTurns; }
    public void maxTurns(int v) { maxTurns = v; }
    public int maxSpawnDepth() { return maxSpawnDepth; }
    public void maxSpawnDepth(int v) { maxSpawnDepth = v; }
    public String appendSystemPrompt() { return appendSystemPrompt; }
    public void appendSystemPrompt(String v) { appendSystemPrompt = v; }
    public String outputFormat() { return outputFormat; }
    public void outputFormat(String v) { outputFormat = v; }
}
