package com.jay.jayflow.authoring.js;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Equivalent to Rust's JsSequenceNode — wraps a JsSequenceSpec under "sequence" key. */
public class JsSequenceNode {
    @JsonProperty("sequence") public JsSequenceSpec sequence;
}
