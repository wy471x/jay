package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/** List saved sessions. Delegates to TUI. */
@Command(name = "sessions", description = "List saved sessions")
public class SessionsCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("Saved sessions: use 'jay thread list' for thread management.");
        return 0;
    }
}
