package com.jay.cli.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

/** Run interactive/non-interactive agent session. Delegates to TUI. */
@Command(name = "run", description = "Run interactive/non-interactive agent session")
public class RunCommand implements Callable<Integer> {

    @Parameters(arity = "0..*", paramLabel = "ARGS", description = "Forwarded args")
    List<String> args;

    @Override
    public Integer call() {
        var promptText = args != null ? String.join(" ", args) : "";
        System.out.println("Run: starting agent session" +
                (promptText.isEmpty() ? "" : " with prompt: " + promptText));
        System.out.println("(TUI delegation: use 'jay exec <prompt>' for non-interactive or 'jay app-server --http' for API)");
        return 0;
    }
}
