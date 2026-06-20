package com.jay.mcp.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.jay.mcp.client.InMemoryMcpClient;
import com.jay.mcp.config.McpServerDefinition;
import com.jay.mcp.manager.McpManager;
import com.jay.mcp.state.StdioMcpState;

import java.io.*;
import java.util.*;

public class StdioMcpServer {

    private static final ObjectMapper mapper = new ObjectMapper();

    public List<McpServerDefinition> run(List<McpServerDefinition> initialDefinitions) {
        StdioMcpState state = buildState(initialDefinitions);

        try (BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = stdin.readLine()) != null) {
                if (line.isBlank()) continue;
                if (!handleLine(line, state)) break;
            }
        } catch (IOException e) {
            System.err.println("deepseek-mcp stdio error: " + e.getMessage());
        }

        state.setLifecycleState("stopped");
        List<McpServerDefinition> definitions = new ArrayList<>(state.definitions().values());
        definitions.sort(Comparator.comparing(d -> d.config().name()));
        return definitions;
    }

    private boolean handleLine(String line, StdioMcpState state) {
        JsonRpcRequest request;
        JsonNode id;
        try {
            request = mapper.readValue(line, JsonRpcRequest.class);
            id = request.id() != null ? request.id() : NullNode.getInstance();
            if (request.jsonrpc() == null || !request.jsonrpc().equals("2.0")) {
                writeError(id, JsonRpcError.invalidRequest("jsonrpc must be 2.0"));
                return true;
            }
        } catch (IOException e) {
            writeError(NullNode.getInstance(), JsonRpcError.parseError(e.getMessage()));
            return true;
        }

        try {
            JsonRpcDispatcher dispatcher = new JsonRpcDispatcher(state);
            JsonRpcDispatcher.DispatchResult result = dispatcher.dispatch(request.method(), request.params());
            writeSuccess(id, result.result());
            return !result.shouldExit();
        } catch (JsonRpcDispatcher.MethodNotFoundException e) {
            writeError(id, e.error());
            return true;
        } catch (JsonRpcDispatcher.JsonRpcException e) {
            writeError(id, e.error());
            return true;
        } catch (Exception e) {
            writeError(id, JsonRpcError.internalError(e.getMessage() != null ? e.getMessage() : "unknown error"));
            return true;
        }
    }

    private void writeSuccess(JsonNode id, JsonNode result) {
        try {
            System.out.println(mapper.writeValueAsString(JsonRpcResponse.success(id, result)));
        } catch (IOException e) {
            System.err.println("failed to write success response: " + e.getMessage());
        }
    }

    private void writeError(JsonNode id, JsonRpcError error) {
        try {
            System.out.println(mapper.writeValueAsString(JsonRpcResponse.error(id, error)));
        } catch (IOException e) {
            System.err.println("failed to write error response: " + e.getMessage());
        }
    }

    private StdioMcpState buildState(List<McpServerDefinition> definitions) {
        McpManager manager = new McpManager();
        Map<String, McpServerDefinition> defs = new LinkedHashMap<>();
        Map<String, Boolean> running = new LinkedHashMap<>();

        for (McpServerDefinition def : definitions) {
            String name = def.config().name();
            defs.put(name, def);
            if (def.config().enabled()) {
                manager.registerServer(def.config(), def.filter(),
                    JsonRpcDispatcher.defaultStdioClient(name));
                running.put(name, true);
            } else {
                running.put(name, false);
            }
        }

        return new StdioMcpState(manager, defs, running, "running");
    }
}
