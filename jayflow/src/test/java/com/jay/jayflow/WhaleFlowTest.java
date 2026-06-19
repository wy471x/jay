package com.jay.jayflow;

import com.jay.jayflow.execution.WhaleFlowEngine;
import com.jay.jayflow.ir.*;
import com.jay.jayflow.validation.WorkflowValidationError;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WhaleFlowTest {

    private final WhaleFlowEngine engine = new WhaleFlowEngine();

    private static WorkflowConfig.Task t(String id) { return new WorkflowConfig.Task(id, "run " + id); }

    // ── Configuration compilation ────────────────────────────

    @Test
    void validConfigCompilesToPlan() {
        var config = new WorkflowConfig("Test workflow", 4, null, List.of(
                new WorkflowConfig.Phase("build", false, FailurePolicy.ABORT, List.of(), List.of(t("t1")))));
        var plan = engine.compile(config);
        assertEquals("Test workflow", plan.goal());
        assertEquals(4, plan.maxConcurrent());
        assertEquals(1, plan.phases().size());
        assertEquals("build", plan.phases().get(0).name());
    }

    @Test
    void multiPhaseConfigRespectsDependencyOrder() {
        var c1 = t("c1");
        var p1 = new WorkflowConfig.Task("p1", "process data", AgentType.GENERAL,
                TaskMode.READ_ONLY, IsolationMode.SHARED, List.of(), List.of("c1"), null, null);
        var config = new WorkflowConfig("multi", 2, null, List.of(
                new WorkflowConfig.Phase("collect", false, FailurePolicy.ABORT, List.of(), List.of(c1)),
                new WorkflowConfig.Phase("process", false, FailurePolicy.ABORT, List.of("collect"), List.of(p1))));
        var plan = engine.compile(config);
        assertEquals("collect", plan.phases().get(0).name());
        assertEquals("process", plan.phases().get(1).name());
    }

    @Test
    void parallelPhaseDetectsWriteScopeConflict() {
        var a = new WorkflowConfig.Task("a", "task a", AgentType.IMPLEMENTER,
                TaskMode.READ_WRITE, IsolationMode.WORKTREE, List.of("src/shared"), List.of(), null, null);
        var b = new WorkflowConfig.Task("b", "task b", AgentType.IMPLEMENTER,
                TaskMode.READ_WRITE, IsolationMode.WORKTREE, List.of("src/shared"), List.of(), null, null);
        var config = new WorkflowConfig("c", 4, null, List.of(
                new WorkflowConfig.Phase("p1", true, FailurePolicy.ABORT, List.of(), List.of(a, b))));
        assertThrows(WorkflowValidationError.Wrapped.class, () -> engine.compile(config));
    }

    @Test
    void emptyGoalRejected() {
        assertThrows(WorkflowValidationError.Wrapped.class,
                () -> engine.compile(new WorkflowConfig("", 4, null, List.of())));
    }

    @Test
    void emptyWorkflowRejected() {
        assertThrows(WorkflowValidationError.Wrapped.class,
                () -> engine.compile(new WorkflowConfig("goal", 4, null, List.of())));
    }

    @Test
    void emptyPhaseRejected() {
        assertThrows(WorkflowValidationError.Wrapped.class, () ->
                engine.compile(new WorkflowConfig("goal", 4, null, List.of(
                        new WorkflowConfig.Phase("empty", false, FailurePolicy.ABORT, List.of(), List.of())))));
    }

    @Test
    void duplicateTaskRejected() {
        assertThrows(WorkflowValidationError.Wrapped.class, () ->
                engine.compile(new WorkflowConfig("goal", 4, null, List.of(
                        new WorkflowConfig.Phase("p1", false, FailurePolicy.ABORT, List.of(),
                                List.of(t("t1"), t("t1")))))));
    }

    @Test
    void invalidMaxConcurrentRejected() {
        assertThrows(WorkflowValidationError.Wrapped.class, () ->
                engine.compile(new WorkflowConfig("goal", 100, null, List.of(
                        new WorkflowConfig.Phase("p1", false, FailurePolicy.ABORT, List.of(), List.of(t("t1")))))));
    }

    @Test
    void invalidPhaseDependencyRejected() {
        assertThrows(WorkflowValidationError.Wrapped.class, () ->
                engine.compile(new WorkflowConfig("goal", 4, null, List.of(
                        new WorkflowConfig.Phase("p1", false, FailurePolicy.ABORT,
                                List.of("nonexistent"), List.of(t("t1")))))));
    }

    @Test
    void unavailableTaskResultDependencyRejected() {
        var t1 = new WorkflowConfig.Task("t1", "first", AgentType.GENERAL,
                TaskMode.READ_ONLY, IsolationMode.SHARED, List.of(), List.of("t2"), null, null);
        var config = new WorkflowConfig("goal", 4, null, List.of(
                new WorkflowConfig.Phase("p1", false, FailurePolicy.ABORT, List.of(), List.of(t1)),
                new WorkflowConfig.Phase("p2", false, FailurePolicy.ABORT, List.of("p1"), List.of(t("t2")))));
        assertThrows(WorkflowValidationError.Wrapped.class, () -> engine.compile(config));
    }

    @Test
    void circularDependencyRejected() {
        var config = new WorkflowConfig("goal", 4, null, List.of(
                new WorkflowConfig.Phase("p1", false, FailurePolicy.ABORT, List.of("p2"), List.of(t("t1"))),
                new WorkflowConfig.Phase("p2", false, FailurePolicy.ABORT, List.of("p1"), List.of(t("t2")))));
        assertThrows(WorkflowValidationError.Wrapped.class, () -> engine.compile(config));
    }

    @Test
    void createExecutionDefaultsToSucceeded() {
        var exec = engine.createExecution();
        assertEquals(WorkflowRunStatus.SUCCEEDED, exec.status());
        assertTrue(exec.leafResults().isEmpty());
        assertTrue(exec.branchResults().isEmpty());
    }

    @Test
    void transitionExecutionChangesStatus() {
        var exec = engine.createExecution();
        var failed = engine.transition(exec, WorkflowRunStatus.FAILED);
        assertEquals(WorkflowRunStatus.FAILED, failed.status());
    }

    @Test
    void configRoundTrip() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var config = new WorkflowConfig("test", 4, null, List.of(
                new WorkflowConfig.Phase("p1", false, FailurePolicy.ABORT, List.of(), List.of(t("t1")))));
        var json = mapper.writeValueAsString(config);
        assertTrue(json.contains("\"goal\":\"test\""));
        assertTrue(json.contains("\"max_concurrent\":4"));
        var back = mapper.readValue(json, WorkflowConfig.class);
        assertEquals("test", back.goal());
        assertEquals("t1", back.phases().get(0).tasks().get(0).id());
    }
}
