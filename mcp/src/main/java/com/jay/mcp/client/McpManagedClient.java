package com.jay.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.jay.mcp.descriptor.McpResourceDescriptor;
import com.jay.mcp.descriptor.McpToolDescriptor;
import java.util.List;

public interface McpManagedClient {
    List<McpToolDescriptor> listTools();

    JsonNode callTool(String toolName, JsonNode arguments);

    List<McpResourceDescriptor> listResources();

    JsonNode readResource(String uri);
}
