package com.jay.cli.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

/** Run a code review over a git diff. Delegates to TUI. */
@Command(name = "review", description = "Run a code review over a git diff")
public class ReviewCommand implements Callable<Integer> {

    @Parameters(arity = "0..*", paramLabel = "ARGS", description = "Forwarded args")
    List<String> args;

    @Override
    public Integer call() {
        System.out.println("Code review: analyzing git diff...");
        System.out.println("(TUI delegation: review command forwards to TUI subprocess)");
        return 0;
    }
}
