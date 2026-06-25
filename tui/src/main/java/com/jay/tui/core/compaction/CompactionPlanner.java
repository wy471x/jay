package com.jay.tui.core.compaction;

import com.jay.tui.client.ChatMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides when and how to compact conversation history.
 * Mirrors Rust compaction planner.
 *
 * <p>Strategy:
 * <ul>
 *   <li>Estimate tokens as ~4 chars per token</li>
 *   <li>Trigger compaction when estimated tokens exceed threshold</li>
 *   <li>Produce a plan: pin recent messages, summarize older ones</li>
 * </ul>
 */
public class CompactionPlanner {

    private static final int CHARS_PER_TOKEN = 4;
    private static final int PIN_RECENT_COUNT = 10;  // pin last 10 messages

    private final CompactionConfig config;

    public CompactionPlanner(CompactionConfig config) {
        this.config = config;
    }

    /** Whether compaction should be triggered. */
    public boolean shouldCompact(List<ChatMessage> messages) {
        if (!config.enabled() || config.tokenThreshold() <= 0) {
            return false;
        }
        return estimateTokens(messages) >= config.tokenThreshold();
    }

    /** Estimate total tokens from message content. */
    public static int estimateTokens(List<ChatMessage> messages) {
        if (messages.isEmpty()) return 0;
        long totalChars = 0;
        for (var msg : messages) {
            if (msg.content() != null) totalChars += msg.content().length();
            if (msg.toolCalls() != null) {
                for (var tc : msg.toolCalls()) {
                    if (tc.function() != null && tc.function().arguments() != null) {
                        totalChars += tc.function().arguments().length();
                    }
                }
            }
        }
        return (int) (totalChars / CHARS_PER_TOKEN);
    }

    /**
     * Produce a compaction plan.
     * Returns the indices of messages to keep pinned (not summarized).
     */
    public CompactionPlan plan(List<ChatMessage> messages) {
        int total = messages.size();
        int summarizeUntil = Math.max(0, total - PIN_RECENT_COUNT);

        // Messages before summarizeUntil get summarized
        List<Integer> toSummarize = new ArrayList<>();
        for (int i = 0; i < summarizeUntil; i++) {
            toSummarize.add(i);
        }

        // Messages from summarizeUntil onward are pinned
        List<Integer> pinned = new ArrayList<>();
        for (int i = summarizeUntil; i < total; i++) {
            pinned.add(i);
        }

        return new CompactionPlan(toSummarize, pinned,
                "Compacting " + toSummarize.size() + " messages, keeping "
                        + pinned.size() + " recent");
    }

    /** Result of compaction planning. */
    public record CompactionPlan(
            List<Integer> toSummarizeIndices,
            List<Integer> pinnedIndices,
            String description
    ) {
        public int messagesBefore() {
            return toSummarizeIndices.size() + pinnedIndices.size();
        }

        public int messagesAfter() {
            return pinnedIndices.size() + 1; // +1 for summary message
        }
    }
}
