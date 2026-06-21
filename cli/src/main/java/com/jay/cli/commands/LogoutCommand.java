package com.jay.cli.commands;

import com.jay.secrets.SecretsStore;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/** Remove saved authentication state. */
@Command(name = "logout", description = "Remove saved authentication state")
public class LogoutCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        try {
            SecretsStore store = SecretsStore.autoDetect();
            for (var p : com.jay.agent.ProviderKind.values()) {
                try {
                    store.delete(p.id());
                } catch (Exception ignored) {}
            }
            System.out.println("Logged out — credentials cleared.");
            return 0;
        } catch (Exception e) {
            System.err.println("Failed to clear credentials: " + e.getMessage());
            return 1;
        }
    }
}
