package com.jay.jayflow.ir.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jay.jayflow.ir.WorkflowNode;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/** Equivalent to Rust's ExpandSpec. */
@JsonInclude(NON_NULL)
public record ExpandSpec(String id, String source,
        @JsonProperty("max_children") Integer maxChildren, WorkflowNode template) { }
