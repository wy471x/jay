package com.jay.cli.commands;

import com.jay.agent.ModelRegistry;
import com.jay.agent.ProviderKind;
import com.jay.cli.converter.ProviderKindConverter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/** Resolve or list available models across providers. */
@Command(name = "model", description = "Resolve or list available models across providers")
public class ModelCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(ModelCommand.class.getName());

    @Option(names = {"list"}, description = "List all registered models")
    boolean list;

    @Option(names = {"--provider"}, description = "Filter by provider",
            converter = ProviderKindConverter.class)
    ProviderKind provider;

    @Parameters(arity = "0..1", paramLabel = "MODEL", description = "Model name to resolve")
    String model;

    @Override
    public Integer call() {
        var registry = ModelRegistry.defaultRegistry();
        if (list) {
            var models = provider != null
                    ? registry.list().stream().filter(m -> m.provider() == provider).toList()
                    : registry.list();
            if (models.isEmpty()) {
                LOGGER.info("No models found" + (provider != null ? " for " + provider.id() : "") + ".");
                return 0;
            }
            System.out.printf("%-50s %-15s %s%n", "MODEL ID", "PROVIDER", "CAPABILITIES");
            LOGGER.info("-".repeat(85));
            for (var m : models) {
                var caps = new StringBuilder();
                if (m.supportsTools()) caps.append("tools ");
                if (m.supportsReasoning()) caps.append("reasoning");
                System.out.printf("%-50s %-15s %s%n", m.id(), m.provider().id(),
                        caps.toString().trim());
            }
            LOGGER.info("-".repeat(85));
            LOGGER.info(models.size() + " model(s)");
        } else {
            var resolved = registry.resolve(model, provider);
            LOGGER.info("Model Resolution");
            LOGGER.info("  Requested:  " + resolved.requested());
            LOGGER.info("  Resolved:   " + resolved.resolved().id()
                    + " (" + resolved.resolved().provider().id() + ")");
            LOGGER.info("  Fallback:   " + resolved.usedFallback());
            LOGGER.info("  Chain:      " + String.join(" → ", resolved.fallbackChain()));
        }
        return 0;
    }
}
