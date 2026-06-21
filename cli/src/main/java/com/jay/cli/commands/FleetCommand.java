package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/** Manage durable Agent Fleet runs. Delegates to TUI. */
@Command(name = "fleet", description = "Manage durable Agent Fleet runs")
public class FleetCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("Fleet management: (jay-jayflow module)");
        System.out.println("Use 'jay fleet' with the TUI for full fleet management capabilities.");
        return 0;
    }
}
