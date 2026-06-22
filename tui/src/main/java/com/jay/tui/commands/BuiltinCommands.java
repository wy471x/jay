package com.jay.tui.commands;

/**
 * Registers all built-in slash commands into the {@link CommandRegistry}.
 * Called once during TUI startup.
 */
public class BuiltinCommands {

    private final CommandRegistry registry;

    public BuiltinCommands(CommandRegistry registry) {
        this.registry = registry;
    }

    /** Register all built-in commands. */
    public void registerAll() {
        registry.register("help", "Show this help message", "/help",
                args -> {
                    var sb = new StringBuilder("Available commands:\n");
                    for (var cmd : registry.list()) {
                        sb.append(String.format("  /%-12s — %s\n",
                                cmd.name(), cmd.description()));
                    }
                    return new CommandResult.Success(sb.toString());
                });

        registry.register("exit", "Exit the TUI", "/exit",
                args -> new CommandResult.Exit());

        registry.register("quit", "Quit the TUI (alias for exit)", "/quit",
                args -> new CommandResult.Exit());

        registry.register("clear", "Clear the transcript view", "/clear",
                args -> new CommandResult.Success("Transcript cleared"));

        registry.register("model", "Show or switch the current model", "/model [name]",
                args -> {
                    if (args.isEmpty()) {
                        return new CommandResult.Success("Current model: deepseek-v4-flash");
                    }
                    return new CommandResult.Success("Model switched to: " + args.get(0));
                });

        registry.register("provider", "Show or switch the current provider", "/provider [name]",
                args -> {
                    if (args.isEmpty()) {
                        return new CommandResult.Success("Current provider: deepseek");
                    }
                    return new CommandResult.Success("Provider switched to: " + args.get(0));
                });

        registry.register("config", "View or set a config value", "/config [key] [value]",
                args -> {
                    if (args.isEmpty()) {
                        return new CommandResult.Success("Config: use /config <key> [value]");
                    }
                    return new CommandResult.Success(
                            "Config '" + args.get(0) + "' = " + (args.size() > 1 ? args.get(1) : "<not set>"));
                });

        registry.register("threads", "List all threads", "/threads",
                args -> new CommandResult.Success("Thread list requested"));

        registry.register("new", "Create a new thread", "/new [name]",
                args -> new CommandResult.Success("New thread created"));

        registry.register("resume", "Resume a thread by ID", "/resume <id>",
                args -> args.isEmpty()
                        ? new CommandResult.Error("Usage: /resume <thread-id>")
                        : new CommandResult.Success("Resumed thread: " + args.get(0)));

        registry.register("sidebar", "Toggle the sidebar", "/sidebar",
                args -> new CommandResult.Success("Sidebar toggled"));

        registry.register("version", "Show version info", "/version",
                args -> new CommandResult.Success("Jay TUI v0.1.0 — Java Edition"));
    }
}
