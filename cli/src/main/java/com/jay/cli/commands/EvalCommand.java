package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/** Run the offline evaluation harness. Delegates to TUI. */
@Command(name = "eval", description = "Run the offline evaluation harness")
public class EvalCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(EvalCommand.class.getName());

    @Override
    public Integer call() {
        LOGGER.info("Eval: (offline evaluation harness)");
        LOGGER.info("(TUI delegation: eval command forwards to TUI subprocess)");
        return 0;
    }
}
