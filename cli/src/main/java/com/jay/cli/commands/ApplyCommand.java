package com.jay.cli.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/** Apply a patch file or stdin to the working tree. Delegates to TUI. */
@Command(name = "apply", description = "Apply a patch file or stdin to the working tree")
public class ApplyCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("Apply: (patch application)");
        System.out.println("(TUI delegation: apply command forwards to TUI subprocess)");
        return 0;
    }
}
