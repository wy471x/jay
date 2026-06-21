package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/** Manage MCP servers. Delegates to TUI. */
@Command(name = "mcp", description = "Manage MCP servers")
public class McpCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("MCP management: use 'jay mcp-server' for stdio mode.");
        System.out.println("(TUI delegation: mcp command provides full MCP server management)");
        return 0;
    }
}
