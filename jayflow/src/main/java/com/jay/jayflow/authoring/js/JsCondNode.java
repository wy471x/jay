package com.jay.jayflow.authoring.js;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Equivalent to Rust's JsCondNode — wraps JsCondSpec under "cond" key. */
public class JsCondNode {
    @JsonProperty("cond") public JsCondSpec cond;
}
