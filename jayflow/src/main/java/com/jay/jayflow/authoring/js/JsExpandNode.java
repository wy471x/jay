package com.jay.jayflow.authoring.js;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Equivalent to Rust's JsExpandNode — wraps JsExpandSpec under "expand" key. */
public class JsExpandNode {
    @JsonProperty("expand") public JsExpandSpec expand;
}
