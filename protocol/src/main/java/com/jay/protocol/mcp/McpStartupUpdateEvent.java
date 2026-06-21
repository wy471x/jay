package com.jay.protocol.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record McpStartupUpdateEvent(@JsonProperty("server_name") String serverName, McpStartupStatus status) {}
