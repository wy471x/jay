package com.jay.jayflow.authoring.js;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jay.jayflow.ir.spec.SequenceSpec;
import java.util.List;

/** Equivalent to Rust's JsSequenceSpec. */
public class JsSequenceSpec {
    public String id;
    @JsonProperty("children") public List<JsWorkflowNode> children = List.of();

    public SequenceSpec toSequence() {
        return new SequenceSpec(id, children.stream().map(JsWorkflowNode::toNode).toList());
    }
}
