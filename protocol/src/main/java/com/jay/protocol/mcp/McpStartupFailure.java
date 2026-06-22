package com.jay.protocol.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;

public record McpStartupFailure(@JsonProperty("server_name") String serverName, String error) { }
