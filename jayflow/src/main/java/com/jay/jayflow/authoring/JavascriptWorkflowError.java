package com.jay.jayflow.authoring;

/** Compilation errors for JS/TS workflow sources. Equivalent to Rust's JavascriptWorkflowError. */
public sealed interface JavascriptWorkflowError
        permits JavascriptWorkflowError.UnsupportedConstruct, JavascriptWorkflowError.MissingWorkflowCall,
               JavascriptWorkflowError.InvalidWorkflowObject, JavascriptWorkflowError.InvalidJson,
               JavascriptWorkflowError.InvalidNode {

    record UnsupportedConstruct(String construct) implements JavascriptWorkflowError { }
    record MissingWorkflowCall() implements JavascriptWorkflowError { }
    record InvalidWorkflowObject(String reason) implements JavascriptWorkflowError { }
    record InvalidJson(String reason) implements JavascriptWorkflowError { }
    record InvalidNode(String reason) implements JavascriptWorkflowError { }
}
