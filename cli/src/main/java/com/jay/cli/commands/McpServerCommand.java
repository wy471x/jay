package com.jay.cli.commands;

import com.jay.cli.CliSpringContext;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/** Run MCP server mode over stdio. */
@Command(name = "mcp-server", description = "Run MCP server mode over stdio")
public class McpServerCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(McpServerCommand.class.getName());

    @Override
    public Integer call() {
        try {
            var mcpManager = CliSpringContext.getBeanOrNull(
                    com.jay.mcp.manager.McpManager.class);
            if (mcpManager == null) {
                LOGGER.severe("MCP manager not available. Ensure Spring context is initialized.");
                return 1;
            }
            LOGGER.info("MCP server starting on stdio...");
            var server = new com.jay.mcp.rpc.StdioMcpServer();
            server.run(java.util.List.of());
            return 0;
        } catch (Exception e) {
            LOGGER.severe("MCP server error: " + e.getMessage());
            return 1;
        }
    }
}
