package com.jay.mcp.lifecycle;

import com.fasterxml.jackson.annotation.JsonProperty;

public record McpStartupFailure(
    @JsonProperty("server_name") String serverName,
    String error
) {}
