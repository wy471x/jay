package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/** Generate SWE-bench prediction rows. Delegates to TUI. */
@Command(name = "swebench", description = "Generate SWE-bench prediction rows")
public class SwebenchCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(SwebenchCommand.class.getName());

    @Override
    public Integer call() {
        LOGGER.info("SWE-bench: (evaluation harness)");
        LOGGER.info("(TUI delegation: swebench command forwards to TUI subprocess)");
        return 0;
    }
}
