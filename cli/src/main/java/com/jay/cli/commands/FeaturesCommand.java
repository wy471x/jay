package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/** Inspect feature flags. Delegates to TUI. */
@Command(name = "features", description = "Inspect feature flags")
public class FeaturesCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(FeaturesCommand.class.getName());

    @Override
    public Integer call() {
        LOGGER.info("Feature flags: (jay-config module)");
        LOGGER.info("(TUI delegation: features command forwards to TUI subprocess)");
        return 0;
    }
}
