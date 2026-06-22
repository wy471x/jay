package com.jay.cli.commands;

import com.jay.agent.ModelRegistry;
import com.jay.config.util.ConfigPathResolver;
import com.jay.secrets.SecretsStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/** Run system diagnostics and configuration audit. */
@picocli.CommandLine.Command(name = "doctor", description = "Run system diagnostics and configuration audit")
public class DoctorCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(DoctorCommand.class.getName());

    @Override
    public Integer call() {
        LOGGER.info("Jay Diagnostics");
        LOGGER.info("=".repeat(50));

        // Java runtime
        LOGGER.info("");
        LOGGER.info("--- Runtime ---");
        System.out.printf("  Java version:    %s%n", System.getProperty("java.version"));
        System.out.printf("  Java vendor:     %s%n", System.getProperty("java.vendor", "unknown"));
        System.out.printf("  OS:              %s %s (%s)%n",
                System.getProperty("os.name"), System.getProperty("os.version"),
                System.getProperty("os.arch"));
        System.out.printf("  Virtual threads: %s%n", Runtime.version().feature() >= 21 ? "supported" : "unsupported");
        System.out.printf("  Available CPUs:  %d%n", Runtime.getRuntime().availableProcessors());
        System.out.printf("  Max memory:      %d MB%n", Runtime.getRuntime().maxMemory() / (1024 * 1024));

        // Config
        LOGGER.info("");
        LOGGER.info("--- Configuration ---");
        Path configPath = ConfigPathResolver.resolveConfigPath(null);
        System.out.printf("  Config file:     %s%n", configPath);
        System.out.printf("  Config exists:   %s%n", Files.exists(configPath) ? "yes" : "no");
        Path permsPath = ConfigPathResolver.resolvePermissionsPath(configPath);
        System.out.printf("  Permissions:     %s (%s)%n", permsPath,
                Files.exists(permsPath) ? "exists" : "not found");
        System.out.printf("  App dir:         %s%n", ConfigPathResolver.appDir());

        // Secrets
        LOGGER.info("");
        LOGGER.info("--- Credentials ---");
        SecretsStore secrets = SecretsStore.autoDetect();
        System.out.printf("  Backend:         %s%n", secrets.backendName());
        int configured = 0;
        for (var p : com.jay.agent.ProviderKind.values()) {
            if (secrets.resolveWithSource(p.id()).isPresent()) configured++;
        }
        System.out.printf("  Providers:       %d configured / %d total%n",
                configured, com.jay.agent.ProviderKind.values().length);

        // Models
        LOGGER.info("");
        LOGGER.info("--- Models ---");
        var registry = ModelRegistry.defaultRegistry();
        System.out.printf("  Models registered: %d%n", registry.list().size());

        // Version
        LOGGER.info("");
        LOGGER.info("--- Version ---");
        System.out.printf("  jay version:     %s%n", System.getProperty("jay.version", "0.1.0"));

        return 0;
    }
}
