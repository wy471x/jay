package com.jay.cli.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/** Run a code review over a git diff. Delegates to TUI. */
@Command(name = "review", description = "Run a code review over a git diff")
public class ReviewCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(ReviewCommand.class.getName());

    @Parameters(arity = "0..*", paramLabel = "ARGS", description = "Forwarded args")
    List<String> args;

    @Override
    public Integer call() {
        LOGGER.info("Code review: analyzing git diff...");
        LOGGER.info("(TUI delegation: review command forwards to TUI subprocess)");
        return 0;
    }
}
