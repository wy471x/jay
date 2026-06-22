package com.jay.jayflow.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.jay.jayflow.ir.WorkflowNode;

/**
 * Validates WorkflowNode trees for shape and reference integrity.
 * Equivalent to Rust's validate_workflow_nodes / validate_workflow_node_shapes.
 */
public final class WorkflowNodeValidator {
    private WorkflowNodeValidator() { }

    public static void validate(List<WorkflowNode> nodes) {
        var seen = new HashSet<String>();
        validateInner(nodes, seen);
        validateReferences(nodes, seen);
    }

    public static void validateShapes(List<WorkflowNode> nodes) {
        validateInner(nodes, new HashSet<>());
    }

    private static void validateInner(List<WorkflowNode> nodes, Set<String> seen) {
        for (var node : nodes) {
            var id = nodeId(node);
            var kind = kindName(node);
            if (id.trim().isEmpty()) new WorkflowExecutionError.EmptyNodeId(kind).panic();
            if (!seen.add(id)) new WorkflowExecutionError.DuplicateNodeId(id).panic();

            switch (node) {
                case WorkflowNode.BranchSet spec -> validateInner(spec.children(), seen);
                case WorkflowNode.Leaf spec -> {
                    if (spec.prompt().trim().isEmpty())
                        new WorkflowExecutionError.EmptyLeafPrompt(spec.id()).panic();
                }
                case WorkflowNode.Sequence spec -> validateInner(spec.children(), seen);
                case WorkflowNode.LoopUntil spec -> validateInner(spec.children(), seen);
                case WorkflowNode.Cond spec -> {
                    validateInner(spec.thenNodes(), seen);
                    validateInner(spec.elseNodes(), seen);
                }
                default -> { }
            }
        }
    }

    private static void validateReferences(List<WorkflowNode> nodes, Set<String> known) {
        for (var node : nodes) {
            switch (node) {
                case WorkflowNode.BranchSet spec -> validateReferences(spec.children(), known);
                case WorkflowNode.Leaf spec ->
                    checkRefs(spec.id(), "depends_on_results", spec.dependsOnResults(), known);
                case WorkflowNode.Sequence spec -> validateReferences(spec.children(), known);
                case WorkflowNode.Reduce spec ->
                    checkRefs(spec.id(), "inputs", spec.inputs(), known);
                case WorkflowNode.TeacherReview spec ->
                    checkRefs(spec.id(), "candidates", spec.candidates(), known);
                case WorkflowNode.LoopUntil spec -> validateReferences(spec.children(), known);
                case WorkflowNode.Cond spec -> {
                    validateReferences(spec.thenNodes(), known);
                    validateReferences(spec.elseNodes(), known);
                }
                default -> { }
            }
        }
    }

    private static void checkRefs(String node, String field, List<String> refs, Set<String> known) {
        for (var ref : refs != null ? refs : List.<String>of())
            if (!known.contains(ref))
                new WorkflowExecutionError.UnknownNodeReference(node, field, ref).panic();
    }

    public static String nodeId(WorkflowNode node) {
        return switch (node) {
            case WorkflowNode.BranchSet s -> s.id();
            case WorkflowNode.Leaf s -> s.id();
            case WorkflowNode.Sequence s -> s.id();
            case WorkflowNode.Reduce s -> s.id();
            case WorkflowNode.TeacherReview s -> s.id();
            case WorkflowNode.LoopUntil s -> s.id();
            case WorkflowNode.Cond s -> s.id();
            case WorkflowNode.Expand s -> s.id();
        };
    }

    static String kindName(WorkflowNode node) {
        return switch (node) {
            case WorkflowNode.BranchSet s -> "branch_set";
            case WorkflowNode.Leaf s -> "leaf";
            case WorkflowNode.Sequence s -> "sequence";
            case WorkflowNode.Reduce s -> "reduce";
            case WorkflowNode.TeacherReview s -> "teacher_review";
            case WorkflowNode.LoopUntil s -> "loop_until";
            case WorkflowNode.Cond s -> "cond";
            case WorkflowNode.Expand s -> "expand";
        };
    }
}
