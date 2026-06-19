package com.jay.jayflow.authoring.js;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jay.jayflow.ir.spec.LoopUntilSpec;
import java.util.List;

/** Equivalent to Rust's JsLoopUntilSpec. */
public class JsLoopUntilSpec {
    public String id;
    public String condition;
    @JsonProperty("max_iterations") public Integer maxIterations;
    @JsonProperty("children") public List<JsWorkflowNode> children = List.of();

    public LoopUntilSpec toLoopUntil() {
        return new LoopUntilSpec(id, condition, maxIterations,
                children.stream().map(JsWorkflowNode::toNode).toList());
    }
}
