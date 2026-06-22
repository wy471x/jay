package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/** Resume a saved session. Delegates to TUI. */
@Command(name = "resume", description = "Resume a saved session")
public class ResumeCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(ResumeCommand.class.getName());

    @Override
    public Integer call() {
        LOGGER.info("Resume: use 'jay thread resume <ID>' to resume a specific thread.");
        return 0;
    }
}
