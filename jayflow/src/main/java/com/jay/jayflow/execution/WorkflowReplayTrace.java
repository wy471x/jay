package com.jay.jayflow.execution;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import com.jay.jayflow.ir.ControlNodeKind;
import com.jay.jayflow.ir.WorkflowNode;

/** Replay trace for workflow execution. Equivalent to Rust's WorkflowReplayTrace. */
public record WorkflowReplayTrace(
        @JsonProperty("trace_id") String traceId,
        @JsonProperty("leaf_records") List<ReplayLeafRecord> leafRecords,
        @JsonProperty("control_records") List<ReplayControlRecord> controlRecords) {}

/** Leaf replay record. Equivalent to Rust's ReplayLeafRecord. */
record ReplayLeafRecord(
        @JsonProperty("trace_id") String traceId,
        @JsonProperty("leaf_id") String leafId,
        @JsonProperty("input_hash") String inputHash,
        LeafResult result) {}

/** Control node replay record. Equivalent to Rust's ReplayControlRecord. */
record ReplayControlRecord(
        @JsonProperty("trace_id") String traceId,
        @JsonProperty("node_id") String nodeId,
        ControlNodeKind kind,
        ControlNodeResult result,
        @JsonProperty("generated_nodes") List<WorkflowNode> generatedNodes) {}
