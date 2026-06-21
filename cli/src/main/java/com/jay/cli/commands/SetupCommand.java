package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/** Bootstrap MCP config and/or skills directories. Delegates to TUI. */
@Command(name = "setup", description = "Bootstrap MCP config and/or skills directories")
public class SetupCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("Bootstrapping MCP config and skills...");
        System.out.println("(TUI delegation: setup command forwards to TUI subprocess)");
        return 0;
    }
}
