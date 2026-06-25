package com.jay.tui.core;

import com.jay.tui.client.StreamEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks a single LLM turn: step counter, cancellation, timing, and usage.
 * Mirrors Rust {@code TurnContext}.
 */
public class TurnContext {

    private final String turnId;
    private final Instant startedAt;
    private int step;
    private final int maxSteps;
    private int toolCallCount;
    private final AtomicBoolean cancelled;
    private long inputTokens;
    private long outputTokens;
    private Long cacheCreationTokens;
    private Long cacheReadTokens;

    public TurnContext(int maxSteps) {
        this.turnId = "turn-" + UUID.randomUUID().toString().substring(0, 8);
        this.startedAt = Instant.now();
        this.step = 0;
        this.maxSteps = maxSteps;
        this.toolCallCount = 0;
        this.cancelled = new AtomicBoolean(false);
    }

    /** Advance to the next step. Returns true if there are more steps. */
    public boolean nextStep() {
        step++;
        return step < maxSteps;
    }

    /** Whether the maximum number of steps has been reached. */
    public boolean atMaxSteps() {
        return step >= maxSteps;
    }

    /** Record a tool call in this turn. */
    public void recordToolCall() {
        toolCallCount++;
    }

    /** Whether any tool calls have been made. */
    public boolean hasToolCalls() {
        return toolCallCount > 0;
    }

    /** Mark the turn as cancelled. */
    public void cancel() {
        cancelled.set(true);
    }

    /** Reset the cancel flag. */
    public void resetCancel() {
        cancelled.set(false);
    }

    /** Whether this turn has been cancelled. */
    public boolean isCancelled() {
        return cancelled.get();
    }

    /** Accumulate usage from a stream event. */
    public void addUsage(StreamEvent.Usage usage) {
        if (usage != null) {
            inputTokens += usage.promptTokens();
            outputTokens += usage.completionTokens();
        }
    }

    /** Accumulate raw token counts. */
    public void addUsage(long input, long output) {
        inputTokens += input;
        outputTokens += output;
    }

    // ── Getters ───────────────────────────────────────────────────────

    public String turnId() { return turnId; }
    public Duration elapsed() { return Duration.between(startedAt, Instant.now()); }
    public int step() { return step; }
    public int maxSteps() { return maxSteps; }
    public int toolCallCount() { return toolCallCount; }

    public long inputTokens() { return inputTokens; }
    public long outputTokens() { return outputTokens; }

    public StreamEvent.Usage toUsage() {
        return new StreamEvent.Usage((int) inputTokens, (int) outputTokens);
    }
}
