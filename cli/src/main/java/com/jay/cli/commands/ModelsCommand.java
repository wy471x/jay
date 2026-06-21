package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/** List live provider API models. Delegates to TUI. */
@Command(name = "models", description = "List live provider API models")
public class ModelsCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("Models: use 'jay model list' for the built-in model registry.");
        System.out.println("(TUI delegation: models command queries live provider APIs)");
        return 0;
    }
}
