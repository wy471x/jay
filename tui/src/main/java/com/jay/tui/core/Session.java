package com.jay.tui.core;

import com.jay.tui.client.ChatMessage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages conversation state for the engine: messages, token usage,
 * system prompt, and revision tracking.
 *
 * <p>Mirrors Rust {@code Session} struct.
 * Every mutation bumps {@link #messagesRevision()} so the cache layer
 * can detect changes and invalidate stale entries.
 */
public class Session {

    private String model;
    private final Path workspace;
    private boolean allowShell;
    private boolean trustMode;
    private boolean autoApprove;
    private String systemPrompt;
    private boolean systemPromptOverride;
    private String compactionSummaryPrompt;
    private final List<ChatMessage> messages;
    private final AtomicLong messagesRevision;
    private final SessionUsage totalUsage;
    private final String id;

    public Session(String model, Path workspace, boolean allowShell, boolean trustMode) {
        this.model = model;
        this.workspace = workspace;
        this.allowShell = allowShell;
        this.trustMode = trustMode;
        this.autoApprove = false;
        this.systemPrompt = null;
        this.systemPromptOverride = false;
        this.compactionSummaryPrompt = null;
        this.messages = new ArrayList<>();
        this.messagesRevision = new AtomicLong(0);
        this.totalUsage = new SessionUsage();
        this.id = "session-" + System.currentTimeMillis();
    }

    // ── Message mutation ──────────────────────────────────────────

    /** Append a single message, bumping the revision counter. */
    public void addMessage(ChatMessage message) {
        messages.add(message);
        messagesRevision.incrementAndGet();
    }

    /** Bulk replace all messages, bumping revision once. */
    public void replaceMessages(List<ChatMessage> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        messagesRevision.incrementAndGet();
    }

    /** Bump revision for in-place mutations (e.g. editing last turn). */
    public void bumpMessagesRevision() {
        messagesRevision.incrementAndGet();
    }

    /** Remove the last N messages from the conversation. */
    public void trimLast(int count) {
        int from = Math.max(0, messages.size() - count);
        messages.subList(from, messages.size()).clear();
        messagesRevision.incrementAndGet();
    }

    // ── Usage tracking ────────────────────────────────────────────

    public void addUsage(long inputTokens, long outputTokens,
                         Long cacheCreationTokens, Long cacheReadTokens) {
        totalUsage.add(inputTokens, outputTokens, cacheCreationTokens, cacheReadTokens);
    }

    // ── Getters / Setters ──────────────────────────────────────────

    public String model() { return model; }
    public void setModel(String m) { this.model = m; }

    public Path workspace() { return workspace; }

    public boolean allowShell() { return allowShell; }
    public void setAllowShell(boolean v) { this.allowShell = v; }

    public boolean trustMode() { return trustMode; }
    public void setTrustMode(boolean v) { this.trustMode = v; }

    public boolean autoApprove() { return autoApprove; }
    public void setAutoApprove(boolean v) { this.autoApprove = v; }

    public String systemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String prompt) { this.systemPrompt = prompt; }

    public boolean systemPromptOverride() { return systemPromptOverride; }
    public void setSystemPromptOverride(boolean v) { this.systemPromptOverride = v; }

    public String compactionSummaryPrompt() { return compactionSummaryPrompt; }
    public void setCompactionSummaryPrompt(String p) { this.compactionSummaryPrompt = p; }

    public List<ChatMessage> messages() { return List.copyOf(messages); }

    /** Return the internal mutable list for engine-internal use (SseTurnLoop). */
    public List<ChatMessage> mutableMessages() { return messages; }

    public long messagesRevision() { return messagesRevision.get(); }

    public SessionUsage totalUsage() { return totalUsage; }

    public String id() { return id; }

    // ── SessionUsage inner class ──────────────────────────────────

    /** Accumulated token usage across all turns. */
    public static class SessionUsage {
        private long inputTokens;
        private long outputTokens;
        private Long cacheCreationInputTokens;
        private Long cacheReadInputTokens;

        public void add(long input, long output, Long cacheCreation, Long cacheRead) {
            this.inputTokens += input;
            this.outputTokens += output;
            if (cacheCreation != null) {
                if (this.cacheCreationInputTokens == null) {
                    this.cacheCreationInputTokens = cacheCreation;
                } else {
                    this.cacheCreationInputTokens += cacheCreation;
                }
            }
            if (cacheRead != null) {
                if (this.cacheReadInputTokens == null) {
                    this.cacheReadInputTokens = cacheRead;
                } else {
                    this.cacheReadInputTokens += cacheRead;
                }
            }
        }

        public long inputTokens() { return inputTokens; }
        public long outputTokens() { return outputTokens; }
        public Long cacheCreationInputTokens() { return cacheCreationInputTokens; }
        public Long cacheReadInputTokens() { return cacheReadInputTokens; }
        public long totalTokens() { return inputTokens + outputTokens; }
    }
}
