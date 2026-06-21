package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/** Resume a saved session. Delegates to TUI. */
@Command(name = "resume", description = "Resume a saved session")
public class ResumeCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("Resume: use 'jay thread resume <ID>' to resume a specific thread.");
        return 0;
    }
}
