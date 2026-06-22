package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/** Bootstrap MCP config and/or skills directories. Delegates to TUI. */
@Command(name = "setup", description = "Bootstrap MCP config and/or skills directories")
public class SetupCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(SetupCommand.class.getName());

    @Override
    public Integer call() {
        LOGGER.info("Bootstrapping MCP config and skills...");
        LOGGER.info("(TUI delegation: setup command forwards to TUI subprocess)");
        return 0;
    }
}
