package com.jay.mcp;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * MCP JSON-RPC server using stdio transport.
 * Uses ProcessBuilder for subprocess management — simpler than
 * Rust's tokio::process thanks to virtual threads.
 */
public class McpServer implements AutoCloseable {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final Process process;
    private final BufferedWriter writer;
    private final BufferedReader reader;
    private final Map<String, McpTool> tools = new ConcurrentHashMap<>();

    public McpServer(String command) throws IOException {
        this.process = new ProcessBuilder(command.split(" "))
                .redirectErrorStream(true)
                .start();
        this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        this.reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    }

    public void registerTool(String name, McpTool tool) {
        tools.put(name, tool);
    }

    public JsonNode invoke(String toolName, JsonNode arguments) throws IOException {
        var tool = tools.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("Unknown MCP tool: " + toolName);
        }
        return tool.execute(arguments);
    }

    @Override
    public void close() throws IOException {
        writer.close();
        reader.close();
        process.destroy();
    }

    @FunctionalInterface
    public interface McpTool {
        JsonNode execute(JsonNode arguments);
    }
}
