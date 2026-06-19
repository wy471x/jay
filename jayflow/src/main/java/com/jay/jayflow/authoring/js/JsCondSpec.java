package com.jay.jayflow.authoring.js;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jay.jayflow.ir.spec.CondSpec;
import java.util.List;

/** Equivalent to Rust's JsCondSpec. */
public class JsCondSpec {
    public String id;
    public String condition;
    @JsonProperty("then_nodes") public List<JsWorkflowNode> thenNodes = List.of();
    @JsonProperty("else_nodes") public List<JsWorkflowNode> elseNodes = List.of();

    public CondSpec toCond() {
        return new CondSpec(id, condition,
                thenNodes.stream().map(JsWorkflowNode::toNode).toList(),
                elseNodes.stream().map(JsWorkflowNode::toNode).toList());
    }
}
