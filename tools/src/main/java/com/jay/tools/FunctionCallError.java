package com.jay.tools;

/**
 * Errors at the dispatch layer. Equivalent to Rust's FunctionCallError.
 * Wraps in a RuntimeException for unchecked propagation matching Rust's Result.
 */
public sealed interface FunctionCallError
        permits FunctionCallError.ToolNotFound, FunctionCallError.KindMismatch,
               FunctionCallError.MutatingToolRejected, FunctionCallError.TimedOut,
               FunctionCallError.Cancelled, FunctionCallError.ExecutionFailed {

    record ToolNotFound(String name) implements FunctionCallError {}
    record KindMismatch(String expected, String got) implements FunctionCallError {}
    record MutatingToolRejected(String name) implements FunctionCallError {}
    record TimedOut(String name, long timeoutMs) implements FunctionCallError {}
    record Cancelled(String name) implements FunctionCallError {}
    record ExecutionFailed(String name, String error) implements FunctionCallError {}

    default void throwUnchecked() {
        throw new FceException(this);
    }

    final class FceException extends RuntimeException {
        private final FunctionCallError error;
        FceException(FunctionCallError error) { super(error.toString()); this.error = error; }
        public FunctionCallError error() { return error; }
    }
}
