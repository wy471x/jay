package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/** List saved sessions. Delegates to TUI. */
@Command(name = "sessions", description = "List saved sessions")
public class SessionsCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(SessionsCommand.class.getName());

    @Override
    public Integer call() {
        LOGGER.info("Saved sessions: use 'jay thread list' for thread management.");
        return 0;
    }
}
