package com.jay.tui.core.compaction;

import com.jay.tui.client.ChatMessage;
import com.jay.tui.client.LlmClient;
import com.jay.tui.core.ErrorTaxonomy;
import com.jay.tui.core.TuiEngineEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiFunction;

/**
 * Executes context compaction: local pruning + LLM summary + message replacement.
 * Mirrors Rust {@code compact_messages_safe} + {@code create_summary}.
 *
 * <h3>Flow</h3>
 * <ol>
 *   <li>{@link #pruneToolResultsUntil} — local pre-compaction: truncate large tool outputs</li>
 *   <li>{@link #compactMessagesSafe} — retry wrapper (3 retries, exponential backoff)</li>
 *   <li>{@link #createSummary} — send summarized messages to LLM for summary</li>
 *   <li>Build new message list: system summary + pinned recent messages</li>
 * </ol>
 */
public class CompactionExecutor {

    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 1000;

    // Summary input limits
    static final int SUMMARY_TEXT_SNIPPET_CHARS = 800;
    static final int SUMMARY_TOOL_RESULT_SNIPPET_CHARS = 240;
    static final int SUMMARY_INPUT_MAX_CHARS = 24_000;
    static final int SUMMARY_INPUT_HEAD_CHARS = 14_000;
    static final int SUMMARY_INPUT_TAIL_CHARS = 6_000;
    static final int SUMMARY_MAX_TOKENS = 1024;
    static final int SUMMARY_WORD_LIMIT = 300;

    // Tool result truncation
    private static final int TOOL_RESULT_MAX_CHARS = 12_000;
    private static final int TOOL_RESULT_HEAD_CHARS = 4_000;
    private static final int TOOL_RESULT_TAIL_CHARS = 4_000;
    private static final int TOOL_PRUNE_STOP_CHECK_BYTES = 16 * 1024;

    // Compaction summary prompt
    private static final String SUMMARY_SYSTEM_PROMPT = """
            You are a conversation summarizer. Your task is to create a concise \
            summary of the conversation so far, preserving key decisions, code changes, \
            file paths, errors encountered, and the user's explicit requests.

            Include:
            - What the user asked for and what was accomplished
            - Key files that were read, modified, or created (with paths)
            - Errors that were encountered and how they were resolved
            - Important decisions or design choices made
            - Any unfinished tasks or open questions

            Be specific and factual. Do not add commentary or speculation.""";

    private static final String SUMMARY_USER_PROMPT = """
            Please summarize the following conversation. Focus on what was accomplished, \
            what files were changed, and what remains to be done.

            Conversation:
            %s""";

    private final LlmClient llmClient;
    private final CompactionConfig config;
    private final BlockingQueue<TuiEngineEvent> eventQueue;

    public CompactionExecutor(LlmClient llmClient, CompactionConfig config,
                              BlockingQueue<TuiEngineEvent> eventQueue) {
        this.llmClient = llmClient;
        this.config = config;
        this.eventQueue = eventQueue;
    }

    // ── Public API ────────────────────────────────────────────────────

    /**
     * Execute compaction with retry and backoff for transient errors.
     *
     * @return compaction result with new message list and metadata
     */
    public CompactionResult compactMessagesSafe(List<ChatMessage> messages) {
        var planner = new CompactionPlanner(config);
        var plan = planner.planCompaction(messages);

        if (plan.summarizeIndices().isEmpty()) {
            return new CompactionResult(messages, null, List.of(), 0);
        }

        // Local pre-pruning
        boolean wasOverThreshold = planner.shouldCompact(messages);
        var pruned = new ArrayList<>(messages);
        int prunedBytes = pruneToolResultsUntil(pruned);
        boolean nowUnderThreshold = !planner.shouldCompact(pruned);

        if (prunedBytes > 0) {
            emit(new TuiEngineEvent.StatusMessage(
                    "Local prune saved " + prunedBytes + " bytes", "info"));
            if (wasOverThreshold && nowUnderThreshold) {
                return new CompactionResult(pruned, null, List.of(), 0);
            }
        }

        List<ChatMessage> input = prunedBytes > 0 ? pruned : messages;

        // Retry loop
        Exception lastError = null;
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            if (attempt > 0) {
                long delay = BASE_DELAY_MS * (1L << (attempt - 1));
                try { Thread.sleep(delay); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                emit(new TuiEngineEvent.StatusMessage(
                        "Compaction retry " + (attempt + 1) + "/" + MAX_RETRIES, "info"));
            }

            try {
                var result = compactMessages(input);
                return new CompactionResult(
                        result.left(), result.middle(), result.right(), attempt);
            } catch (Exception e) {
                lastError = e;
                if (!isTransientError(e)) break;
            }
        }

        // All retries failed — return original messages
        emit(new TuiEngineEvent.EngineError(
                "Compaction failed after " + MAX_RETRIES + " attempts: "
                        + (lastError != null ? lastError.getMessage() : "unknown"),
                "compaction", false));
        return new CompactionResult(messages, null, List.of(), MAX_RETRIES);
    }

    // ── Core compaction ───────────────────────────────────────────────

    /**
     * Core compaction: plan → build transcript → call LLM → replace with summary.
     *
     * @return triple of (new messages, summary prompt text, removed message list)
     */
    private Triple<List<ChatMessage>, String, List<ChatMessage>> compactMessages(
            List<ChatMessage> messages) throws Exception {

        var planner = new CompactionPlanner(config);
        var plan = planner.planCompaction(messages);

        if (plan.summarizeIndices().isEmpty()) {
            return new Triple<>(messages, null, List.of());
        }

        // Build the list of messages to summarize
        List<ChatMessage> toSummarize = new ArrayList<>();
        for (int idx : plan.summarizeIndices()) {
            if (idx < messages.size()) toSummarize.add(messages.get(idx));
        }

        // Build the list of pinned messages (kept as-is)
        List<ChatMessage> pinned = new ArrayList<>();
        for (int idx : plan.pinnedIndices()) {
            if (idx < messages.size()) pinned.add(messages.get(idx));
        }

        // Call LLM for summary
        String summary = createSummary(toSummarize);
        if (summary == null || summary.isBlank()) {
            throw new Exception("LLM summary returned empty result");
        }

        // Build new message list: system summary + pinned messages
        List<ChatMessage> newMessages = new ArrayList<>();
        newMessages.add(ChatMessage.system("[Previous conversation summary]\n" + summary));
        newMessages.addAll(pinned);

        return new Triple<>(newMessages, summary, toSummarize);
    }

    // ── LLM Summary ───────────────────────────────────────────────────

    /**
     * Send summarized messages to LLM and get a concise summary.
     * Formats messages as a transcript and sends with a summarization prompt.
     */
    private String createSummary(List<ChatMessage> messages) throws Exception {
        if (messages.isEmpty()) return "";

        // Build formatted transcript
        String transcript = buildFormattedTranscript(messages);

        var summaryMessages = List.of(
                ChatMessage.system(SUMMARY_SYSTEM_PROMPT),
                ChatMessage.user(String.format(SUMMARY_USER_PROMPT, transcript))
        );

        String model = config.model() != null ? config.model()
                : CompactionConfig.DEFAULT_SUMMARY_MODEL;

        var result = llmClient.chatBlocking(model, summaryMessages, 0.1, SUMMARY_MAX_TOKENS);
        if (!result.success() || result.content() == null) {
            throw new Exception("Summary call failed: " + result.error());
        }

        emit(new TuiEngineEvent.StatusMessage(
                "Compaction summary generated", "info"));
        return result.content();
    }

    /**
     * Build a formatted transcript from messages for the summary prompt.
     * Mirrors Rust {@code build_formatted_summary_request}.
     */
    public static String buildFormattedTranscript(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (var msg : messages) {
            String role = msg.role();
            String content = msg.content() != null ? msg.content() : "";

            switch (role) {
                case "user" -> {
                    sb.append("User: ");
                    sb.append(truncateChars(content, SUMMARY_TEXT_SNIPPET_CHARS));
                    sb.append('\n');
                }
                case "assistant" -> {
                    sb.append("Assistant: ");
                    sb.append(truncateChars(content, SUMMARY_TEXT_SNIPPET_CHARS));
                    sb.append('\n');
                }
                case "tool" -> {
                    sb.append("Tool result");
                    if (msg.toolCallId() != null) sb.append(" [").append(msg.toolCallId()).append(']');
                    sb.append(": ");
                    sb.append(truncateChars(content, SUMMARY_TOOL_RESULT_SNIPPET_CHARS));
                    sb.append('\n');
                }
                case "system" -> {
                    sb.append("System: ");
                    sb.append(truncateChars(content, SUMMARY_TEXT_SNIPPET_CHARS));
                    sb.append('\n');
                }
                default -> {
                    sb.append(role).append(": ");
                    sb.append(truncateChars(content, SUMMARY_TEXT_SNIPPET_CHARS));
                    sb.append('\n');
                }
            }
        }

        // Apply head-tail truncation if total exceeds limit
        String result = sb.toString();
        if (result.length() > SUMMARY_INPUT_MAX_CHARS) {
            result = headTailTruncate(result, SUMMARY_INPUT_HEAD_CHARS, SUMMARY_INPUT_TAIL_CHARS);
        }
        return result;
    }

    // ── Local tool-result pruning ─────────────────────────────────────

    /**
     * Prune tool results to reduce context size before LLM compaction.
     * Truncates each tool result that exceeds the max character limit.
     *
     * @return total bytes saved
     */
    public int pruneToolResultsUntil(List<ChatMessage> messages) {
        int totalSaved = 0;
        for (int i = 0; i < messages.size(); i++) {
            var msg = messages.get(i);
            if ("tool".equals(msg.role()) && msg.content() != null) {
                String content = msg.content();
                if (content.length() > TOOL_RESULT_MAX_CHARS) {
                    int before = content.length();
                    String truncated = headTailTruncate(content,
                            TOOL_RESULT_HEAD_CHARS, TOOL_RESULT_TAIL_CHARS);
                    messages.set(i, ChatMessage.tool(msg.toolCallId(), truncated));
                    totalSaved += before - truncated.length();
                }
            }
        }
        return totalSaved;
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /** Head-tail truncation with a marker in the middle. */
    public static String headTailTruncate(String text, int headChars, int tailChars) {
        if (text == null || text.length() <= headChars + tailChars) {
            return text != null ? text : "";
        }
        String head = truncateChars(text, headChars);
        String tail = tailChars(text, tailChars);
        return head + "\n... [truncated " + (text.length() - headChars - tailChars)
                + " chars] ...\n" + tail;
    }

    /** Truncate to at most maxChars from beginning. */
    public static String truncateChars(String text, int maxChars) {
        if (text == null || maxChars <= 0) return "";
        if (text.length() <= maxChars) return text;
        return text.substring(0, maxChars);
    }

    /** Take last maxChars characters from text. */
    public static String tailChars(String text, int maxChars) {
        if (text == null || maxChars <= 0) return "";
        if (text.length() <= maxChars) return text;
        return text.substring(text.length() - maxChars);
    }

    private static boolean isTransientError(Exception e) {
        if (e == null) return false;
        var envelope = ErrorTaxonomy.classify(e.getMessage());
        return ErrorTaxonomy.shouldRetry(envelope.category());
    }

    private void emit(TuiEngineEvent event) {
        if (eventQueue != null) eventQueue.offer(event);
    }

    // ── Result types ──────────────────────────────────────────────────

    /** Result of a compaction operation. */
    public record CompactionResult(
            List<ChatMessage> messages,
            String summaryPrompt,
            List<ChatMessage> removedMessages,
            int retriesUsed
    ) {
        public boolean compacted() {
            return !removedMessages.isEmpty();
        }
    }

    /** Simple triple for internal use. */
    private record Triple<A, B, C>(A left, B middle, C right) {}
}
