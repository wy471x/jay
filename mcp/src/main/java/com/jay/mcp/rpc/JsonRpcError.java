package com.jay.mcp.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JsonRpcError(int code, String message, JsonNode data) {

    public static JsonRpcError parseError(String detail) {
        return new JsonRpcError(-32700, "Parse error: " + detail, null);
    }

    public static JsonRpcError invalidRequest(String detail) {
        return new JsonRpcError(-32600, "Invalid Request: " + detail, null);
    }

    public static JsonRpcError methodNotFound(String method) {
        return new JsonRpcError(-32601, "Method not found: " + method, null);
    }

    public static JsonRpcError invalidParams(String detail) {
        return new JsonRpcError(-32602, "Invalid params: " + detail, null);
    }

    public static JsonRpcError internalError(String detail) {
        return new JsonRpcError(-32603, "Internal error: " + detail, null);
    }
}
