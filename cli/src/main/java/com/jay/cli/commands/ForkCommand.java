package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/** Fork a saved session. Delegates to TUI. */
@Command(name = "fork", description = "Fork a saved session")
public class ForkCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("Fork: use 'jay thread fork <ID>' to fork a thread.");
        return 0;
    }
}
