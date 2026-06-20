package com.jay.mcp.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JsonRpcResponse(
    String jsonrpc,
    JsonNode id,
    JsonNode result,
    JsonRpcError error
) {
    public static JsonRpcResponse success(JsonNode id, JsonNode result) {
        return new JsonRpcResponse("2.0", id, result, null);
    }

    public static JsonRpcResponse error(JsonNode id, JsonRpcError error) {
        return new JsonRpcResponse("2.0", id, null, error);
    }
}
