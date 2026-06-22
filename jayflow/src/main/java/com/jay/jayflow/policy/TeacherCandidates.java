package com.jay.jayflow.policy;

import java.util.ArrayList;
import java.util.List;
import com.jay.jayflow.execution.BranchResult;
import com.jay.jayflow.execution.ControlNodeResult;
import com.jay.jayflow.execution.LeafResult;
import com.jay.jayflow.execution.WorkflowExecution;
import com.jay.jayflow.ir.WorkflowNode;
import com.jay.jayflow.ir.WorkflowRunStatus;

/**
 * Extracts TeacherCandidate entries from a workflow execution.
 * Ported from Rust's teacher_candidates_from_execution + helper functions.
 */
public final class TeacherCandidates {
    private TeacherCandidates() { }

    public static List<TeacherCandidate> fromExecution(WorkflowNode.TeacherReview review,
                                                        WorkflowExecution execution) {
        var candidates = new ArrayList<TeacherCandidate>();
        for (var source : review.candidates()) {
            // Check branches
            for (var branch : execution.branchResults()) {
                if (branch.branchId().equals(source) || branch.taskId().equals(source)) {
                    candidates.add(fromBranch(review, branch));
                }
            }
            // Check leaves
            for (var leaf : execution.leafResults()) {
                if (leaf.leafId().equals(source) || leaf.taskId().equals(source)) {
                    candidates.add(fromLeaf(review, leaf));
                }
            }
            // Check control nodes
            for (var ctrl : execution.controlNodeResults()) {
                if (ctrl.nodeId().equals(source)) {
                    candidates.add(fromControl(review, ctrl));
                }
            }
        }
        return candidates;
    }

    private static TeacherCandidate fromBranch(WorkflowNode.TeacherReview review, BranchResult branch) {
        var kind = (branch.memoUsage().armhHits() > 0 || branch.memoUsage().providerPromptCacheHits() > 0)
                ? TeacherCandidateKind.CACHE_POLICY_PATCH
                : branch.status() == WorkflowRunStatus.SUCCEEDED
                ? TeacherCandidateKind.WORKFLOW_RECIPE
                : TeacherCandidateKind.BRANCH_HEURISTIC;

        var evidence = new ArrayList<String>();
        evidence.add("status=" + branch.status().name().toLowerCase());
        if (branch.usage().totalTokens() > 0 || branch.usage().costMicrousd() > 0)
            evidence.add("tokens=" + branch.usage().totalTokens() + ", cost_microusd=" + branch.usage().costMicrousd());
        if (branch.memoUsage().armhHits() > 0 || branch.memoUsage().providerPromptCacheHits() > 0)
            evidence.add("armh_hits=" + branch.memoUsage().armhHits()
                    + ", provider_prompt_cache_hits=" + branch.memoUsage().providerPromptCacheHits());
        if (branch.notes() != null) evidence.add("notes=" + branch.notes());

        return new TeacherCandidate(review.id() + ":" + branch.branchId(), kind,
                TeacherCandidateStatus.PROPOSED, branch.taskId(), branch.branchId(),
                "TeacherReview candidate from branch `" + branch.branchId() + "' with " + branch.status().name().toLowerCase() + " status.",
                evidence, List.of());
    }

    private static TeacherCandidate fromLeaf(WorkflowNode.TeacherReview review, LeafResult leaf) {
        var kind = leaf.status() == WorkflowRunStatus.FAILED
                ? TeacherCandidateKind.REGRESSION_TEST
                : leaf.memoUsage().armhHits() > 0 || leaf.memoUsage().providerPromptCacheHits() > 0
                ? TeacherCandidateKind.CACHE_POLICY_PATCH
                : TeacherCandidateKind.NOTE;

        var evidence = new ArrayList<String>();
        evidence.add("status=" + leaf.status().name().toLowerCase());
        if (leaf.output() != null) evidence.add("output=" + truncate(leaf.output()));

        return new TeacherCandidate(review.id() + ":" + leaf.leafId(), kind,
                TeacherCandidateStatus.PROPOSED, leaf.leafId(), null,
                "TeacherReview candidate from leaf `" + leaf.leafId() + "' with " + leaf.status().name().toLowerCase() + " status.",
                evidence, List.of());
    }

    private static TeacherCandidate fromControl(WorkflowNode.TeacherReview review, ControlNodeResult ctrl) {
        var evidence = new ArrayList<String>();
        evidence.add("status=" + ctrl.status().name().toLowerCase());
        if (!ctrl.selectedChildren().isEmpty())
            evidence.add("selected_children=" + String.join(",", ctrl.selectedChildren()));
        if (ctrl.summary() != null) evidence.add("summary=" + truncate(ctrl.summary()));

        return new TeacherCandidate(review.id() + ":" + ctrl.nodeId(), TeacherCandidateKind.STARLARK_AUTHORING_PROMPT_PATCH,
                TeacherCandidateStatus.PROPOSED, ctrl.nodeId(), null,
                "TeacherReview candidate from control node `" + ctrl.nodeId() + "'.",
                evidence, List.of());
    }

    private static String truncate(String value) {
        return value.length() <= 240 ? value : value.substring(0, 239) + "...";
    }
}
