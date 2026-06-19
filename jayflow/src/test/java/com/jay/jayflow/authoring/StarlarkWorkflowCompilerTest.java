package com.jay.jayflow.authoring;

import com.jay.jayflow.authoring.StarlarkWorkflowCompiler.StarlarkCompileException;
import com.jay.jayflow.execution.MockWorkflowExecutor;
import com.jay.jayflow.ir.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StarlarkWorkflowCompilerTest {

    // ── Positive compilation tests ──────────────────────────

    @Test
    void starlarkCompilesToIR() {
        var source = """
            workflow(
                id = "rlm-cache-change",
                goal = "Implement RLM cache change with parallel discovery",
                nodes = [
                    branch(
                        id = "candidate-branches",
                        parallel = True,
                        children = [
                            agent(
                                id = "map-dependency-move",
                                prompt = "Rename and move the dependency",
                                agent_type = "explore",
                            ),
                        ],
                    ),
                    reduce(
                        id = "verify",
                        prompt = "Merge findings",
                    ),
                ],
            )
            """;

        var wf = StarlarkWorkflowCompiler.compile("test.star", source);
        assertEquals("rlm-cache-change", wf.id());
        assertEquals(2, wf.nodes().size());
        var branch = (WorkflowNode.BranchSet) wf.nodes().get(0);
        assertEquals("candidate-branches", branch.id());
        assertTrue(branch.parallel());
        var leaf = (WorkflowNode.Leaf) branch.children().get(0);
        assertEquals(AgentType.EXPLORE, leaf.agentType());
    }

    @Test
    void rlmCacheChangeWorkflowRunsWithMockProvider() {
        var source = """
            workflow(
                id = "rlm-cache-change",
                goal = "Implement RLM cache change",
                nodes = [
                    branch(
                        id = "candidate-branches",
                        children = [
                            agent(id = "regression-tests", prompt = "Run tests", agent_type = "verifier"),
                        ],
                    ),
                    teacher_review(id = "teacher-review"),
                    reduce(id = "summarize-cache-change", prompt = "Merge"),
                    loop_until(
                        id = "implement-until-tests-pass",
                        condition = "tests pass",
                        max_iterations = 3,
                        children = [ agent(id = "run-check", prompt = "Run checks") ],
                    ),
                ],
            )
            """;

        var wf = StarlarkWorkflowCompiler.compile("dogfood.star", source);
        var executor = new MockWorkflowExecutor()
                .withPredicateResults("implement-until-tests-pass", List.of(true));
        var exec = executor.run(wf);

        assertEquals(WorkflowRunStatus.SUCCEEDED, exec.status());
        assertTrue(exec.leafResults().stream().anyMatch(r -> r.leafId().equals("regression-tests")));
        assertTrue(exec.controlNodeResults().stream().anyMatch(r -> r.nodeId().equals("teacher-review")));
    }

    // ── Repair tests ────────────────────────────────────────

    @Test
    void starlarkRepairLoop() {
        var source = """
            workflow(
                id = "repair-demo",
                goal = "repair ctx aliases",
                nodes = [
                    ctx.parallel(id = "discover", children = [
                        agent(id = "scan", prompt = "scan repo"),
                    ]),
                ],
            )
            """;

        var wf = StarlarkWorkflowCompiler.compileWithRepair("repair.star", source);
        assertEquals("repair-demo", wf.id());
        assertInstanceOf(WorkflowNode.BranchSet.class, wf.nodes().get(0));
    }

    @Test
    void starlarkGeneratedWorkflowRepairsThenRuns() {
        var source = """
            workflow(
                id = "generated-repair-run",
                goal = "repair generated workflow aliases",
                nodes = [
                    ctx.parallel(id = "discover", children = [
                        agent(id = "scan", prompt = "scan repo"),
                    ]),
                    ctx.loop_until(
                        id = "verify",
                        condition = "checks pass",
                        max_iterations = 1,
                        children = [
                            test(id = "run-tests", command = "cargo test"),
                        ],
                    ),
                ],
            )
            """;

        var wf = StarlarkWorkflowCompiler.compileWithRepair("generated.star", source);
        var executor = new MockWorkflowExecutor().withPredicateResults("verify", List.of(true));
        var exec = executor.run(wf);
        assertEquals(WorkflowRunStatus.SUCCEEDED, exec.status());
        assertEquals(2, exec.leafResults().size());
    }

    // ── Rejection tests ─────────────────────────────────────

    @Test
    void invalidWorkflowRejected() {
        var source = """
            load("@stdlib//fs.star", "open")
            workflow(goal = "bad", nodes = [])
            """;

        var ex = assertThrows(StarlarkCompileException.class,
                () -> StarlarkWorkflowCompiler.compile("invalid.star", source));
        assertInstanceOf(StarlarkWorkflowError.UnsupportedConstruct.class, ex.error());
        assertEquals("load", ((StarlarkWorkflowError.UnsupportedConstruct) ex.error()).construct());
    }

    @Test
    void starlarkCompileGateRejectsUnknownReferences() {
        var source = """
            workflow(
                id = "bad-reference",
                goal = "reject missing candidates",
                nodes = [
                    teacher_review(id = "review", candidates = ["missing-candidate"]),
                ],
            )
            """;

        var ex = assertThrows(StarlarkCompileException.class,
                () -> StarlarkWorkflowCompiler.compile("bad-ref.star", source));
        assertInstanceOf(StarlarkWorkflowError.InvalidNode.class, ex.error());
        assertTrue(ex.getMessage().contains("missing-candidate"));
    }

    // ── Built-in coverage tests ─────────────────────────────

    @Test
    void searchBuiltinCreatesExploreAgent() {
        var source = """
            workflow(goal = "search test", nodes = [
                search(id = "find-symbols", query = "MyClass"),
            ])
            """;
        var wf = StarlarkWorkflowCompiler.compile("search.star", source);
        var leaf = (WorkflowNode.Leaf) wf.nodes().get(0);
        assertEquals(AgentType.EXPLORE, leaf.agentType());
        assertTrue(leaf.prompt().contains("Search codebase:"));
        assertTrue(leaf.prompt().contains("MyClass"));
    }

    @Test
    void testBuiltinCreatesVerifierAgent() {
        var source = """
            workflow(goal = "test run", nodes = [
                test(id = "run-unit", command = "cargo test"),
            ])
            """;
        var wf = StarlarkWorkflowCompiler.compile("test.star", source);
        var leaf = (WorkflowNode.Leaf) wf.nodes().get(0);
        assertEquals(AgentType.VERIFIER, leaf.agentType());
        assertTrue(leaf.prompt().contains("Run test command: cargo test"));
    }

    @Test
    void shellBuiltinCreatesVerifierAgent() {
        var source = """
            workflow(goal = "shell test", nodes = [
                shell(id = "format", command = "cargo fmt"),
            ])
            """;
        var wf = StarlarkWorkflowCompiler.compile("shell.star", source);
        var leaf = (WorkflowNode.Leaf) wf.nodes().get(0);
        assertEquals(AgentType.VERIFIER, leaf.agentType());
        assertTrue(leaf.prompt().contains("Run shell command: cargo fmt"));
    }

    @Test
    void sequenceAndWhenNodesCompile() {
        var source = """
            workflow(
                id = "sequence-test",
                goal = "sequence and when",
                nodes = [
                    sequence(id = "seq", children = [
                        agent(id = "step1", prompt = "first"),
                        agent(id = "step2", prompt = "second"),
                    ]),
                    when(
                        id = "choose",
                        condition = "x > 0",
                        then_nodes = [ agent(id = "yes", prompt = "positive") ],
                        else_nodes = [ agent(id = "no", prompt = "negative") ],
                    ),
                ],
            )
            """;
        var wf = StarlarkWorkflowCompiler.compile("seq.star", source);
        assertEquals(2, wf.nodes().size());
        assertInstanceOf(WorkflowNode.Sequence.class, wf.nodes().get(0));
        assertInstanceOf(WorkflowNode.Cond.class, wf.nodes().get(1));
    }

    @Test
    void expandNodeCompiles() {
        var source = """
            workflow(goal = "expand test", nodes = [
                expand(id = "split", source = "plan", max_children = 4),
            ])
            """;
        var wf = StarlarkWorkflowCompiler.compile("expand.star", source);
        var expand = (WorkflowNode.Expand) wf.nodes().get(0);
        assertEquals("split", expand.id());
        assertEquals(4, expand.maxChildren());
    }

    // ── Enum validation tests ───────────────────────────────

    @Test
    void invalidAgentTypeRejected() {
        var source = """
            workflow(goal = "bad agent", nodes = [
                agent(id = "a", prompt = "hi", agent_type = "invalid"),
            ])
            """;
        var ex = assertThrows(StarlarkCompileException.class,
                () -> StarlarkWorkflowCompiler.compile("bad.star", source));
        assertInstanceOf(StarlarkWorkflowError.InvalidEnum.class, ex.error());
        var ie = (StarlarkWorkflowError.InvalidEnum) ex.error();
        assertEquals("agent_type", ie.field());
        assertEquals("invalid", ie.value());
    }

    @Test
    void tournamentAliasMapsToTeacherReview() {
        var source = """
            workflow(goal = "tournament test", nodes = [
                agent(id = "c1", prompt = "candidate 1"),
                agent(id = "c2", prompt = "candidate 2"),
                tournament(id = "select", candidates = ["c1", "c2"]),
            ])
            """;
        var wf = StarlarkWorkflowCompiler.compile("tourney.star", source);
        assertTrue(wf.nodes().stream().anyMatch(n -> n instanceof WorkflowNode.TeacherReview));
    }
}
