package com.jay.jayflow.validation;

/** Node validation errors thrown during WorkflowSpec validation. */
public sealed interface WorkflowExecutionError
        permits WorkflowExecutionError.EmptyNodeId, WorkflowExecutionError.EmptyLeafPrompt,
               WorkflowExecutionError.DuplicateNodeId, WorkflowExecutionError.UnknownNodeReference {

    record EmptyNodeId(String kind) implements WorkflowExecutionError { }
    record EmptyLeafPrompt(String leaf) implements WorkflowExecutionError { }
    record DuplicateNodeId(String node) implements WorkflowExecutionError { }
    record UnknownNodeReference(String node, String field, String reference) implements WorkflowExecutionError { }

    /** Throw this error as an unchecked exception, for use in throw positions. */
    default void panic() { throw new RuntimeException(this.toString()); }

    /** Create and throw. */
    static void raise(WorkflowExecutionError err) { throw new RuntimeException(err.toString()); }
}
