package com.jay.mcp.descriptor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpToolDescriptor(
    @JsonProperty("server_name") String serverName,
    @JsonProperty("tool_name") String toolName,
    @JsonProperty("qualified_name") String qualifiedName,
    String description
) {}
