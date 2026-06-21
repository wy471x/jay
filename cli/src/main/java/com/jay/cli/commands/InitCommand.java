package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/** Create a default AGENTS.md in the current directory. Delegates to TUI. */
@Command(name = "init", description = "Create a default AGENTS.md in the current directory")
public class InitCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("Creating AGENTS.md...");
        System.out.println("(TUI delegation: init command forwards to TUI subprocess)");
        return 0;
    }
}
