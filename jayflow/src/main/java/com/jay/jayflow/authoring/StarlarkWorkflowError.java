package com.jay.jayflow.authoring;

/** Compilation errors for Starlark workflow sources. Equivalent to Rust's StarlarkWorkflowError. */
public sealed interface StarlarkWorkflowError
        permits StarlarkWorkflowError.UnsupportedConstruct, StarlarkWorkflowError.MissingWorkflow,
               StarlarkWorkflowError.InvalidNode, StarlarkWorkflowError.InvalidEnum,
               StarlarkWorkflowError.Starlark {

    record UnsupportedConstruct(String construct) implements StarlarkWorkflowError {}
    record MissingWorkflow() implements StarlarkWorkflowError {}
    record InvalidNode(String reason) implements StarlarkWorkflowError {}
    record InvalidEnum(String field, String value) implements StarlarkWorkflowError {}
    record Starlark(String reason) implements StarlarkWorkflowError {}
}
