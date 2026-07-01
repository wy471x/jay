package com.jay.tui.core.seam;

import com.jay.tui.client.ChatMessage;
import com.jay.tui.client.LlmClient;
import com.jay.tui.core.ErrorTaxonomy;
import com.jay.tui.core.TuiEngineEvent;
import com.jay.tui.core.compaction.CompactionExecutor;
import com.jay.tui.core.compaction.CompactionPlanner;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Append-only layered context management — Flash seam manager.
 *
 * <p>Unlike replacement compaction, the SeamManager uses an <b>additive</b> strategy:
 * it generates {@code <archived_context>} XML blocks as assistant messages appended
 * to the end of the conversation. This <b>fully preserves the V4 prefix cache</b>
 * because no existing messages are replaced or rewritten.
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>Each tick, estimate active input tokens</li>
 *   <li>Check if a seam level should fire based on thresholds</li>
 *   <li>If yes: summarize older messages with Flash model, append result as
 *       an {@code <archived_context>} assistant message</li>
 *   <li>At higher levels, recompact prior seams into denser summaries</li>
 * </ol>
 *
 * <h3>Seam levels</h3>
 * L1 (192K) → L2 (384K) → L3 (576K). Each fires at most once, in order.
 */
public class SeamManager {

    // Summary snippet limits
    private static final int TEXT_SNIPPET_CHARS = 800;
    private static final int TOOL_RESULT_SNIPPET_CHARS = 200;

    // Retry config
    private static final int MAX_RETRIES = 2;
    private static final long BASE_DELAY_MS = 1000;

    private final LlmClient flashClient;
    private final SeamConfig config;
    private final BlockingQueue<TuiEngineEvent> eventQueue;
    private final List<SeamMetadata> activeSeams;

    public SeamManager(LlmClient flashClient, SeamConfig config,
                       BlockingQueue<TuiEngineEvent> eventQueue) {
        this.flashClient = flashClient;
        this.config = config;
        this.eventQueue = eventQueue;
        this.activeSeams = new ArrayList<>();
    }

    // ── Public API ────────────────────────────────────────────────────

    /** Current seam count. */
    public int seamCount() {
        return activeSeams.size();
    }

    /** Highest seam level currently recorded. */
    public Integer highestLevel() {
        return activeSeams.isEmpty() ? null
                : activeSeams.get(activeSeams.size() - 1).level();
    }

    /** Get the current config. */
    public SeamConfig config() { return config; }

    /**
     * Determine which seam level (if any) should fire.
     *
     * @param activeInputTokens estimated active input tokens
     * @return level number (1/2/3) or null if no seam is due
     */
    public Integer seamLevelFor(int activeInputTokens) {
        return seamLevelForActiveInput(config, activeInputTokens, highestLevel());
    }

    /**
     * Pure function: determine seam level from config, input tokens, and highest existing.
     */
    public static Integer seamLevelForActiveInput(SeamConfig config,
                                                   int activeInputTokens,
                                                   Integer highestExistingLevel) {
        if (!config.enabled()) return null;

        int highest = highestExistingLevel != null ? highestExistingLevel : 0;

        if (highest < 1 && activeInputTokens >= config.l1Threshold()) return 1;
        if (highest < 2 && activeInputTokens >= config.l2Threshold()) return 2;
        if (highest < 3 && activeInputTokens >= config.l3Threshold()) return 3;

        return null;
    }

    /**
     * Compute the verbatim window start index.
     * The last N turns (user+assistant pairs) are never summarized.
     */
    public int verbatimWindowStart(int messageCount) {
        int turnCount = messageCount / 2;
        int verbatimTurns = Math.min(config.verbatimWindowTurns(), turnCount);
        int verbatimMessages = Math.min(verbatimTurns * 2, messageCount);
        return Math.max(0, messageCount - verbatimMessages);
    }

    /**
     * Produce a soft seam for the given message range and level.
     * Returns the {@code <archived_context>} XML block as a string.
     */
    public String produceSoftSeam(List<ChatMessage> messages, int level,
                                   int startIdx, int endIdx) throws Exception {
        if (messages.isEmpty() || startIdx >= endIdx) return "";

        int actualEnd = Math.min(endIdx, messages.size());
        if (startIdx >= actualEnd) return "";

        List<ChatMessage> range = messages.subList(startIdx, actualEnd);

        // Use CompactionPlanner to identify which messages to keep verbatim
        var planner = new CompactionPlanner(
                com.jay.tui.core.compaction.CompactionConfig.defaultConfig());
        var plan = planner.planCompaction(new ArrayList<>(range));

        // Messages NOT pinned → to summarize
        var toSummarize = new ArrayList<ChatMessage>();
        for (int i = 0; i < range.size(); i++) {
            if (!plan.pinnedIndices().contains(i)) {
                toSummarize.add(range.get(i));
            }
        }

        if (toSummarize.isEmpty()) return "";

        String summary = summarizeMessages(toSummarize, level, startIdx, actualEnd);

        // Record this seam
        int tokenEstimate = summary.length() / 4;
        activeSeams.add(new SeamMetadata(
                (byte) level, startIdx, actualEnd,
                tokenEstimate, Instant.now(), config.seamModel()));

        return formatArchivedContext(level, startIdx, actualEnd,
                tokenEstimate, config.seamModel(), Instant.now(), summary);
    }

    /**
     * Recompact existing seams into a higher-level block.
     * Fuses prior {@code <archived_context>} content with new messages.
     */
    public String recompact(List<String> existingSeamTexts,
                             List<ChatMessage> newMessages,
                             int level, int startIdx, int endIdx) throws Exception {
        var sb = new StringBuilder();
        sb.append("## Prior Context Summaries\n\n");
        sb.append("The following <archived_context> blocks were produced earlier. ");
        sb.append("Merge their key information into a single denser summary.\n\n");

        for (int i = 0; i < existingSeamTexts.size(); i++) {
            sb.append("### Seam ").append(i + 1).append('\n');
            sb.append(existingSeamTexts.get(i)).append("\n\n");
        }

        if (!newMessages.isEmpty()) {
            sb.append("## Recent Messages\n\n");
            for (var msg : newMessages) {
                String roleLabel = "user".equals(msg.role()) ? "User" : "Assistant";
                if (msg.content() != null) {
                    String snippet = CompactionExecutor.truncateChars(
                            msg.content(), TEXT_SNIPPET_CHARS);
                    sb.append("**").append(roleLabel).append(":** ").append(snippet).append("\n\n");
                }
            }
        }

        int maxTokens = SeamConfig.maxTokensForLevel(level);
        int wordLimit = SeamConfig.wordLimitForLevel(level);
        String model = config.seamModel();

        var summaryMessages = List.of(
                ChatMessage.system("You are a context compaction specialist. "
                        + "Produce dense, factual summaries that preserve every decision, "
                        + "path, error, constraint, and open question. Drop conversational "
                        + "filler and repetition."),
                ChatMessage.user("Synthesize the following context into a single dense summary. "
                        + "Preserve: decisions made, file paths, error messages, "
                        + "constraints, hypotheses, open questions, and task state. "
                        + "Drop: greeting, filler, repeated information. "
                        + "Keep it under " + wordLimit + " words.\n\n" + sb)
        );

        String summary = callLlmWithRetry(model, summaryMessages, maxTokens);

        int tokenEstimate = summary.length() / 4;
        activeSeams.add(new SeamMetadata(
                (byte) level, startIdx, endIdx,
                tokenEstimate, Instant.now(), model));

        return formatArchivedContext(level, startIdx, endIdx,
                tokenEstimate, model, Instant.now(), summary);
    }

    /**
     * Collect text content of all active seams from messages.
     * Scans for {@code <archived_context>} blocks in assistant messages.
     */
    public static List<String> collectSeamTexts(List<ChatMessage> messages) {
        var texts = new ArrayList<String>();
        for (var msg : messages) {
            if ("assistant".equals(msg.role()) && msg.content() != null
                    && msg.content().contains("<archived_context")) {
                texts.add(msg.content());
            }
        }
        return texts;
    }

    // ── Internal ──────────────────────────────────────────────────────

    /**
     * Summarize a slice of messages using Flash model.
     */
    private String summarizeMessages(List<ChatMessage> messages, int level,
                                      int startIdx, int endIdx) throws Exception {
        var sb = new StringBuilder();

        for (var msg : messages) {
            String roleLabel = "user".equals(msg.role()) ? "User" : "Assistant";
            if ("tool".equals(msg.role())) {
                if (msg.content() != null) {
                    String snippet = CompactionExecutor.truncateChars(
                            msg.content(), TOOL_RESULT_SNIPPET_CHARS);
                    sb.append("Tool result: ").append(snippet).append("\n\n");
                }
                continue;
            }
            if (msg.toolCalls() != null && !msg.toolCalls().isEmpty()) {
                for (var tc : msg.toolCalls()) {
                    if (tc.function() != null && tc.function().name() != null) {
                        sb.append(roleLabel).append(": [Used tool: ")
                                .append(tc.function().name()).append("]\n\n");
                    }
                }
                continue;
            }
            if (msg.content() != null) {
                String snippet = CompactionExecutor.truncateChars(
                        msg.content(), TEXT_SNIPPET_CHARS);
                sb.append(roleLabel).append(": ").append(snippet).append("\n\n");
            }
        }

        int maxTokens = SeamConfig.maxTokensForLevel(level);
        int wordLimit = SeamConfig.wordLimitForLevel(level);
        String model = config.seamModel();

        var summaryMessages = List.of(
                ChatMessage.system("You are a context summarization specialist. "
                        + "Produce dense, factual summaries that preserve every decision, "
                        + "path, error, constraint, and open question. "
                        + "Never omit a file path, error message, or decision rationale."),
                ChatMessage.user("Summarize the following conversation segment "
                        + "(messages " + startIdx + "-" + endIdx + "). "
                        + "Preserve: key decisions and their rationale, exact file paths, "
                        + "command invocations, error messages, tool-result facts, "
                        + "constraints discovered, hypotheses being tested, "
                        + "and open questions. "
                        + "Drop: greetings, filler, repeated information, and thinking blocks. "
                        + "Keep it under " + wordLimit + " words.\n\n---\n\n" + sb)
        );

        return callLlmWithRetry(model, summaryMessages, maxTokens);
    }

    private String callLlmWithRetry(String model, List<ChatMessage> messages,
                                     int maxTokens) throws Exception {
        if (flashClient == null) {
            return "[Seam summary placeholder — Flash client not configured]";
        }

        Exception lastError = null;
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            if (attempt > 0) {
                long delay = BASE_DELAY_MS * (1L << (attempt - 1));
                try { Thread.sleep(delay); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
            try {
                var result = flashClient.chatBlocking(model, messages, 0.1, maxTokens);
                if (result.success() && result.content() != null) {
                    return result.content();
                }
                lastError = new Exception(result.error());
            } catch (Exception e) {
                lastError = e;
                if (!isTransientError(e)) break;
            }
        }

        throw new Exception("Seam summarization failed: "
                + (lastError != null ? lastError.getMessage() : "unknown"));
    }

    private static boolean isTransientError(Exception e) {
        if (e == null) return false;
        var envelope = ErrorTaxonomy.classify(e.getMessage());
        return ErrorTaxonomy.shouldRetry(envelope.category());
    }

    // ── XML formatting ────────────────────────────────────────────────

    public static String formatArchivedContext(int level, int startIdx, int endIdx,
                                         int tokenEstimate, String model,
                                         Instant timestamp, String summary) {
        return String.format("""
                <archived_context level="%d" range="msg %d-%d" tokens="~%d" \
                density="%s" model="%s" timestamp="%s">
                %s
                </archived_context>""",
                level, startIdx, endIdx, tokenEstimate,
                SeamConfig.densityLabel(level), model,
                timestamp.toString(), summary);
    }

    // ── Metadata record ───────────────────────────────────────────────

    /** Metadata for a single soft seam block. */
    public record SeamMetadata(
            int level,
            int startIdx,
            int endIdx,
            int tokenEstimate,
            Instant timestamp,
            String model
    ) {}
}
