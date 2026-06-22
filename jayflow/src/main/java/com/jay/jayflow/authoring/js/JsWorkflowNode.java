package com.jay.jayflow.authoring.js;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jay.jayflow.ir.WorkflowNode;
import java.io.IOException;

/**
 * Intermediate untagged enum for JS workflow nodes.
 * Uses custom deserializer matching Rust's #[serde(untagged)] behavior:
 * tries each variant in order, dispatches by presence of key fields.
 * Equivalent to Rust's JsWorkflowNode.
 */
@JsonDeserialize(using = JsWorkflowNode.Deserializer.class)
public class JsWorkflowNode {

    public enum Kind { RAW, AGENT, BRANCH, SEQUENCE, REDUCE, TEACHER_REVIEW, LOOP_UNTIL, COND, EXPAND }

    public Kind kind;
    public WorkflowNode raw;
    public JsAgentNode agent;
    public JsBranchNode branch;
    public JsSequenceNode sequence;
    public JsReduceNode reduce;
    public JsTeacherReviewNode teacherReview;
    public JsLoopUntilNode loopUntil;
    public JsCondNode cond;
    public JsExpandNode expand;

    public WorkflowNode toNode() {
        return switch (kind) {
            case RAW -> raw;
            case AGENT -> new WorkflowNode.Leaf(agent.agent);
            case BRANCH -> new WorkflowNode.BranchSet(branch.branch.toBranch());
            case SEQUENCE -> new WorkflowNode.Sequence(sequence.sequence.toSequence());
            case REDUCE -> new WorkflowNode.Reduce(reduce.reduce);
            case TEACHER_REVIEW -> new WorkflowNode.TeacherReview(teacherReview.teacherReview);
            case LOOP_UNTIL -> new WorkflowNode.LoopUntil(loopUntil.loopUntil.toLoopUntil());
            case COND -> new WorkflowNode.Cond(cond.cond.toCond());
            case EXPAND -> new WorkflowNode.Expand(expand.expand.toExpand());
        };
    }

    /** Custom deserializer: tries deserializing as each variant. */
    public static class Deserializer extends JsonDeserializer<JsWorkflowNode> {
        private static final ObjectMapper MAPPER = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        @Override
        public JsWorkflowNode deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            var node = (JsonNode) p.readValueAsTree();
            var result = new JsWorkflowNode();

            // Try Raw first (has "kind" field)
            if (node.has("kind")) {
                result.kind = Kind.RAW;
                result.raw = MAPPER.treeToValue(node, WorkflowNode.class);
                return result;
            }
            // Dispatch by wrapper key name
            if (node.has("agent"))             { result.kind = Kind.AGENT; result.agent = MAPPER.treeToValue(node, JsAgentNode.class); }
            else if (node.has("branch"))       { result.kind = Kind.BRANCH; result.branch = MAPPER.treeToValue(node, JsBranchNode.class); }
            else if (node.has("sequence"))     { result.kind = Kind.SEQUENCE; result.sequence = MAPPER.treeToValue(node, JsSequenceNode.class); }
            else if (node.has("reduce"))       { result.kind = Kind.REDUCE; result.reduce = MAPPER.treeToValue(node, JsReduceNode.class); }
            else if (node.has("teacher_review")) { result.kind = Kind.TEACHER_REVIEW; result.teacherReview = MAPPER.treeToValue(node, JsTeacherReviewNode.class); }
            else if (node.has("loop_until"))   { result.kind = Kind.LOOP_UNTIL; result.loopUntil = MAPPER.treeToValue(node, JsLoopUntilNode.class); }
            else if (node.has("cond"))         { result.kind = Kind.COND; result.cond = MAPPER.treeToValue(node, JsCondNode.class); }
            else if (node.has("expand"))       { result.kind = Kind.EXPAND; result.expand = MAPPER.treeToValue(node, JsExpandNode.class); }
            else throw new IOException("unknown JS workflow node type: " + node);
            return result;
        }
    }
}
