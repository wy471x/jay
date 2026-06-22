package com.jay.jayflow.authoring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jay.jayflow.ir.WorkflowNode;
import com.jay.jayflow.ir.WorkflowSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates WorkflowSpec during Starlark evaluation.
 * Equivalent to Rust's WorkflowBuilder — each builtin function appends
 * nodes encoded as JSON tokens, which are decoded at the end into WorkflowNode objects.
 */
public class WorkflowBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WorkflowSpec workflow;
    private final List<String> nodeTokens = new ArrayList<>();

    public void setWorkflow(WorkflowSpec workflow) { this.workflow = workflow; }
    public WorkflowSpec getWorkflow() { return workflow; }

    /** Encode a WorkflowNode to a JSON token string. Equivalent to Rust's encode_node(). */
    public String encodeNode(WorkflowNode node) {
        try { return MAPPER.writeValueAsString(node); }
        catch (JsonProcessingException e) { throw new RuntimeException("failed to encode node", e); }
    }

    /** Add a JSON-encoded node token. Used by builtins during evaluation. */
    public void addNodeToken(String token) { nodeTokens.add(token); }

    /** Decode all accumulated node tokens into WorkflowNode list. Equivalent to Rust's decode_nodes(). */
    public List<WorkflowNode> decodeNodes() {
        return nodeTokens.stream().map(token -> {
            try { return MAPPER.readValue(token, WorkflowNode.class); }
            catch (JsonProcessingException e) { throw new RuntimeException("failed to decode node token", e); }
        }).toList();
    }

    /** Decode a single JSON token string to a WorkflowNode. Equivalent to Rust's decode_node(). */
    public WorkflowNode decodeNode(String token) {
        try { return MAPPER.readValue(token, WorkflowNode.class); }
        catch (JsonProcessingException e) { throw new RuntimeException("failed to decode node token: " + token, e); }
    }

    @Override
    public String toString() {
        return "WorkflowBuilder[nodes=" + nodeTokens.size() + ", workflow=" + (workflow != null) + "]";
    }
}
