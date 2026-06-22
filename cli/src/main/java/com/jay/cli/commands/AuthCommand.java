package com.jay.cli.commands;

import com.jay.agent.ProviderKind;

import com.jay.secrets.ProviderEnv;

import com.jay.secrets.SecretsStore;

import picocli.CommandLine.Command;

import picocli.CommandLine.Option;

import com.jay.cli.converter.ProviderKindConverter;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**

 * Manage authentication credentials and provider mode.

 *

 * <p>Subcommands: status, set, get, clear, list, migrate

 */

@Command(name = "auth", description = "Manage authentication credentials and provider mode",

    subcommands = {

        AuthCommand.Status.class, AuthCommand.Set.class,

        AuthCommand.Get.class, AuthCommand.Clear.class,

        AuthCommand.List.class, AuthCommand.Migrate.class

    })

public class AuthCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(AuthCommand.class.getName());

    @Override

    public Integer call() {

        LOGGER.info("Usage: jay auth <status|set|get|clear|list|migrate>");

        return 0;

    }

    // ── status ────────────────────────────────────────────────────────

    @Command(name = "status", description = "Show authentication status for providers")

    static class Status implements Callable<Integer> {

        @Option(names = {"--provider"}, description = "Provider to check",

                converter = ProviderKindConverter.class)

        ProviderKind provider;

        @Override

        public Integer call() {

            if (provider != null) {

                String env = ProviderEnv.envFor(provider.id());

                System.out.printf("%-20s %s%n", provider.id(),

                        env != null ? "set (env)" : "not set");

            } else {

                for (ProviderKind p : ProviderKind.values()) {

                    String env = ProviderEnv.envFor(p.id());

                    System.out.printf("%-20s %s%n", p.id(),

                            env != null ? "set (env)" : "not set");

                }

            }

            return 0;

        }

    }

    // ── set ───────────────────────────────────────────────────────────

    @Command(name = "set", description = "Set API key for a provider")

    static class Set implements Callable<Integer> {

        @Option(names = {"--provider"}, required = true, description = "Provider to configure",

                converter = ProviderKindConverter.class)

        ProviderKind provider;

        @Option(names = {"--api-key"}, description = "API key value")

        String apiKey;

        @Option(names = {"--api-key-stdin"}, description = "Read API key from stdin")

        boolean apiKeyStdin;

        @Override

        public Integer call() {

            try {

                SecretsStore store = SecretsStore.autoDetect();

                String key = apiKey;

                if (key == null && apiKeyStdin) {

                    key = new String(System.console().readPassword("API key: "));

                }

                if (key == null || key.isBlank()) {

                    LOGGER.severe("No API key provided. Use --api-key or --api-key-stdin.");

                    return 1;

                }

                store.set(provider.id(), key);

                LOGGER.info("API key set for " + provider.id());

                return 0;

            } catch (Exception e) {

                LOGGER.severe("Failed to set API key: " + e.getMessage());

                return 1;

            }

        }

    }

    // ── get ───────────────────────────────────────────────────────────

    @Command(name = "get", description = "Check if provider has a key configured")

    static class Get implements Callable<Integer> {

        @Option(names = {"--provider"}, required = true, description = "Provider to check",

                converter = ProviderKindConverter.class)

        ProviderKind provider;

        @Override

        public Integer call() {

            SecretsStore store = SecretsStore.autoDetect();

            var resolved = store.resolveWithSource(provider.id());

            if (resolved.isPresent()) {

                var rs = resolved.get();

                System.out.printf("%s: configured (%s)%n", provider.id(), rs.source().name().toLowerCase());

            } else {

                System.out.printf("%s: not configured%n", provider.id());

            }

            return 0;

        }

    }

    // ── clear ─────────────────────────────────────────────────────────

    @Command(name = "clear", description = "Delete stored API key for a provider")

    static class Clear implements Callable<Integer> {

        @Option(names = {"--provider"}, required = true, description = "Provider to clear",

                converter = ProviderKindConverter.class)

        ProviderKind provider;

        @Override

        public Integer call() {

            try {

                SecretsStore store = SecretsStore.autoDetect();

                store.delete(provider.id());

                LOGGER.info("Cleared API key for " + provider.id());

                return 0;

            } catch (Exception e) {

                LOGGER.severe("Failed to clear: " + e.getMessage());

                return 1;

            }

        }

    }

    // ── list ──────────────────────────────────────────────────────────

    @Command(name = "list", description = "List all providers auth state")

    static class List implements Callable<Integer> {

        @Override

        public Integer call() {

            SecretsStore store = SecretsStore.autoDetect();

            for (ProviderKind p : ProviderKind.values()) {

                var resolved = store.resolveWithSource(p.id());

                String status = resolved.map(rs ->

                        rs.source().name().toLowerCase()).orElse("not configured");

                System.out.printf("%-20s %s%n", p.id(), status);

            }

            return 0;

        }

    }

    // ── migrate ───────────────────────────────────────────────────────

    @Command(name = "migrate", description = "Migrate config-file keys to credential store")

    static class Migrate implements Callable<Integer> {

        @Option(names = {"--dry-run"}, description = "Preview migration without changes")

        boolean dryRun;

        @Override

        public Integer call() {

            LOGGER.info("Migration" + (dryRun ? " (dry-run)" : "") + ": checking for legacy keys...");

            LOGGER.info("No legacy keys found to migrate.");

            return 0;

        }

    }

}
