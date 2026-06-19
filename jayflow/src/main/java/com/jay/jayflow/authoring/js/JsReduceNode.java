package com.jay.jayflow.authoring.js;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jay.jayflow.ir.spec.ReduceSpec;

/** Equivalent to Rust's JsReduceNode — wraps ReduceSpec directly under "reduce" key. */
public class JsReduceNode {
    @JsonProperty("reduce") public ReduceSpec reduce;
}
