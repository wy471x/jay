package com.jay.cli.commands;

import com.jay.config.store.ConfigStore;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Read/write/list config values.
 *
 * <p>Subcommands: get, set, unset, list, path
 */
@Command(name = "config", description = "Read/write/list config values",
    subcommands = {
        ConfigCommand.Get.class, ConfigCommand.Set.class,
        ConfigCommand.Unset.class, ConfigCommand.ListKeys.class,
        ConfigCommand.Path.class
    })
public class ConfigCommand implements Callable<Integer> {
    private static final Logger LOGGER = Logger.getLogger(ConfigCommand.class.getName());

    @Override
    public Integer call() {
        LOGGER.info("Usage: jay config <get|set|unset|list|path>");
        return 0;
    }

    private static ConfigStore loadStore() throws IOException {
        java.nio.file.Path configPath = null;
        String envPath = System.getenv("CODEWHALE_CONFIG_PATH");
        if (envPath == null) envPath = System.getenv("DEEPSEEK_CONFIG_PATH");
        if (envPath != null && !envPath.isBlank()) {
            configPath = java.nio.file.Path.of(envPath);
        }
        return ConfigStore.load(configPath);
    }

    // ── get ───────────────────────────────────────────────────────────

    @Command(name = "get", description = "Get a config value by key")
    static class Get implements Callable<Integer> {
        @Parameters(index = "0", description = "Config key (e.g. default-model)")
        String key;

        @Override
        public Integer call() {
            try {
                ConfigStore store = loadStore();
                String value = store.getValue(key);
                if (value != null) {
                    LOGGER.info(key + " = " + value);
                } else {
                    LOGGER.info(key + " = <not set>");
                }
                return 0;
            } catch (IOException e) {
                LOGGER.severe("Error reading config: " + e.getMessage());
                return 1;
            }
        }
    }

    // ── set ───────────────────────────────────────────────────────────

    @Command(name = "set", description = "Set a config key=value")
    static class Set implements Callable<Integer> {
        @Parameters(index = "0", description = "Config key")
        String key;

        @Parameters(index = "1", description = "Config value")
        String value;

        @Override
        public Integer call() {
            try {
                ConfigStore store = loadStore();
                store.setValue(key, value);
                store.save();
                LOGGER.info(key + " = " + value);
                return 0;
            } catch (IOException e) {
                LOGGER.severe("Error writing config: " + e.getMessage());
                return 1;
            }
        }
    }

    // ── unset ─────────────────────────────────────────────────────────

    @Command(name = "unset", description = "Remove a config key")
    static class Unset implements Callable<Integer> {
        @Parameters(index = "0", description = "Config key to remove")
        String key;

        @Override
        public Integer call() {
            try {
                ConfigStore store = loadStore();
                store.unsetValue(key);
                store.save();
                LOGGER.info("Unset " + key);
                return 0;
            } catch (IOException e) {
                LOGGER.severe("Error writing config: " + e.getMessage());
                return 1;
            }
        }
    }

    // ── list ──────────────────────────────────────────────────────────

    @Command(name = "list", description = "List all config values")
    static class ListKeys implements Callable<Integer> {
        @Override
        public Integer call() {
            try {
                ConfigStore store = loadStore();
                var values = store.listValues();
                if (values.isEmpty()) {
                    LOGGER.info("No config values set.");
                } else {
                    for (var entry : values.entrySet()) {
                        LOGGER.info(entry.getKey() + " = " + entry.getValue());
                    }
                }
                return 0;
            } catch (IOException e) {
                LOGGER.severe("Error reading config: " + e.getMessage());
                return 1;
            }
        }
    }

    // ── path ──────────────────────────────────────────────────────────

    @Command(name = "path", description = "Print the config file path")
    static class Path implements Callable<Integer> {
        @Override
        public Integer call() {
            try {
                ConfigStore store = loadStore();
                LOGGER.info(store.path().toString());
                return 0;
            } catch (IOException e) {
                LOGGER.severe("Error resolving config path: " + e.getMessage());
                return 1;
            }
        }
    }
}
