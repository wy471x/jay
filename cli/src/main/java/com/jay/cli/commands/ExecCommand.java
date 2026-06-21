package com.jay.cli.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;

/** Run a non-interactive prompt through the agent runtime. Delegates to TUI. */
@Command(name = "exec", description = "Run a non-interactive prompt through the agent runtime")
public class ExecCommand implements Callable<Integer> {

    @Option(names = {"--auto"}, description = "Enable tool-backed agent mode with auto-approvals")
    boolean autoMode;

    @Option(names = {"--json"}, description = "Emit summary JSON")
    boolean json;

    @Option(names = {"--resume"}, description = "Resume previous session by ID or prefix")
    String resume;

    @Option(names = {"--session-id"}, description = "Resume previous session by ID")
    String sessionId;

    @Option(names = {"--continue"}, description = "Continue most recent session")
    boolean continueSession;

    @Option(names = {"--output-format"}, description = "Output format: text or stream-json")
    String outputFormat;

    @Parameters(arity = "0..*", paramLabel = "PROMPT", description = "Prompt text")
    List<String> prompt;

    @Override
    public Integer call() {
        var promptText = prompt != null ? String.join(" ", prompt) : "";
        var mode = autoMode ? "agent" : "oneshot";
        System.out.printf("Exec [%s]: %s%n", mode, promptText);
        System.out.println("(TUI delegation: exec command forwards to TUI subprocess)");
        return 0;
    }
}
