package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/** Run the offline evaluation harness. Delegates to TUI. */
@Command(name = "eval", description = "Run the offline evaluation harness")
public class EvalCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("Eval: (offline evaluation harness)");
        System.out.println("(TUI delegation: eval command forwards to TUI subprocess)");
        return 0;
    }
}
