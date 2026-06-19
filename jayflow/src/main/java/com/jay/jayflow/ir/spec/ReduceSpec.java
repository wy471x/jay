package com.jay.jayflow.ir.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jay.jayflow.ir.WorkflowConfig;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/** Equivalent to Rust's ReduceSpec. */
@JsonInclude(NON_NULL)
public record ReduceSpec(String id, List<String> inputs, String prompt,
        WorkflowConfig.ModelPolicy modelPolicy) {}
