package com.jay.jayflow.execution;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.jay.jayflow.ir.WorkflowNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Deterministic replay executor. Equivalent to Rust's WorkflowReplayExecutor. */
public class WorkflowReplayExecutor {

    private static final ObjectMapper SORTED = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

    private final String traceId;
    private final boolean allowLiveReplay;
    private final Map<ReplayLeafKey, LeafResult> leafRecords = new HashMap<>();
    private final Map<ReplayControlKey, ReplayControlRecord> controlRecords = new HashMap<>();
    private final Map<String, String> resolvedOutputs = new HashMap<>();

    public WorkflowReplayExecutor(WorkflowReplayTrace trace) { this(trace, false); }
    public WorkflowReplayExecutor(WorkflowReplayTrace trace, boolean allowLiveReplay) {
        this.traceId = trace.traceId(); this.allowLiveReplay = allowLiveReplay;
        for (var r : trace.leafRecords()) leafRecords.put(new ReplayLeafKey(r.traceId(), r.leafId(), r.inputHash()), r.result());
        for (var r : trace.controlRecords()) controlRecords.put(new ReplayControlKey(r.traceId(), r.nodeId()), r);
    }

    public static String computeLeafInputHash(String workflowId, String goal, WorkflowNode.Leaf leaf, Map<String, String> resolved) {
        try {
            var input = new TreeMap<String, Object>();
            input.put("workflow_id", workflowId); input.put("workflow_goal", goal);
            input.put("leaf_id", leaf.id()); input.put("leaf_prompt", leaf.prompt());
            input.put("leaf_agent_type", leaf.agentType().name());
            input.put("resolved_inputs", new TreeMap<>(resolved));
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(SORTED.writeValueAsBytes(input));
            var sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new WorkflowReplayError("failed to compute leaf hash: " + e.getMessage());
        }
    }

    static class WorkflowReplayError extends RuntimeException {
        WorkflowReplayError(String msg) { super(msg); }
    }
}

record ReplayLeafKey(String traceId, String leafId, String inputHash) {}
record ReplayControlKey(String traceId, String nodeId) {}
