package com.jay.jayflow.execution;

import com.jay.jayflow.ir.*;
import com.jay.jayflow.validation.WorkflowNodeValidator;
import java.util.*;

/** Mock executor for testing. Equivalent to Rust's MockWorkflowExecutor. */
public class MockWorkflowExecutor {

    private final Map<String, MockLeafOutcome> leafOutcomes = new HashMap<>();
    private final Map<String, List<Boolean>> predicates = new HashMap<>();
    private final Map<String, List<WorkflowNode>> generatedNodes = new HashMap<>();
    private boolean cancelled;
    private Integer maxLeafSteps;
    private int leafStepsExecuted;

    // Mutable execution state built during run()
    private List<LeafResult> leafResults;
    private List<BranchResult> branchResults;
    private List<ControlNodeResult> controlResults;
    private WorkflowUsage totalUsage;
    private WorkflowMemoUsage totalMemo;
    private WorkflowRunStatus currentStatus;

    public MockWorkflowExecutor withLeafOutcome(String id, MockLeafOutcome o) { leafOutcomes.put(id, o); return this; }
    public MockWorkflowExecutor withPredicateResults(String id, List<Boolean> r) { predicates.put(id, new ArrayList<>(r)); return this; }
    public MockWorkflowExecutor withGeneratedNodes(String id, List<WorkflowNode> n) { generatedNodes.put(id, n); return this; }
    public MockWorkflowExecutor withCancelled() { cancelled = true; return this; }
    public MockWorkflowExecutor withMaxLeafSteps(int m) { maxLeafSteps = m; return this; }

    public WorkflowExecution run(WorkflowSpec spec) {
        WorkflowNodeValidator.validate(spec.nodes());
        leafResults = new ArrayList<>(); branchResults = new ArrayList<>(); controlResults = new ArrayList<>();
        totalUsage = new WorkflowUsage(); totalMemo = new WorkflowMemoUsage(); currentStatus = WorkflowRunStatus.SUCCEEDED;
        executeNodes(spec.nodes());
        return new WorkflowExecution(currentStatus, totalUsage, totalMemo, leafResults, branchResults, controlResults);
    }

    private void executeNodes(List<WorkflowNode> nodes) {
        for (var n : nodes) { if (shouldStop()) break; executeNode(n); }
    }

    private void executeNode(WorkflowNode node) {
        switch (node) {
            case WorkflowNode.BranchSet s -> executeBranchSet(s);
            case WorkflowNode.Leaf s -> executeLeaf(s);
            case WorkflowNode.Sequence s -> { executeNodes(s.children()); ctrl(s.id(), ControlNodeKind.SEQUENCE, ids(s.children()), "sequence"); }
            case WorkflowNode.Reduce s -> ctrl(s.id(), ControlNodeKind.REDUCE, s.inputs(), s.prompt());
            case WorkflowNode.TeacherReview s -> ctrl(s.id(), ControlNodeKind.TEACHER_REVIEW, s.candidates(), "teacher review");
            case WorkflowNode.LoopUntil s -> executeLoopUntil(s);
            case WorkflowNode.Cond s -> executeCond(s);
            case WorkflowNode.Expand s -> executeExpand(s);
        }
    }

    private void executeBranchSet(WorkflowNode.BranchSet s) {
        int before = leafResults.size();
        executeNodes(s.children());
        var after = leafResults.subList(before, leafResults.size());
        var st = aggregateStatus(after);
        markStatus(st);
        var u = new WorkflowUsage(); var m = new WorkflowMemoUsage();
        for (var r : after) { u = u.add(r.usage()); m = m.add(r.memoUsage()); }
        totalUsage = totalUsage.add(u); totalMemo = totalMemo.add(m);
        branchResults.add(new BranchResult(s.id(), s.id(), st, u, m, List.of(), "mock branch"));
        ctrl(s.id(), ControlNodeKind.BRANCH_SET, ids(s.children()), "branch scaffold");
    }

    private void executeLeaf(WorkflowNode.Leaf s) {
        var outcome = mockLeafOutcome(s);
        markStatus(outcome.status());
        totalUsage = totalUsage.add(outcome.usage()); totalMemo = totalMemo.add(outcome.memoUsage());
        leafResults.add(new LeafResult(s.id(), s.id(), outcome.status(), outcome.usage(), outcome.memoUsage(), outcome.output(), outcome.artifacts()));
    }

    private void executeLoopUntil(WorkflowNode.LoopUntil s) {
        int max = s.maxIterations() != null ? Math.max(s.maxIterations(), 1) : 1, iters = 0;
        boolean passed = false;
        while (iters < max && !shouldStop()) { iters++; executeNodes(s.children()); if (!shouldStop() && nextPredicate(s.id())) { passed = true; break; } }
        markStatus(shouldStop() ? currentStatus : passed ? WorkflowRunStatus.SUCCEEDED : WorkflowRunStatus.FAILED);
        ctrl(s.id(), ControlNodeKind.LOOP_UNTIL, ids(s.children()), "loop_until iters=" + iters);
    }

    private void executeCond(WorkflowNode.Cond s) {
        boolean passed = nextPredicate(s.id());
        var selected = passed ? s.thenNodes() : s.elseNodes();
        executeNodes(selected);
        ctrl(s.id(), ControlNodeKind.COND, ids(selected), "predicate_result=" + passed);
    }

    private void executeExpand(WorkflowNode.Expand s) {
        var nodes = generatedNodes.getOrDefault(s.id(), List.of());
        int max = s.maxChildren() != null ? Math.min(nodes.size(), s.maxChildren()) : nodes.size();
        var trimmed = nodes.subList(0, max);
        WorkflowNodeValidator.validateShapes(trimmed);
        executeNodes(trimmed);
        ctrl(s.id(), ControlNodeKind.EXPAND, ids(trimmed), "expanded_from=" + s.source());
    }

    private MockLeafOutcome mockLeafOutcome(WorkflowNode.Leaf s) {
        if (cancelled) return MockLeafOutcome.cancelled("cancelled before leaf");
        if ((maxLeafSteps != null && leafStepsExecuted >= maxLeafSteps) ||
                (s.budget() != null && s.budget().maxSteps() != null && s.budget().maxSteps() == 0))
            return MockLeafOutcome.budgetExceeded("budget exhausted");
        leafStepsExecuted++;
        return leafOutcomes.getOrDefault(s.id(), MockLeafOutcome.succeeded("mock " + s.id()));
    }

    private boolean nextPredicate(String id) {
        var list = predicates.get(id);
        return list != null && !list.isEmpty() && list.removeFirst();
    }

    private boolean shouldStop() { return currentStatus == WorkflowRunStatus.CANCELLED || currentStatus == WorkflowRunStatus.BUDGET_EXCEEDED; }

    private void markStatus(WorkflowRunStatus s) {
        switch (s) {
            case FAILED -> currentStatus = WorkflowRunStatus.FAILED;
            case CANCELLED -> currentStatus = WorkflowRunStatus.CANCELLED;
            case BUDGET_EXCEEDED -> currentStatus = WorkflowRunStatus.BUDGET_EXCEEDED;
            default -> {}
        }
    }

    private void ctrl(String id, ControlNodeKind kind, List<String> children, String summary) {
        controlResults.add(new ControlNodeResult(id, kind, currentStatus, children, summary));
    }

    private static List<String> ids(List<WorkflowNode> nodes) { return nodes.stream().map(WorkflowNodeValidator::nodeId).toList(); }

    private static WorkflowRunStatus aggregateStatus(List<LeafResult> results) {
        if (results.stream().anyMatch(r -> r.status() == WorkflowRunStatus.CANCELLED)) return WorkflowRunStatus.CANCELLED;
        if (results.stream().anyMatch(r -> r.status() == WorkflowRunStatus.BUDGET_EXCEEDED)) return WorkflowRunStatus.BUDGET_EXCEEDED;
        if (results.stream().anyMatch(r -> r.status() != WorkflowRunStatus.SUCCEEDED)) return WorkflowRunStatus.FAILED;
        return WorkflowRunStatus.SUCCEEDED;
    }
}

record MockLeafOutcome(WorkflowRunStatus status, WorkflowUsage usage, WorkflowMemoUsage memoUsage, String output, List<String> artifacts) {
    static MockLeafOutcome succeeded(String o) { return new MockLeafOutcome(WorkflowRunStatus.SUCCEEDED, new WorkflowUsage(), new WorkflowMemoUsage(), o, List.of()); }
    static MockLeafOutcome failed(String o) { return new MockLeafOutcome(WorkflowRunStatus.FAILED, new WorkflowUsage(), new WorkflowMemoUsage(), o, List.of()); }
    static MockLeafOutcome cancelled(String o) { return new MockLeafOutcome(WorkflowRunStatus.CANCELLED, new WorkflowUsage(), new WorkflowMemoUsage(), o, List.of()); }
    static MockLeafOutcome budgetExceeded(String o) { return new MockLeafOutcome(WorkflowRunStatus.BUDGET_EXCEEDED, new WorkflowUsage(), new WorkflowMemoUsage(), o, List.of()); }
}
