package com.jay.cli.commands;

import com.jay.secrets.SecretsStore;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/** Remove saved authentication state. */
@Command(name = "logout", description = "Remove saved authentication state")
public class LogoutCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(LogoutCommand.class.getName());

    @Override
    public Integer call() {
        try {
            SecretsStore store = SecretsStore.autoDetect();
            for (var p : com.jay.agent.ProviderKind.values()) {
                try {
                    store.delete(p.id());
                } catch (Exception ignored) { }
            }
            LOGGER.info("Logged out — credentials cleared.");
            return 0;
        } catch (Exception e) {
            LOGGER.severe("Failed to clear credentials: " + e.getMessage());
            return 1;
        }
    }
}
