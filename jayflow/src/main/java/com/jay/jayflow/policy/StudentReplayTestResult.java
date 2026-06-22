package com.jay.jayflow.policy;

/** Individual test result within a student replay. Equivalent to Rust's StudentReplayTestResult. */
public record StudentReplayTestResult(String name, boolean passed) { }
