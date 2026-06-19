package com.jay.jayflow.ir.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jay.jayflow.ir.WorkflowNode;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/** Equivalent to Rust's LoopUntilSpec. */
@JsonInclude(NON_NULL)
public record LoopUntilSpec(String id, String condition,
        @JsonProperty("max_iterations") Integer maxIterations,
        List<WorkflowNode> children) {}
