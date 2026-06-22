package com.jay.jayflow.ir.spec;

import com.jay.jayflow.ir.WorkflowNode;
import java.util.List;

/** Equivalent to Rust's SequenceSpec. */
public record SequenceSpec(String id, List<WorkflowNode> children) { }
