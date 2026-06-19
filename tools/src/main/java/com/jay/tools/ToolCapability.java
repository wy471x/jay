package com.jay.tools;

/** Capabilities that a tool may have or require. Equivalent to Rust's ToolCapability. */
public enum ToolCapability {
    READ_ONLY,
    WRITES_FILES,
    EXECUTES_CODE,
    NETWORK,
    SANDBOXABLE,
    REQUIRES_APPROVAL
}
