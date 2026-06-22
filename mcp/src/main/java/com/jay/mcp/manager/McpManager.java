package com.jay.mcp.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jay.mcp.client.McpManagedClient;
import com.jay.mcp.config.McpServerConfig;
import com.jay.mcp.config.ToolFilter;
import com.jay.mcp.descriptor.McpResourceDescriptor;
import com.jay.mcp.descriptor.McpToolDescriptor;
import com.jay.mcp.lifecycle.McpStartupCompleteEvent;
import com.jay.mcp.lifecycle.McpStartupFailure;
import com.jay.mcp.lifecycle.McpStartupStatus;
import com.jay.mcp.lifecycle.McpStartupUpdateEvent;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class McpManager {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    final Map<String, McpServerConfig> configs = new LinkedHashMap<>();
    private final Map<String, ToolFilter> filters = new LinkedHashMap<>();
    final Map<String, McpManagedClient> clients = new LinkedHashMap<>();

    public void registerServer(McpServerConfig config, ToolFilter filter, McpManagedClient client) {
        configs.put(config.name(), config);
        filters.put(config.name(), filter);
        clients.put(config.name(), client);
    }

    public McpStartupCompleteEvent startAll(Consumer<McpStartupUpdateEvent> emit) {
        List<String> ready = new ArrayList<>();
        List<McpStartupFailure> failed = new ArrayList<>();
        List<String> cancelled = new ArrayList<>();

        for (var entry : configs.entrySet()) {
            String name = entry.getKey();
            McpServerConfig cfg = entry.getValue();

            if (!cfg.enabled()) {
                cancelled.add(name);
                emit.accept(new McpStartupUpdateEvent(name, new McpStartupStatus.Cancelled()));
                continue;
            }

            emit.accept(new McpStartupUpdateEvent(name, new McpStartupStatus.Starting()));

            if (clients.containsKey(name)) {
                emit.accept(new McpStartupUpdateEvent(name, new McpStartupStatus.Ready()));
                ready.add(name);
            } else {
                String error = "client not registered";
                emit.accept(new McpStartupUpdateEvent(name, new McpStartupStatus.Failed(error)));
                failed.add(new McpStartupFailure(name, error));
            }
        }
        return new McpStartupCompleteEvent(ready, failed, cancelled);
    }

    public void stopServer(String serverName) {
        if (!clients.containsKey(serverName)) {
            throw new NoSuchElementException("server '" + serverName + "' is not running");
        }
        clients.remove(serverName);
    }

    public void unregisterServer(String serverName) {
        if (!configs.containsKey(serverName)) {
            throw new NoSuchElementException("server '" + serverName + "' is not registered");
        }
        configs.remove(serverName);
        filters.remove(serverName);
        clients.remove(serverName);
    }

    public List<McpToolDescriptor> listTools() {
        List<McpToolDescriptor> out = new ArrayList<>();
        for (var entry : clients.entrySet()) {
            String serverName = entry.getKey();
            ToolFilter filter = filters.get(serverName);
            for (McpToolDescriptor tool : entry.getValue().listTools()) {
                if (!allowedByFilter(tool.toolName(), filter)) continue;
                String qualifiedName = ToolNameQualifier.qualify(serverName, tool.toolName());
                out.add(new McpToolDescriptor(
                    serverName, tool.toolName(), qualifiedName, tool.description()));
            }
        }
        return out;
    }

    public JsonNode callTool(String serverName, String toolName, JsonNode arguments) {
        McpManagedClient client = clients.get(serverName);
        if (client == null) {
            throw new NoSuchElementException("MCP server '" + serverName + "' not available");
        }
        return client.callTool(toolName, arguments);
    }

    public JsonNode callQualifiedTool(String qualifiedToolName, JsonNode arguments) {
        // First try: parse the qualified name and do a direct lookup
        String[] parsed = ToolNameQualifier.parse(qualifiedToolName);
        if (parsed != null && clients.containsKey(parsed[0])) {
            try {
                return clients.get(parsed[0]).callTool(parsed[1], arguments);
            } catch (NoSuchElementException e) {
                // fall through to scan
            }
        }

        // Second try: scan all servers for the matching qualified name
        for (var entry : clients.entrySet()) {
            String serverName = entry.getKey();
            for (McpToolDescriptor tool : entry.getValue().listTools()) {
                String qn = ToolNameQualifier.qualify(serverName, tool.toolName());
                if (qn.equals(qualifiedToolName)) {
                    return entry.getValue().callTool(tool.toolName(), arguments);
                }
            }
        }

        if (parsed != null) {
            return callTool(parsed[0], parsed[1], arguments);
        }

        throw new NoSuchElementException("tool not found: " + qualifiedToolName);
    }

    public List<McpResourceDescriptor> listResources() {
        List<McpResourceDescriptor> out = new ArrayList<>();
        for (var entry : clients.entrySet()) {
            String serverName = entry.getKey();
            for (McpResourceDescriptor res : entry.getValue().listResources()) {
                out.add(new McpResourceDescriptor(serverName, res.uri(), res.description()));
            }
        }
        return out;
    }

    public JsonNode readResource(String serverName, String uri) {
        McpManagedClient client = clients.get(serverName);
        if (client == null) {
            throw new NoSuchElementException("MCP server '" + serverName + "' not available");
        }
        return client.readResource(uri);
    }

    public List<JsonNode> updateSandboxState(String sandboxMode, String cwd) {
        List<JsonNode> notices = new ArrayList<>();
        for (String serverName : configs.keySet()) {
            ObjectNode notice = MAPPER.createObjectNode();
            notice.put("server_name", serverName);
            notice.put("method", "codex/sandbox-state/update");
            ObjectNode params = MAPPER.createObjectNode();
            params.put("sandbox_mode", sandboxMode);
            params.put("cwd", cwd);
            notice.set("params", params);
            notices.add(notice);
        }
        return notices;
    }

    public static boolean allowedByFilter(String toolName, ToolFilter filter) {
        if (filter == null) return true;
        if (filter.deny().contains(toolName)) return false;
        if (filter.allow().isEmpty()) return true;
        return filter.allow().contains(toolName);
    }
}
