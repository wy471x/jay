package com.jay.cli.commands;

import com.jay.cli.CliSpringContext;
import com.jay.execpolicy.ExecPolicyContext;
import com.jay.execpolicy.ExecPolicyEngine;
import com.jay.protocol.approval.AskForApproval;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/** Evaluate sandbox/approval policy decisions. */
@Command(name = "sandbox", description = "Evaluate sandbox/approval policy decisions")
public class SandboxCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(SandboxCommand.class.getName());

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
            LOGGER.severe("No command provided. Use: jay sandbox --check 'rm -rf /'");
            return 1;
        }

        AskForApproval askMode = switch (ask.toLowerCase()) {
            case "never" -> new AskForApproval.Never();
            case "unless-trusted" -> new AskForApproval.UnlessTrusted();
            case "on-failure" -> new AskForApproval.OnFailure();
            case "on-request" -> new AskForApproval.OnRequest();
            default -> {
                LOGGER.severe("Unknown approval mode: " + ask);
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

        LOGGER.info("Command:       " + command);
        LOGGER.info("Approval mode: " + ask);
        LOGGER.info("Allowed:       " + decision.allow());
        LOGGER.info("Needs approval:" + decision.requiresApproval());
        if (decision.requirement() != null) {
            LOGGER.info("Requirement:   " + decision.requirement());
        }
        if (decision.matchedRule() != null) {
            LOGGER.info("Matched rule:  " + decision.matchedRule());
        }
        return decision.allow() ? 0 : 1;
    }
}
