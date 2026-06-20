package com.jay.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.jay.mcp.descriptor.McpResourceDescriptor;
import com.jay.mcp.descriptor.McpToolDescriptor;
import java.util.*;

public class InMemoryMcpClient implements McpManagedClient {

    private final String serverName;
    private final Map<String, JsonNode> toolResults = new LinkedHashMap<>();
    private final Map<String, JsonNode> resources = new LinkedHashMap<>();
    private final Map<String, String> toolDescriptions = new LinkedHashMap<>();
    private final Map<String, String> resourceDescriptions = new LinkedHashMap<>();

    public InMemoryMcpClient(String serverName) {
        this.serverName = serverName;
    }

    public InMemoryMcpClient withTool(String name, JsonNode sampleResult) {
        return withTool(name, null, sampleResult);
    }

    public InMemoryMcpClient withTool(String name, String description, JsonNode sampleResult) {
        toolResults.put(name, sampleResult);
        if (description != null) toolDescriptions.put(name, description);
        return this;
    }

    public InMemoryMcpClient withResource(String uri, JsonNode data) {
        return withResource(uri, null, data);
    }

    public InMemoryMcpClient withResource(String uri, String description, JsonNode data) {
        resources.put(uri, data);
        if (description != null) resourceDescriptions.put(uri, description);
        return this;
    }

    @Override
    public List<McpToolDescriptor> listTools() {
        List<McpToolDescriptor> result = new ArrayList<>();
        for (var entry : toolResults.entrySet()) {
            result.add(new McpToolDescriptor(
                serverName,
                entry.getKey(),
                entry.getKey(),
                toolDescriptions.get(entry.getKey())
            ));
        }
        return result;
    }

    @Override
    public JsonNode callTool(String toolName, JsonNode arguments) {
        JsonNode result = toolResults.get(toolName);
        if (result == null) {
            throw new NoSuchElementException("tool '" + toolName + "' not found");
        }
        return result;
    }

    @Override
    public List<McpResourceDescriptor> listResources() {
        List<McpResourceDescriptor> result = new ArrayList<>();
        for (var entry : resources.entrySet()) {
            result.add(new McpResourceDescriptor(
                serverName,
                entry.getKey(),
                resourceDescriptions.get(entry.getKey())
            ));
        }
        return result;
    }

    @Override
    public JsonNode readResource(String uri) {
        JsonNode result = resources.get(uri);
        if (result == null) {
            throw new NoSuchElementException("resource '" + uri + "' not found");
        }
        return result;
    }
}
