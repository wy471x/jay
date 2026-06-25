package com.jay.tui.core.turn;

import com.fasterxml.jackson.databind.JsonNode;
import com.jay.tui.client.ContentBlock;
import com.jay.tui.core.TuiEngineEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Plans and executes tool calls from one turn step.
 * Mirrors Rust {@code tool_execution.rs} — batching, parallel/serial dispatch.
 */
public class ToolDispatcher {

    /** Maximum number of tool calls to execute in parallel. */
    private static final int MAX_PARALLEL_TOOLS = 8;

    private final BlockingQueue<TuiEngineEvent> eventQueue;

    public ToolDispatcher(BlockingQueue<TuiEngineEvent> eventQueue) {
        this.eventQueue = eventQueue;
    }

    /**
     * Plan execution batches from tool-use blocks.
     * Groups parallel-safe tools together, serializes others.
     */
    public List<ToolBatch> planBatches(List<ContentBlock.ToolUse> toolUses) {
        List<ToolBatch> batches = new ArrayList<>();
        List<ToolExecutionPlan> parallelGroup = new ArrayList<>();

        for (var tu : toolUses) {
            var plan = new ToolExecutionPlan(tu.id(), tu.name(), tu.input());
            if (plan.supportsParallel() && parallelGroup.size() < MAX_PARALLEL_TOOLS) {
                parallelGroup.add(plan);
            } else {
                // Flush current parallel group if any
                if (!parallelGroup.isEmpty()) {
                    batches.add(new ToolBatch.Parallel(List.copyOf(parallelGroup)));
                    parallelGroup.clear();
                }
                batches.add(new ToolBatch.Serial(plan));
            }
        }
        // Flush remaining parallel group
        if (!parallelGroup.isEmpty()) {
            batches.add(new ToolBatch.Parallel(List.copyOf(parallelGroup)));
        }
        return batches;
    }

    /**
     * Execute a single tool call, emitting appropriate engine events.
     * This is a synchronous placeholder — real implementation connects
     * to the jay/tools module for actual execution.
     */
    public TuiEngineEvent.ToolResult executeTool(ToolExecutionPlan plan) {
        emit(new TuiEngineEvent.ToolCallStarted(plan.id(), plan.name(),
                plan.input().toString()));

        // Placeholder execution — real tool dispatch connects to ToolRegistry
        TuiEngineEvent.ToolResult result = TuiEngineEvent.ToolResult.success(
                plan.id(), plan.name(), "Tool executed: " + plan.name());

        emit(new TuiEngineEvent.ToolCallComplete(
                plan.id(), plan.name(), result));
        return result;
    }

    /** Execute a batch of tools (parallel or serial). */
    public List<TuiEngineEvent.ToolResult> executeBatch(ToolBatch batch) {
        List<TuiEngineEvent.ToolResult> results = new ArrayList<>();
        switch (batch) {
            case ToolBatch.Parallel p -> {
                // Execute in parallel using virtual threads
                List<Thread> threads = new ArrayList<>();
                List<TuiEngineEvent.ToolResult> parallelResults = new ArrayList<>();
                for (var plan : p.plans()) {
                    threads.add(Thread.ofVirtual().start(() -> {
                        var result = executeTool(plan);
                        synchronized (parallelResults) {
                            parallelResults.add(result);
                        }
                    }));
                }
                for (var t : threads) {
                    try { t.join(30_000); } catch (InterruptedException e) { }
                }
                results.addAll(parallelResults);
            }
            case ToolBatch.Serial s -> {
                results.add(executeTool(s.plan()));
            }
        }
        return results;
    }

    private void emit(TuiEngineEvent event) {
        eventQueue.offer(event);
    }

    // ── ToolBatch sealed interface ───────────────────────────────────

    public sealed interface ToolBatch {
        record Parallel(List<ToolExecutionPlan> plans) implements ToolBatch {}
        record Serial(ToolExecutionPlan plan) implements ToolBatch {}
    }

    // ── ToolExecutionPlan ────────────────────────────────────────────

    public record ToolExecutionPlan(String id, String name, JsonNode input) {
        /** Whether this tool can run in parallel with others. */
        public boolean supportsParallel() {
            return switch (name) {
                case "bash", "shell", "write_file", "edit_file",
                     "apply_patch", "execute_command" -> false;  // mutation: serial
                default -> true;  // read-only tools: parallel-safe
            };
        }
    }
}
