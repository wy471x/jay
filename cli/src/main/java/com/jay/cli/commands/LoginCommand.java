package com.jay.cli.commands;

import com.jay.agent.ProviderKind;
import com.jay.cli.converter.ProviderKindConverter;
import com.jay.secrets.SecretsStore;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.Console;
import java.util.concurrent.Callable;

/** Configure provider credentials interactively or via flags. */
@Command(name = "login", description = "Configure provider credentials")
public class LoginCommand implements Callable<Integer> {

    @Option(names = {"--provider"}, description = "Provider to log into",
            converter = ProviderKindConverter.class)
    ProviderKind provider;

    @Option(names = {"--api-key"}, description = "API key (prompts if omitted)")
    String apiKey;

    @Override
    public Integer call() {
        var p = provider != null ? provider : ProviderKind.DEEPSEEK;
        String key = apiKey;
        if (key == null) {
            Console console = System.console();
            if (console != null) {
                key = new String(console.readPassword("API key for " + p.id() + ": "));
            } else {
                System.err.println("No console available. Use --api-key to provide the key.");
                return 1;
            }
        }
        if (key == null || key.isBlank()) {
            System.err.println("No API key provided.");
            return 1;
        }
        try {
            SecretsStore store = SecretsStore.autoDetect();
            store.set(p.id(), key);
            System.out.println("Logged in to " + p.id());
            return 0;
        } catch (Exception e) {
            System.err.println("Failed to save credentials: " + e.getMessage());
            return 1;
        }
    }
}
