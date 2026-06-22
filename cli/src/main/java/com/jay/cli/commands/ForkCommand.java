package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/** Fork a saved session. Delegates to TUI. */
@Command(name = "fork", description = "Fork a saved session")
public class ForkCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(ForkCommand.class.getName());

    @Override
    public Integer call() {
        LOGGER.info("Fork: use 'jay thread fork <ID>' to fork a thread.");
        return 0;
    }
}
