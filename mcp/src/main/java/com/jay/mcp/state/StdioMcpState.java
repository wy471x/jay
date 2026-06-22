package com.jay.mcp.state;

import com.jay.mcp.config.McpServerDefinition;
import com.jay.mcp.manager.McpManager;

import java.util.LinkedHashMap;
import java.util.Map;

public class StdioMcpState {

    private final McpManager manager;
    private final Map<String, McpServerDefinition> definitions;
    private final Map<String, Boolean> running;
    private String lifecycleState;

    public StdioMcpState(McpManager manager, Map<String, McpServerDefinition> definitions,
                         Map<String, Boolean> running, String lifecycleState) {
        this.manager = manager;
        this.definitions = new LinkedHashMap<>(definitions);
        this.running = new LinkedHashMap<>(running);
        this.lifecycleState = lifecycleState;
    }

    public McpManager manager() { return manager; }

    public Map<String, McpServerDefinition> definitions() { return definitions; }

    public Map<String, Boolean> running() { return running; }

    public String lifecycleState() { return lifecycleState; }

    public void setLifecycleState(String state) { this.lifecycleState = state; }
}
