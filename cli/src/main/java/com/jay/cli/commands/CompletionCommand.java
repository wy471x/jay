package com.jay.cli.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Callable;

/** Shell completion generation. */
@Command(name = "completion", description = "Generate shell completions")
public class CompletionCommand implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "SHELL",
            description = "Target shell: bash, zsh, fish")
    String shell;

    @Option(names = {"-o", "--output"}, description = "Output file (defaults to stdout)")
    File output;

    @Override
    public Integer call() {
        String name = "jay";
        picocli.CommandLine cmd = new picocli.CommandLine(new com.jay.cli.JayCli());
        try {
            switch (shell.toLowerCase()) {
                case "bash" -> {
                    if (output != null) {
                        File parentCmd = new File(output.getParentFile(), name);
                        picocli.AutoComplete.bash(name, output.getParentFile(), parentCmd, cmd);
                        System.out.println("Bash completion written to " + output);
                    } else {
                        String script = picocli.AutoComplete.bash(name, cmd);
                        System.out.print(script);
                    }
                }
                case "zsh", "fish" -> {
                    // picocli 4.x does not have dedicated zsh/fish methods.
                    // Generate bash script first, then convert or instruct user.
                    String bashScript = picocli.AutoComplete.bash(name, cmd);
                    if (output != null) {
                        Files.writeString(output.toPath(), "# " + shell + " completion for jay\n");
                        Files.writeString(output.toPath(), bashScript,
                                java.nio.file.StandardOpenOption.APPEND);
                        System.out.println(shell + " completion written to " + output);
                        System.out.println("Note: For native " + shell + " completions, use the bash script as a base.");
                    } else {
                        System.out.println("# " + shell + " completion for jay (bash-based)");
                        System.out.println(bashScript);
                    }
                }
                default -> {
                    System.err.println("Unknown shell: " + shell);
                    System.err.println("Supported shells: bash, zsh, fish");
                    return 1;
                }
            }
            return 0;
        } catch (IOException e) {
            System.err.println("Error generating completion: " + e.getMessage());
            return 1;
        }
    }
}
