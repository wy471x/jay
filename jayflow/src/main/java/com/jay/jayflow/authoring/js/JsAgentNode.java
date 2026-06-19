package com.jay.jayflow.authoring.js;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jay.jayflow.ir.spec.LeafSpec;

/** Equivalent to Rust's JsAgentNode — wraps a LeafSpec under "agent" key. */
public class JsAgentNode {
    @JsonProperty("agent") public LeafSpec agent;
}
