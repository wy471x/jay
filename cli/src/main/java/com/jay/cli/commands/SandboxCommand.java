package com.jay.cli.commands;

import com.jay.cli.CliSpringContext;
import com.jay.execpolicy.ExecPolicyContext;
import com.jay.execpolicy.ExecPolicyEngine;
import com.jay.protocol.approval.AskForApproval;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/** Evaluate sandbox/approval policy decisions. */
@Command(name = "sandbox", description = "Evaluate sandbox/approval policy decisions")
public class SandboxCommand implements Callable<Integer> {

    @Option(names = {"--check"}, description = "Check command against policy")
    String checkCommand;

    @Option(names = {"--ask"}, description = "Approval mode: unless-trusted, on-failure, on-request, never")
    String ask = "on-request";

    @Parameters(arity = "0..1", paramLabel = "COMMAND",
            description = "Shell command to evaluate (alternative to --check)")
    String positionalCommand;

    @Override
    public Integer call() {
        String command = checkCommand != null ? checkCommand : positionalCommand;
        if (command == null || command.isBlank()) {
            System.err.println("No command provided. Use: jay sandbox --check 'rm -rf /'");
            return 1;
        }

        AskForApproval askMode = switch (ask.toLowerCase()) {
            case "never" -> new AskForApproval.Never();
            case "unless-trusted" -> new AskForApproval.UnlessTrusted();
            case "on-failure" -> new AskForApproval.OnFailure();
            case "on-request" -> new AskForApproval.OnRequest();
            default -> {
                System.err.println("Unknown approval mode: " + ask);
                yield new AskForApproval.OnRequest();
            }
        };

        ExecPolicyEngine engine = CliSpringContext.getBeanOrNull(ExecPolicyEngine.class);
        if (engine == null) {
            engine = new ExecPolicyEngine(java.util.List.of(), java.util.List.of());
        }

        var ctx = new ExecPolicyContext(command,
                System.getProperty("user.dir"), null, null, askMode, "workspace");
        var decision = engine.check(ctx);

        System.out.println("Command:       " + command);
        System.out.println("Approval mode: " + ask);
        System.out.println("Allowed:       " + decision.allow());
        System.out.println("Needs approval:" + decision.requiresApproval());
        if (decision.requirement() != null) {
            System.out.println("Requirement:   " + decision.requirement());
        }
        if (decision.matchedRule() != null) {
            System.out.println("Matched rule:  " + decision.matchedRule());
        }
        return decision.allow() ? 0 : 1;
    }
}
