package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/** Manage MCP servers. Delegates to TUI. */
@Command(name = "mcp", description = "Manage MCP servers")
public class McpCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(McpCommand.class.getName());

    @Override
    public Integer call() {
        LOGGER.info("MCP management: use 'jay mcp-server' for stdio mode.");
        LOGGER.info("(TUI delegation: mcp command provides full MCP server management)");
        return 0;
    }
}
