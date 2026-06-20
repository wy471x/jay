package com.jay.mcp.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ToolFilter(
    @JsonProperty(defaultValue = "[]") List<String> allow,
    @JsonProperty(defaultValue = "[]") List<String> deny
) {
    public static final ToolFilter EMPTY = new ToolFilter(List.of(), List.of());
}
