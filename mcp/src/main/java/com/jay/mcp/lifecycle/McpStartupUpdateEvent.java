package com.jay.mcp.lifecycle;

import com.fasterxml.jackson.annotation.JsonProperty;

public record McpStartupUpdateEvent(
    @JsonProperty("server_name") String serverName,
    McpStartupStatus status
) { }
