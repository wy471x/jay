package com.jay.protocol.core;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ToolKind {
    @JsonProperty("function") FUNCTION,
    @JsonProperty("mcp") MCP
}
