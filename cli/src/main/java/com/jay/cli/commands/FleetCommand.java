package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/** Manage durable Agent Fleet runs. Delegates to TUI. */
@Command(name = "fleet", description = "Manage durable Agent Fleet runs")
public class FleetCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(FleetCommand.class.getName());

    @Override
    public Integer call() {
        LOGGER.info("Fleet management: (jay-jayflow module)");
        LOGGER.info("Use 'jay fleet' with the TUI for full fleet management capabilities.");
        return 0;
    }
}
