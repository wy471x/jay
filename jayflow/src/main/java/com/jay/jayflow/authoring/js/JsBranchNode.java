package com.jay.jayflow.authoring.js;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Equivalent to Rust's JsBranchNode — wraps a JsBranchSpec under "branch" key. */
public class JsBranchNode {
    @JsonProperty("branch") public JsBranchSpec branch;
}
