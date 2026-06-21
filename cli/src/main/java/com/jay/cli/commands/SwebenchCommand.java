package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/** Generate SWE-bench prediction rows. Delegates to TUI. */
@Command(name = "swebench", description = "Generate SWE-bench prediction rows")
public class SwebenchCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("SWE-bench: (evaluation harness)");
        System.out.println("(TUI delegation: swebench command forwards to TUI subprocess)");
        return 0;
    }
}
