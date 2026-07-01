package com.jay.tui.core.compaction;

import com.jay.tui.client.ChatMessage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Decides when and how to compact conversation history.
 * Mirrors Rust {@code plan_compaction} + {@code should_compact}.
 *
 * <h3>Strategy (v0.8.11+)</h3>
 * <ul>
 *   <li>Token-only trigger — no message-count heuristic</li>
 *   <li>Pin last {@value #KEEP_RECENT_MESSAGES} messages for immediate context</li>
 *   <li>Derive working-set paths from recent tool calls (read, write, edit, grep, glob)</li>
 *   <li>Pin messages referencing working-set files, errors, or patches</li>
 *   <li>Enforce tool-call/result pair completeness</li>
 *   <li>Guarantee at least one user text query in pinned set</li>
 *   <li>Subtract pinned tokens from threshold before deciding</li>
 *   <li>Require at least {@value #MIN_SUMMARIZE_MESSAGES} messages to summarize</li>
 * </ul>
 */
public class CompactionPlanner {

    static final int KEEP_RECENT_MESSAGES = 4;
    static final int RECENT_WORKING_SET_WINDOW = 12;
    static final int MAX_WORKING_SET_PATHS = 24;
    static final int MIN_SUMMARIZE_MESSAGES = 6;
    private static final int CHARS_PER_TOKEN = 4;

    // Tool names that reference filesystem paths
    private static final Set<String> PATH_TOOLS = Set.of(
            "read", "write_file", "edit_file", "grep", "glob",
            "read_file", "write", "edit", "apply_patch"
    );

    // Keywords indicating error messages worth pinning
    private static final String[] ERROR_MARKERS = {
            "error", "Error", "ERROR", "failed", "Failed", "FAILED",
            "exception", "Exception", "panic", "Panic", "traceback",
            "Traceback", "stack trace"
    };

    // Keywords indicating patch/diff messages worth pinning
    private static final String[] PATCH_MARKERS = {
            "```diff", "apply_patch", "*** delete file:", "patch",
            "diff --git", "--- a/", "+++ b/"
    };

    private final CompactionConfig config;
    private final Path workspace;

    public CompactionPlanner(CompactionConfig config) {
        this(config, Path.of("."));
    }

    public CompactionPlanner(CompactionConfig config, Path workspace) {
        this.config = config;
        this.workspace = workspace;
    }

    // ── Token estimation ──────────────────────────────────────────────

    /** Estimate total tokens across all messages. */
    public static int estimateTokens(List<ChatMessage> messages) {
        if (messages.isEmpty()) return 0;
        long totalChars = 0;
        for (var msg : messages) {
            totalChars += estimateCharsForMessage(msg);
        }
        return (int) (totalChars / CHARS_PER_TOKEN);
    }

    /** Conservative token estimate including framing overhead (×1.5). */
    public static int estimateTokensConservative(List<ChatMessage> messages) {
        int baseTokens = estimateTokens(messages);
        int framing = messages.size() * 12 + 48;
        return baseTokens * 3 / 2 + framing;
    }

    private static int estimateCharsForMessage(ChatMessage msg) {
        int chars = 0;
        if (msg.content() != null) chars += msg.content().length();
        if (msg.toolCalls() != null) {
            for (var tc : msg.toolCalls()) {
                if (tc.function() != null) {
                    if (tc.function().name() != null) chars += tc.function().name().length();
                    if (tc.function().arguments() != null) chars += tc.function().arguments().length();
                }
            }
        }
        return chars;
    }

    // ── Should compact? ───────────────────────────────────────────────

    /**
     * Whether compaction should be triggered.
     *
     * <p>Algorithm (matches Rust v0.8.11):
     * <ol>
     *   <li>If config disabled, return false</li>
     *   <li>Plan compaction → get pinned + summarize indices</li>
     *   <li>Subtract pinned tokens from threshold (pinned messages consume budget)</li>
     *   <li>Return true only if: summarize count ≥ MIN_SUMMARIZE_MESSAGES
     *       AND summarize tokens > effective threshold</li>
     *   <li>Edge case: if effective threshold is 0, trigger on message count alone</li>
     * </ol>
     */
    public boolean shouldCompact(List<ChatMessage> messages) {
        if (!config.enabled() || config.tokenThreshold() <= 0) {
            return false;
        }

        var plan = planCompaction(messages);

        // Pinned messages consume part of the budget
        int pinnedTokens = 0;
        for (int idx : plan.pinnedIndices()) {
            if (idx < messages.size()) {
                pinnedTokens += estimateTokensForMessage(messages.get(idx));
            }
        }

        int effectiveThreshold = Math.max(0, config.tokenThreshold() - pinnedTokens);
        int summarizeCount = plan.summarizeIndices().size();

        if (effectiveThreshold == 0) {
            return summarizeCount >= MIN_SUMMARIZE_MESSAGES;
        }
        if (summarizeCount < MIN_SUMMARIZE_MESSAGES) {
            return false;
        }

        int summarizeTokens = 0;
        for (int idx : plan.summarizeIndices()) {
            if (idx < messages.size()) {
                summarizeTokens += estimateTokensForMessage(messages.get(idx));
            }
        }
        return summarizeTokens > effectiveThreshold;
    }

    /** Estimate tokens for a single message (chars/4, rounded up). */
    private static int estimateTokensForMessage(ChatMessage msg) {
        int chars = estimateCharsForMessage(msg);
        return (chars + CHARS_PER_TOKEN - 1) / CHARS_PER_TOKEN;
    }

    // ── Compaction planning ───────────────────────────────────────────

    /**
     * Produce a compaction plan with smart pinning.
     *
     * <p>Pinning strategy:
     * <ul>
     *   <li>Always keep last {@value #KEEP_RECENT_MESSAGES} messages</li>
     *   <li>Derive working-set paths from recent tool calls</li>
     *   <li>Pin messages referencing those paths, errors, or patches</li>
     *   <li>Enforce tool-call/result pair completeness</li>
     *   <li>Ensure at least one user text query in pinned set</li>
     * </ul>
     */
    public CompactionPlan planCompaction(List<ChatMessage> messages) {
        return planCompaction(messages, null, null);
    }

    /**
     * Full compaction plan with optional external pins and working-set paths.
     *
     * @param externalPins additional indices to pin (e.g. from UI context)
     * @param externalWorkingSetPaths additional paths to treat as working-set
     */
    public CompactionPlan planCompaction(List<ChatMessage> messages,
                                          int[] externalPins,
                                          List<String> externalWorkingSetPaths) {
        int len = messages.size();
        if (len == 0) {
            return new CompactionPlan(List.of(), List.of(), "empty");
        }

        BitSet pinned = new BitSet(len);

        // 1. Always pin the tail of the conversation
        int recentStart = Math.max(0, len - KEEP_RECENT_MESSAGES);
        pinned.set(recentStart, len);

        // 2. Derive working-set paths from recent tool calls
        Set<String> workingSetPaths = deriveWorkingSetPaths(messages, externalPins);

        // Merge external working-set paths
        if (externalWorkingSetPaths != null) {
            for (String path : externalWorkingSetPaths) {
                String normalized = normalizePath(path);
                if (normalized != null && workingSetPaths.size() < MAX_WORKING_SET_PATHS) {
                    workingSetPaths.add(normalized);
                }
            }
        }

        // 3. Pin messages that reference working-set files, errors, or patches
        for (int i = 0; i < len; i++) {
            if (pinned.get(i)) continue;
            String text = messageText(messages.get(i));
            if (shouldPinMessage(text, workingSetPaths)) {
                pinned.set(i);
            }
        }

        // 4. External pins are authoritative
        if (externalPins != null) {
            for (int idx : externalPins) {
                if (idx >= 0 && idx < len) pinned.set(idx);
            }
        }

        // 5. Enforce tool-call/result pairs
        enforceToolCallPairs(messages, pinned);

        // 6. Ensure at least one user text query
        ensureUserTextQuery(messages, pinned);

        // Build index lists
        List<Integer> pinnedList = new ArrayList<>();
        List<Integer> summarizeList = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            if (pinned.get(i)) {
                pinnedList.add(i);
            } else {
                summarizeList.add(i);
            }
        }

        return new CompactionPlan(summarizeList, pinnedList,
                "Compacting " + summarizeList.size() + " messages, keeping "
                        + pinnedList.size() + " recent");
    }

    // ── Working-set path detection ────────────────────────────────────

    /**
     * Derive repo-aware working-set paths from recent messages and tool calls.
     * Mirrors Rust {@code derive_working_set_paths}.
     */
    private Set<String> deriveWorkingSetPaths(List<ChatMessage> messages, int[] externalPins) {
        Set<String> paths = new HashSet<>();
        int len = messages.size();

        // Seed from external pins
        if (externalPins != null) {
            for (int idx : externalPins) {
                if (idx >= 0 && idx < len) {
                    extractPaths(messageText(messages.get(idx)), paths);
                }
            }
        }

        // Scan recent messages for tool calls that reference paths
        int scanStart = Math.max(0, len - RECENT_WORKING_SET_WINDOW);
        for (int i = scanStart; i < len; i++) {
            var msg = messages.get(i);
            if (msg.toolCalls() != null) {
                for (var tc : msg.toolCalls()) {
                    if (tc.function() != null && tc.function().name() != null) {
                        String toolName = tc.function().name();
                        if (PATH_TOOLS.contains(toolName) && tc.function().arguments() != null) {
                            extractPathsFromJson(tc.function().arguments(), paths);
                        }
                    }
                }
            }
            // Also extract from content for inline paths
            if (paths.size() < MAX_WORKING_SET_PATHS) {
                extractPaths(messageText(msg), paths);
            }
        }

        return paths;
    }

    private void extractPaths(String text, Set<String> paths) {
        if (text == null || paths.size() >= MAX_WORKING_SET_PATHS) return;
        // Match common path patterns: relative paths, absolute paths, file references
        for (String word : text.split("[\\s,;:'\"()\\[\\]{}]+")) {
            if (paths.size() >= MAX_WORKING_SET_PATHS) break;
            String normalized = normalizePath(word);
            if (normalized != null) paths.add(normalized);
        }
    }

    /** Extract path fragments from tool call JSON arguments. */
    private void extractPathsFromJson(String jsonArgs, Set<String> paths) {
        if (jsonArgs == null || paths.size() >= MAX_WORKING_SET_PATHS) return;
        // Simple JSON path extraction: look for quoted strings that look like paths
        // Matches "path": "...", "file_path": "...", "pattern": "...", etc.
        extractPaths(jsonArgs, paths);
    }

    private String normalizePath(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String trimmed = raw.strip();
        if (trimmed.isEmpty()) return null;

        // Must contain a path separator or look like a file
        boolean hasSeparator = trimmed.contains("/")
                || trimmed.contains("\\")
                || trimmed.contains(".");
        boolean looksLikePath = hasSeparator
                && !trimmed.startsWith("http://")
                && !trimmed.startsWith("https://")
                && !trimmed.startsWith(".")
                && trimmed.length() > 1
                && trimmed.length() < 512;

        if (!looksLikePath) return null;
        return trimmed;
    }

    // ── Pin heuristics ────────────────────────────────────────────────

    /**
     * Whether a message should be pinned based on its content.
     * Pins messages that reference working-set files, contain errors, or show patches.
     */
    private static boolean shouldPinMessage(String text, Set<String> workingSetPaths) {
        if (text == null || text.isEmpty()) return false;

        // Check for working-set path references
        if (!workingSetPaths.isEmpty()) {
            for (String path : workingSetPaths) {
                if (text.contains(path)) return true;
            }
        }

        // Check for error markers
        String lower = text.toLowerCase();
        for (String marker : ERROR_MARKERS) {
            if (lower.contains(marker.toLowerCase())) return true;
        }

        // Check for patch markers
        for (String marker : PATCH_MARKERS) {
            if (lower.contains(marker.toLowerCase())) return true;
        }

        return false;
    }

    // ── Tool-call pair enforcement ────────────────────────────────────

    /**
     * Ensure tool result messages stay with their tool calls.
     * If a tool result is pinned, pin the corresponding tool call. Vice versa.
     * Uses fixpoint iteration to handle transitive pairs.
     */
    private static void enforceToolCallPairs(List<ChatMessage> messages, BitSet pinned) {
        if (messages.isEmpty()) return;

        // Build maps: tool_call_id → index, tool_result(tool_call_id) → index
        var callIdToIdx = new HashMap<String, Integer>();
        var resultIdToIdx = new HashMap<String, Integer>();

        for (int i = 0; i < messages.size(); i++) {
            var msg = messages.get(i);
            if (msg.toolCalls() != null) {
                for (var tc : msg.toolCalls()) {
                    if (tc.id() != null) callIdToIdx.put(tc.id(), i);
                }
            }
            if ("tool".equals(msg.role()) && msg.toolCallId() != null) {
                resultIdToIdx.put(msg.toolCallId(), i);
            }
        }

        // Fixpoint: re-check until stable
        int maxIters = Math.max(messages.size(), 10);
        Set<Integer> permanentlyRemoved = new HashSet<>();

        for (int iter = 0; iter < maxIters; iter++) {
            var toAdd = new ArrayList<Integer>();
            var toRemove = new ArrayList<Integer>();

            // For each pinned tool call, ensure its tool result is also pinned
            for (int i = 0; i < messages.size(); i++) {
                if (!pinned.get(i)) continue;

                var msg = messages.get(i);
                if (msg.toolCalls() != null) {
                    for (var tc : msg.toolCalls()) {
                        if (tc.id() != null) {
                            Integer resultIdx = resultIdToIdx.get(tc.id());
                            if (resultIdx != null && !pinned.get(resultIdx)
                                    && !permanentlyRemoved.contains(resultIdx)) {
                                toAdd.add(resultIdx);
                            }
                        }
                    }
                }
            }

            // For each pinned tool result, ensure its tool call is also pinned
            for (int i = 0; i < messages.size(); i++) {
                if (!pinned.get(i)) continue;

                var msg = messages.get(i);
                if ("tool".equals(msg.role()) && msg.toolCallId() != null) {
                    Integer callIdx = callIdToIdx.get(msg.toolCallId());
                    if (callIdx != null && !pinned.get(callIdx)
                            && !permanentlyRemoved.contains(callIdx)) {
                        toAdd.add(callIdx);
                    }
                }
            }

            // Remove orphans: pinned results without corresponding calls (and vice versa)
            for (int i = 0; i < messages.size(); i++) {
                if (!pinned.get(i)) continue;
                var msg = messages.get(i);
                if ("tool".equals(msg.role()) && msg.toolCallId() != null) {
                    Integer callIdx = callIdToIdx.get(msg.toolCallId());
                    if (callIdx == null || (!pinned.get(callIdx) && permanentlyRemoved.contains(callIdx))) {
                        toRemove.add(i);
                    }
                }
            }

            if (toAdd.isEmpty() && toRemove.isEmpty()) break;

            for (int idx : toAdd) pinned.set(idx);
            for (int idx : toRemove) {
                pinned.clear(idx);
                permanentlyRemoved.add(idx);
            }
        }
    }

    // ── User text query preservation ──────────────────────────────────

    /**
     * Ensure at least one user text query is in the pinned set.
     * Required by some OpenAI-compatible backends.
     */
    private static void ensureUserTextQuery(List<ChatMessage> messages, BitSet pinned) {
        // Check if any pinned message is a user text query
        boolean hasUserQuery = false;
        for (int i = 0; i < messages.size(); i++) {
            if (pinned.get(i) && isUserTextQuery(messages.get(i))) {
                hasUserQuery = true;
                break;
            }
        }
        if (hasUserQuery) return;

        // Find the last user text query and pin it
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (isUserTextQuery(messages.get(i))) {
                pinned.set(i);
                return;
            }
        }
    }

    private static boolean isUserTextQuery(ChatMessage msg) {
        return "user".equals(msg.role())
                && msg.content() != null
                && !msg.content().isEmpty()
                && (msg.toolCalls() == null || msg.toolCalls().isEmpty());
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /** Extract text content from a message for analysis. */
    private static String messageText(ChatMessage msg) {
        StringBuilder sb = new StringBuilder();
        if (msg.content() != null) sb.append(msg.content());
        if (msg.toolCalls() != null) {
            for (var tc : msg.toolCalls()) {
                if (tc.function() != null) {
                    if (tc.function().name() != null) sb.append(' ').append(tc.function().name());
                    if (tc.function().arguments() != null) {
                        sb.append(' ').append(tc.function().arguments());
                    }
                }
            }
        }
        return sb.toString();
    }

    // ── CompactionPlan record ─────────────────────────────────────────

    /** Result of compaction planning. */
    public record CompactionPlan(
            List<Integer> summarizeIndices,
            List<Integer> pinnedIndices,
            String description
    ) {
        public int messagesBefore() {
            return summarizeIndices.size() + pinnedIndices.size();
        }

        public int messagesAfter() {
            return pinnedIndices.size() + 1; // +1 for summary message
        }
    }
}
