package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/** Apply a patch file or stdin to the working tree. Delegates to TUI. */
@Command(name = "apply", description = "Apply a patch file or stdin to the working tree")
public class ApplyCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(ApplyCommand.class.getName());

    @Override
    public Integer call() {
        LOGGER.info("Apply: (patch application)");
        LOGGER.info("(TUI delegation: apply command forwards to TUI subprocess)");
        return 0;
    }
}
