package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/** Inspect feature flags. Delegates to TUI. */
@Command(name = "features", description = "Inspect feature flags")
public class FeaturesCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("Feature flags: (jay-config module)");
        System.out.println("(TUI delegation: features command forwards to TUI subprocess)");
        return 0;
    }
}
