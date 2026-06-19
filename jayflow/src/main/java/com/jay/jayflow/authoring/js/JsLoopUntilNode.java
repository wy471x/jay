package com.jay.jayflow.authoring.js;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Equivalent to Rust's JsLoopUntilNode — wraps JsLoopUntilSpec under "loop_until" key. */
public class JsLoopUntilNode {
    @JsonProperty("loop_until") public JsLoopUntilSpec loopUntil;
}
