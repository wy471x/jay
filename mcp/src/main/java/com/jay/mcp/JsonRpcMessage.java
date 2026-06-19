package com.jay.mcp;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * JSON-RPC 2.0 message types for MCP protocol communication.
 * Sealed class hierarchy with Jackson polymorphic serialization.
 */
public sealed interface JsonRpcMessage {

    String jsonrpc();

    record Request(String jsonrpc, String id, String method, JsonNode params)
            implements JsonRpcMessage {}

    record Response(String jsonrpc, String id, JsonNode result, JsonNode error)
            implements JsonRpcMessage {}

    record Notification(String jsonrpc, String method, JsonNode params)
            implements JsonRpcMessage {}
}
