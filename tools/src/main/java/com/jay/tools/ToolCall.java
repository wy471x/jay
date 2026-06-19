package com.jay.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jay.protocol.ToolPayload;
import com.jay.protocol.LocalShellParams;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * A tool invocation request before validation. Equivalent to Rust's ToolCall.
 */
@JsonInclude(NON_NULL)
public record ToolCall(
        String name,
        ToolPayload payload,
        ToolCallSource source,
        String rawToolCallId
) {
    /**
     * Derive the execution subject: (command, cwd, kind) tuple.
     * For LocalShell payloads returns the shell command; for others returns the tool name.
     */
    public ExecutionSubject executionSubject(String fallbackCwd) {
        if (payload instanceof ToolPayload.LocalShell localShell) {
            var params = localShell.params();
            return new ExecutionSubject(
                    params.command(),
                    params.cwd() != null ? params.cwd() : fallbackCwd,
                    "shell");
        }
        return new ExecutionSubject(name, fallbackCwd, "tool");
    }

    public record ExecutionSubject(String command, String cwd, String kind) {}
}
