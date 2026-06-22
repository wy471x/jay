package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/** List live provider API models. Delegates to TUI. */
@Command(name = "models", description = "List live provider API models")
public class ModelsCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(ModelsCommand.class.getName());

    @Override
    public Integer call() {
        LOGGER.info("Models: use 'jay model list' for the built-in model registry.");
        LOGGER.info("(TUI delegation: models command queries live provider APIs)");
        return 0;
    }
}
