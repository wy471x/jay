package com.jay.tools;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Identifies where a tool call originated from. Equivalent to Rust's ToolCallSource.
 */
public enum ToolCallSource {
    @JsonProperty("direct") DIRECT,
    @JsonProperty("js_repl") JS_REPL
}
