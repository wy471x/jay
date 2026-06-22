package com.jay.tui.commands;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of slash commands. Commands are registered by name and executed
 * via a functional interface.
 */
public class CommandRegistry {

    @FunctionalInterface
    public interface CommandHandler {
        CommandResult execute(List<String> args);
    }

    private final Map<String, Entry> commands = new LinkedHashMap<>();

    private record Entry(String name, String description, String usage,
                         CommandHandler handler) {}

    /** Register a new command. */
    public void register(String name, String description, String usage,
                         CommandHandler handler) {
        commands.put(name, new Entry(name, description, usage, handler));
    }

    /** Execute a command by name with the given arguments. */
    public CommandResult execute(String name, List<String> args) {
        var entry = commands.get(name);
        if (entry == null) {
            return new CommandResult.Error("Unknown command: /" + name);
        }
        try {
            return entry.handler().execute(args);
        } catch (Exception e) {
            return new CommandResult.Error("Command failed: " + e.getMessage());
        }
    }

    /** List all registered commands (sorted by name). */
    public List<CommandInfo> list() {
        return commands.values().stream()
                .map(e -> new CommandInfo(e.name(), e.description(), e.usage()))
                .sorted((a, b) -> a.name().compareTo(b.name()))
                .toList();
    }

    /** Find a command by name. Returns null if not found. */
    public CommandInfo find(String name) {
        var entry = commands.get(name);
        return entry != null
                ? new CommandInfo(entry.name(), entry.description(), entry.usage())
                : null;
    }

    public record CommandInfo(String name, String description, String usage) {}
}
