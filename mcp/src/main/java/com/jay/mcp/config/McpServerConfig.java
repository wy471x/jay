package com.jay.mcp.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record McpServerConfig(
    String name,
    String command,
    List<String> args,
    Map<String, String> env,
    boolean enabled
) {
    @JsonCreator
    public static McpServerConfig create(
            @JsonProperty("name") String name,
            @JsonProperty("command") String command,
            @JsonProperty("args") List<String> args,
            @JsonProperty("env") Map<String, String> env,
            @JsonProperty("enabled") Boolean enabled) {
        return new McpServerConfig(
            name,
            command,
            args != null ? args : List.of(),
            env != null ? env : Map.of(),
            enabled != null ? enabled : true);
    }
}
