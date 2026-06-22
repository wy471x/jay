package com.jay.mcp.rpc;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record JsonRpcRequest(
    @JsonProperty("jsonrpc") String jsonrpc,
    JsonNode id,
    String method,
    JsonNode params
) { }
