package com.jay.tools;

/**
 * Errors that can occur during tool execution. Equivalent to Rust's ToolError.
 * Extends RuntimeException so errors propagate like Rust's Result::Err.
 */
public sealed interface ToolError
        permits ToolError.InvalidInput, ToolError.MissingField, ToolError.PathEscape,
               ToolError.ExecutionFailed, ToolError.Timeout, ToolError.NotAvailable,
               ToolError.PermissionDenied {

    String message();

    record InvalidInput(String message) implements ToolError { }

    record MissingField(String field) implements ToolError {
        public String message() { return "missing required field '" + field + "'"; }
    }

    record PathEscape(String path) implements ToolError {
        public String message() { return "path escapes workspace: " + path; }
    }

    record ExecutionFailed(String message) implements ToolError { }

    record Timeout(long seconds) implements ToolError {
        public String message() { return "operation timed out after " + seconds + "s"; }
    }

    record NotAvailable(String message) implements ToolError { }

    record PermissionDenied(String message) implements ToolError { }

    // ---- factory methods ----

    static InvalidInput invalidInput(String msg) { return new InvalidInput(msg); }

    static MissingField missingField(String field) { return new MissingField(field); }

    static ExecutionFailed executionFailed(String msg) { return new ExecutionFailed(msg); }

    static PathEscape pathEscape(String path) { return new PathEscape(path); }

    static NotAvailable notAvailable(String msg) { return new NotAvailable(msg); }

    static PermissionDenied permissionDenied(String msg) { return new PermissionDenied(msg); }

    // ---- unchecked throw support ----

    /** Throw this error as an unchecked exception. */
    default void throwUnchecked() {
        throw new ToolErrorException(this);
    }

    /** Wrapper that makes ToolError throwable. */
    final class ToolErrorException extends RuntimeException {
        private final ToolError error;

        ToolErrorException(ToolError error) {
            super(error.message());
            this.error = error;
        }

        public ToolError error() { return error; }
    }
}
