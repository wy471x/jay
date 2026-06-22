package com.jay.tui.commands;

/**
 * Result of executing a slash command.
 * Equivalent to Rust's command result type.
 */
public sealed interface CommandResult {

    /** Command completed successfully. */
    record Success(String message) implements CommandResult {}

    /** Command failed with an error. */
    record Error(String message) implements CommandResult {}

    /** Command requests the TUI to exit. */
    record Exit() implements CommandResult {}
}
