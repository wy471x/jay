package com.jay.cli.commands;

import com.jay.cli.CliSpringContext;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/** Run MCP server mode over stdio. */
@Command(name = "mcp-server", description = "Run MCP server mode over stdio")
public class McpServerCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        try {
            var mcpManager = CliSpringContext.getBeanOrNull(
                    com.jay.mcp.manager.McpManager.class);
            if (mcpManager == null) {
                System.err.println("MCP manager not available. Ensure Spring context is initialized.");
                return 1;
            }
            System.out.println("MCP server starting on stdio...");
            var server = new com.jay.mcp.rpc.StdioMcpServer();
            server.run(java.util.List.of());
            return 0;
        } catch (Exception e) {
            System.err.println("MCP server error: " + e.getMessage());
            return 1;
        }
    }
}
