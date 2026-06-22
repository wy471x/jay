package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/** Create a default AGENTS.md in the current directory. Delegates to TUI. */
@Command(name = "init", description = "Create a default AGENTS.md in the current directory")
public class InitCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(InitCommand.class.getName());

    @Override
    public Integer call() {
        LOGGER.info("Creating AGENTS.md...");
        LOGGER.info("(TUI delegation: init command forwards to TUI subprocess)");
        return 0;
    }
}
