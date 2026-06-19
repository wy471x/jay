package com.jay.jayflow.authoring.js;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jay.jayflow.ir.spec.ExpandSpec;

/** Equivalent to Rust's JsExpandSpec. */
public class JsExpandSpec {
    public String id;
    public String source;
    @JsonProperty("max_children") public Integer maxChildren;
    public JsWorkflowNode template;

    public ExpandSpec toExpand() {
        return new ExpandSpec(id, source, maxChildren,
                template != null ? template.toNode() : null);
    }
}
