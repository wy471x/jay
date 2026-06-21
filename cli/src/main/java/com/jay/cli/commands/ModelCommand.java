package com.jay.cli.commands;

import com.jay.agent.ModelRegistry;
import com.jay.agent.ProviderKind;
import com.jay.cli.converter.ProviderKindConverter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/** Resolve or list available models across providers. */
@Command(name = "model", description = "Resolve or list available models across providers")
public class ModelCommand implements Callable<Integer> {

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
                System.out.println("No models found" + (provider != null ? " for " + provider.id() : "") + ".");
                return 0;
            }
            System.out.printf("%-50s %-15s %s%n", "MODEL ID", "PROVIDER", "CAPABILITIES");
            System.out.println("-".repeat(85));
            for (var m : models) {
                var caps = new StringBuilder();
                if (m.supportsTools()) caps.append("tools ");
                if (m.supportsReasoning()) caps.append("reasoning");
                System.out.printf("%-50s %-15s %s%n", m.id(), m.provider().id(),
                        caps.toString().trim());
            }
            System.out.println("-".repeat(85));
            System.out.println(models.size() + " model(s)");
        } else {
            var resolved = registry.resolve(model, provider);
            System.out.println("Model Resolution");
            System.out.println("  Requested:  " + resolved.requested());
            System.out.println("  Resolved:   " + resolved.resolved().id()
                    + " (" + resolved.resolved().provider().id() + ")");
            System.out.println("  Fallback:   " + resolved.usedFallback());
            System.out.println("  Chain:      " + String.join(" → ", resolved.fallbackChain()));
        }
        return 0;
    }
}
