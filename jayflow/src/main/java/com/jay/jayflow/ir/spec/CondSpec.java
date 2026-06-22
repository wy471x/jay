package com.jay.jayflow.ir.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jay.jayflow.ir.WorkflowNode;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/** Equivalent to Rust's CondSpec. */
@JsonInclude(NON_NULL)
public record CondSpec(String id, String condition,
        @JsonProperty("then_nodes") List<WorkflowNode> thenNodes,
        @JsonProperty("else_nodes") List<WorkflowNode> elseNodes) { }
