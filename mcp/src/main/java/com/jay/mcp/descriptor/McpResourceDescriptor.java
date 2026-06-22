package com.jay.mcp.descriptor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpResourceDescriptor(
    @JsonProperty("server_name") String serverName,
    String uri,
    String description
) { }
