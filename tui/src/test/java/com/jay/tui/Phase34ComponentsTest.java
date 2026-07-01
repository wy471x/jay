package com.jay.tui;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.jay.tui.cache.OutputRowsCache;
import com.jay.tui.cache.TranscriptViewCache;
import com.jay.tui.client.ChatMessage;
import com.jay.tui.client.ContentBlock;
import com.jay.tui.client.SseStreamEvent;
import com.jay.tui.core.ToolParser;
import com.jay.tui.core.StreamState;
import com.jay.tui.core.compaction.CompactionConfig;
import com.jay.tui.core.compaction.CompactionExecutor;
import com.jay.tui.core.compaction.CompactionPlanner;
import com.jay.tui.core.turn.ToolDispatcher;
import com.jay.tui.state.ScrollState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Phase34ComponentsTest {

    // ── CompactionConfig ────────────────────────────────────────────────

    @Nested
    @DisplayName("CompactionConfig")
    class CompactionConfigTests {

        @Test
        @DisplayName("default config: enabled, 800K threshold, cache summary")
        void defaultConfigMatchesSpec() {
            var cfg = CompactionConfig.defaultConfig();
            assertTrue(cfg.enabled());
            assertEquals(800_000, cfg.tokenThreshold());
            assertEquals("deepseek-v4-flash", cfg.model());
            assertTrue(cfg.cacheSummary());
        }

        @Test
        @DisplayName("disabled config: all false/zero")
        void disabledConfig() {
            var cfg = CompactionConfig.disabled();
            assertFalse(cfg.enabled());
            assertEquals(0, cfg.tokenThreshold());
            assertFalse(cfg.cacheSummary());
        }

        @Test
        @DisplayName("of() with 3 args defaults cacheSummary to true")
        void ofDefaultsCacheSummary() {
            var cfg = CompactionConfig.of(true, 4000, "gpt-5");
            assertTrue(cfg.enabled());
            assertEquals(4000, cfg.tokenThreshold());
            assertEquals("gpt-5", cfg.model());
            assertTrue(cfg.cacheSummary());
        }

        @Test
        @DisplayName("of() with 4 args respects cacheSummary")
        void ofWithCacheSummary() {
            var cfg = CompactionConfig.of(true, 5000, "claude", false);
            assertFalse(cfg.cacheSummary());
        }
    }

    // ── CompactionPlanner: Token Estimation ────────────────────────────

    @Nested
    @DisplayName("CompactionPlanner Token Estimation")
    class TokenEstimationTests {

        @Test
        void estimateTokensZeroForEmpty() {
            assertEquals(0, CompactionPlanner.estimateTokens(List.of()));
        }

        @Test
        void estimateTokensUsesCharsPer4() {
            var msgs = List.of(
                    ChatMessage.user("12345678"), // 8 chars
                    ChatMessage.assistant("abcd")  // 4 chars
            );
            assertEquals(3, CompactionPlanner.estimateTokens(msgs));
        }

        @Test
        void conservativeIncludesFramingOverhead() {
            var msgs = List.of(
                    ChatMessage.user("Hello world! This is a test message.")
            );
            int base = CompactionPlanner.estimateTokens(msgs);
            int conservative = CompactionPlanner.estimateTokensConservative(msgs);
            // Conservative should be higher due to framing overhead
            assertTrue(conservative > base,
                    "conservative=" + conservative + " should be > base=" + base);
        }

        @Test
        void toolCallArgumentsCountedInEstimate() {
            var msg = new ChatMessage("assistant", null,
                    List.of(new ChatMessage.ToolCall("id1", "function",
                            new ChatMessage.ToolCall.Function("read",
                                    "{\"path\":\"/tmp/test.txt\"}"))),
                    null);
            // tool call arguments: ~30 chars → ~7 tokens
            int tokens = CompactionPlanner.estimateTokens(List.of(msg));
            assertTrue(tokens > 0, "Tool call args should be counted");
        }
    }

    // ── CompactionPlanner: planCompaction ──────────────────────────────

    @Nested
    @DisplayName("CompactionPlanner planCompaction")
    class PlanCompactionTests {

        @Test
        void emptyMessagesReturnsEmptyPlan() {
            var planner = new CompactionPlanner(CompactionConfig.defaultConfig());
            var plan = planner.planCompaction(List.of());
            assertTrue(plan.pinnedIndices().isEmpty());
            assertTrue(plan.summarizeIndices().isEmpty());
        }

        @Test
        void keepRecentMessagesPinsLast4() {
            var messages = new ArrayList<ChatMessage>();
            for (int i = 0; i < 30; i++) {
                messages.add(ChatMessage.user("msg" + i));
            }
            var planner = new CompactionPlanner(CompactionConfig.defaultConfig());
            var plan = planner.planCompaction(messages);

            // 30 messages → 4 pinned (KEEP_RECENT=4), 26 to summarize
            assertEquals(26, plan.summarizeIndices().size());
            assertEquals(4, plan.pinnedIndices().size());
            assertEquals(30, plan.messagesBefore());
            assertEquals(5, plan.messagesAfter()); // 4 pinned + 1 summary

            // Pinned should be indices [26, 27, 28, 29]
            assertEquals(26, plan.pinnedIndices().get(0));
            assertEquals(29, plan.pinnedIndices().get(3));
        }

        @Test
        void smallConversationAllPinned() {
            var messages = List.of(
                    ChatMessage.user("Hello"),
                    ChatMessage.assistant("Hi there!")
            );
            var planner = new CompactionPlanner(CompactionConfig.defaultConfig());
            var plan = planner.planCompaction(messages);

            // 2 messages < KEEP_RECENT (4) → all pinned, none to summarize
            assertEquals(2, plan.pinnedIndices().size());
            assertEquals(0, plan.summarizeIndices().size());
        }

        @Test
        void toolCallPairsKeptTogether() {
            var messages = new ArrayList<ChatMessage>();
            // build conversation with tool call and result far apart
            for (int i = 0; i < 8; i++) {
                messages.add(ChatMessage.user("msg" + i));
            }
            // Assistant with tool call
            messages.add(new ChatMessage("assistant", "Let me read that",
                    List.of(new ChatMessage.ToolCall("call1", "function",
                            new ChatMessage.ToolCall.Function("read", "{\"path\":\"f\"}"))),
                    null));
            // Tool result
            messages.add(ChatMessage.tool("call1", "file contents here"));
            // More messages to push the index gap
            messages.add(ChatMessage.user("more"));
            messages.add(ChatMessage.assistant("done"));

            var planner = new CompactionPlanner(CompactionConfig.defaultConfig());
            var plan = planner.planCompaction(messages);

            // The tool call (index 8) and result (index 9) should both be pinned
            // or both summarized — enforced by tool-call pair logic
            boolean call8Pinned = plan.pinnedIndices().contains(8);
            boolean result9Pinned = plan.pinnedIndices().contains(9);
            assertEquals(call8Pinned, result9Pinned,
                    "Tool call and result must stay together");
        }

        @Test
        void userTextQueryPreserved() {
            var messages = new ArrayList<ChatMessage>();
            // All assistant/tool messages, then a user query at the very beginning
            messages.add(ChatMessage.user("important question"));
            for (int i = 0; i < 20; i++) {
                messages.add(ChatMessage.assistant("response " + i));
            }

            var planner = new CompactionPlanner(CompactionConfig.defaultConfig());
            var plan = planner.planCompaction(messages);

            // At least one user text query must be in pinned indices
            boolean hasUserQuery = plan.pinnedIndices().stream()
                    .anyMatch(idx -> messages.get(idx).role().equals("user"));
            assertTrue(hasUserQuery, "Must preserve at least one user text query");
        }

        @Test
        void externalPinsAreAuthoritative() {
            var messages = new ArrayList<ChatMessage>();
            for (int i = 0; i < 20; i++) {
                messages.add(ChatMessage.user("msg" + i));
            }
            var planner = new CompactionPlanner(CompactionConfig.defaultConfig());
            var plan = planner.planCompaction(messages, new int[]{5, 7}, null);

            // External pins 5 and 7 must be in the pinned set
            assertTrue(plan.pinnedIndices().contains(5),
                    "External pin 5 must be preserved");
            assertTrue(plan.pinnedIndices().contains(7),
                    "External pin 7 must be preserved");
        }

        @Test
        void errorMarkersPinMessages() {
            var messages = new ArrayList<ChatMessage>();
            for (int i = 0; i < 15; i++) {
                messages.add(ChatMessage.user("routine msg " + i));
            }
            // Message with error — should be pinned
            messages.add(ChatMessage.assistant("ERROR: NullPointerException at line 42"));

            var planner = new CompactionPlanner(CompactionConfig.defaultConfig());
            var plan = planner.planCompaction(messages);

            // The error message (index 15) should be pinned
            assertTrue(plan.pinnedIndices().contains(15),
                    "Error message should be pinned: " + plan.pinnedIndices());
        }

        @Test
        void patchMarkersPinMessages() {
            var messages = new ArrayList<ChatMessage>();
            for (int i = 0; i < 15; i++) {
                messages.add(ChatMessage.user("routine msg " + i));
            }
            messages.add(ChatMessage.assistant("Here's the fix:\n```diff\n--- a/file\n+++ b/file\n@@ -1 +1 @@\n-old\n+new\n```"));

            var planner = new CompactionPlanner(CompactionConfig.defaultConfig());
            var plan = planner.planCompaction(messages);

            assertTrue(plan.pinnedIndices().contains(15),
                    "Patch message should be pinned: " + plan.pinnedIndices());
        }

        @Test
        void workingSetPathsPinMessages() {
            var messages = new ArrayList<ChatMessage>();
            for (int i = 0; i < 10; i++) {
                messages.add(ChatMessage.user("routine msg " + i));
            }
            // Tool call that references a path
            messages.add(new ChatMessage("assistant", "Reading file",
                    List.of(new ChatMessage.ToolCall("c1", "function",
                            new ChatMessage.ToolCall.Function("read",
                                    "{\"path\":\"src/main/java/App.java\"}"))),
                    null));
            for (int i = 0; i < 10; i++) {
                messages.add(ChatMessage.user("more msg " + i));
            }
            // Later message referencing the same file
            messages.add(ChatMessage.assistant("The issue is in src/main/java/App.java"));

            var planner = new CompactionPlanner(CompactionConfig.defaultConfig());
            var plan = planner.planCompaction(messages);

            // The message referencing App.java should be pinned (it's in the working set)
            // At minimum, the one at index 21 that explicitly mentions the path
            boolean hasPathRef = plan.pinnedIndices().stream()
                    .anyMatch(idx -> {
                        String text = messages.get(idx).content();
                        return text != null && text.contains("App.java");
                    });
            assertTrue(hasPathRef, "Messages referencing working-set paths should be pinned");
        }
    }

    // ── CompactionPlanner: shouldCompact ──────────────────────────────

    @Nested
    @DisplayName("CompactionPlanner shouldCompact")
    class ShouldCompactTests {

        @Test
        void disabledConfigNeverCompacts() {
            var planner = new CompactionPlanner(CompactionConfig.disabled());
            assertFalse(planner.shouldCompact(List.of()));
        }

        @Test
        void emptyMessagesNeverCompact() {
            var config = CompactionConfig.of(true, 100, "model");
            var planner = new CompactionPlanner(config);
            assertFalse(planner.shouldCompact(List.of()));
        }

        @Test
        void belowMinSummarizeMessagesNeverCompacts() {
            var config = CompactionConfig.of(true, 10, "model");
            var planner = new CompactionPlanner(config);
            // Only 3 messages — below MIN_SUMMARIZE_MESSAGES (6)
            var messages = List.of(
                    ChatMessage.user("short"),
                    ChatMessage.assistant("ok"),
                    ChatMessage.user("done")
            );
            assertFalse(planner.shouldCompact(messages));
        }

        @Test
        void triggersAboveThresholdWithEnoughMessages() {
            var config = CompactionConfig.of(true, 20, "model");
            var planner = new CompactionPlanner(config);
            // 15 messages → 4 pinned, 11 to summarize — all above threshold
            var messages = new ArrayList<ChatMessage>();
            for (int i = 0; i < 15; i++) {
                messages.add(ChatMessage.user(
                        "This is a long message with enough text to push past token limit " + i));
            }
            assertTrue(planner.shouldCompact(messages));
        }

        @Test
        void pinnedTokensReduceEffectiveThreshold() {
            // Set threshold very low, but pinned messages consume most of the budget
            var config = CompactionConfig.of(true, 50, "model");
            var planner = new CompactionPlanner(config);
            // 20 messages: last 4 pinned (KEEP_RECENT), so 16 to summarize
            var messages = new ArrayList<ChatMessage>();
            for (int i = 0; i < 20; i++) {
                messages.add(ChatMessage.assistant("tiny"));  // 4 chars each → 1 token
            }
            // Each message = ~1 token, 16 summarize = 16 tokens
            // Pinned = ~4 tokens, effective threshold = 50 - 4 = 46
            // 16 > 46? No → should NOT compact
            assertFalse(planner.shouldCompact(messages));

            // Now make messages big enough
            var bigMessages = new ArrayList<ChatMessage>();
            for (int i = 0; i < 20; i++) {
                bigMessages.add(ChatMessage.user("A".repeat(200)));  // 200 chars → 50 tokens
            }
            // Pinned = 4 * 50 = 200, effective threshold = 50 - 200 = 0
            // Effective=0 → check message count only: 16 >= 6 → true
            assertTrue(planner.shouldCompact(bigMessages));
        }
    }

    // ── CompactionExecutor: Truncation Helpers ─────────────────────────

    @Nested
    @DisplayName("CompactionExecutor Truncation")
    class TruncationTests {

        @Test
        void truncateCharsReturnsFullIfUnderLimit() {
            assertEquals("hello", CompactionExecutor.truncateChars("hello", 10));
        }

        @Test
        void truncateCharsCutsAtLimit() {
            assertEquals("hello", CompactionExecutor.truncateChars("hello world", 5));
        }

        @Test
        void truncateCharsNullReturnsEmpty() {
            assertEquals("", CompactionExecutor.truncateChars(null, 5));
        }

        @Test
        void tailCharsReturnsFullIfUnderLimit() {
            assertEquals("world", CompactionExecutor.tailChars("world", 10));
        }

        @Test
        void tailCharsTakesLastN() {
            assertEquals("world", CompactionExecutor.tailChars("hello world", 5));
        }

        @Test
        void headTailTruncateInsertsMarker() {
            String input = "A".repeat(100);
            String result = CompactionExecutor.headTailTruncate(input, 20, 20);
            assertTrue(result.contains("truncated"),
                    "Should contain truncation marker");
            assertTrue(result.startsWith("A"), "Should start with head chars");
            assertTrue(result.endsWith("A"), "Should end with tail chars");
        }

        @Test
        void headTailTruncateShortInputUnchanged() {
            String input = "short";
            String result = CompactionExecutor.headTailTruncate(input, 10, 10);
            assertEquals("short", result);
        }
    }

    // ── CompactionExecutor: Transcript Building ────────────────────────

    @Nested
    @DisplayName("CompactionExecutor Transcript Building")
    class TranscriptTests {

        @Test
        void buildFormattedTranscriptIncludesRoles() {
            var messages = List.of(
                    ChatMessage.user("Hi"),
                    ChatMessage.assistant("Hello!"),
                    ChatMessage.system("be helpful")
            );
            String t = CompactionExecutor.buildFormattedTranscript(messages);
            assertTrue(t.contains("User:"));
            assertTrue(t.contains("Assistant:"));
            assertTrue(t.contains("System:"));
        }

        @Test
        void buildFormattedTranscriptTruncatesLongMessages() {
            var messages = List.of(
                    ChatMessage.user("A".repeat(2000))  // > SUMMARY_TEXT_SNIPPET_CHARS (800)
            );
            String t = CompactionExecutor.buildFormattedTranscript(messages);
            assertTrue(t.length() < 2000,
                    "Long messages should be truncated, got " + t.length());
        }

        @Test
        void buildFormattedTranscriptHeadTailForHugeInput() {
            var messages = new ArrayList<ChatMessage>();
            for (int i = 0; i < 100; i++) {
                messages.add(ChatMessage.user("Message " + i + ": " + "X".repeat(500)));
            }
            String t = CompactionExecutor.buildFormattedTranscript(messages);
            // Should be head-tail truncated at SUMMARY_INPUT_MAX_CHARS
            assertTrue(t.length() < 50_000,
                    "Huge input should be truncated, got " + t.length());
        }
    }

    // ── CompactionExecutor: Tool Result Pruning ────────────────────────

    @Nested
    @DisplayName("CompactionExecutor Tool Result Pruning")
    class PruneToolResultsTests {

        @Test
        void pruneTruncatesLargeToolResults() {
            var executor = new CompactionExecutor(null,
                    CompactionConfig.defaultConfig(), null);
            var messages = new ArrayList<ChatMessage>();
            messages.add(ChatMessage.user("hi"));
            messages.add(ChatMessage.tool("t1", "A".repeat(20_000))); // > 12K limit
            messages.add(ChatMessage.assistant("ok"));

            int saved = executor.pruneToolResultsUntil(messages);
            assertTrue(saved > 0, "Should save bytes: " + saved);

            String toolContent = messages.get(1).content();
            assertTrue(toolContent.length() < 20_000,
                    "Tool result should be truncated, got " + toolContent.length());
            assertTrue(toolContent.contains("truncated"),
                    "Should contain truncation marker");
        }

        @Test
        void pruneSkipsSmallToolResults() {
            var executor = new CompactionExecutor(null,
                    CompactionConfig.defaultConfig(), null);
            var messages = new ArrayList<ChatMessage>();
            messages.add(ChatMessage.tool("t1", "short result"));

            int saved = executor.pruneToolResultsUntil(messages);
            assertEquals(0, saved, "Small results should not be pruned");
        }

        @Test
        void pruneSkipsNonToolMessages() {
            var executor = new CompactionExecutor(null,
                    CompactionConfig.defaultConfig(), null);
            var messages = new ArrayList<ChatMessage>();
            messages.add(ChatMessage.user("A".repeat(20_000))); // user msg, not tool

            int saved = executor.pruneToolResultsUntil(messages);
            assertEquals(0, saved, "Non-tool messages should not be pruned");
        }
    }

    // ── ToolParser ─────────────────────────────────────────────────────

    @Nested
    class ToolParserTests {

        @Test
        void parseValidJson() {
            var result = ToolParser.parseToolInput("{\"path\":\"/tmp\"}");
            assertEquals("/tmp", result.get("path").asText());
        }

        @Test
        void parseEmptyReturnsEmptyObject() {
            var result = ToolParser.parseToolInput("");
            assertEquals(0, result.size());
        }

        @Test
        void parseNullReturnsEmptyObject() {
            var result = ToolParser.parseToolInput(null);
            assertEquals(0, result.size());
        }

        @Test
        void stripCodeFences() {
            var result = ToolParser.parseToolInput(
                    "```json\n{\"key\": \"value\"}\n```");
            assertEquals("value", result.get("key").asText());
        }

        @Test
        void extractJsonObjectFromText() {
            var result = ToolParser.parseToolInput(
                    "Some text {\"x\": 42} trailing");
            assertEquals(42, result.get("x").asInt());
        }

        @Test
        void fallbackWrapsRawInRawField() {
            var result = ToolParser.parseToolInput("not json at all");
            assertTrue(result.has("_raw"));
            assertEquals("not json at all", result.get("_raw").asText());
        }
    }

    // ── StreamState ────────────────────────────────────────────────────

    @Nested
    class StreamStateTests {

        @Test
        void appendTextAccumulates() {
            var ss = new StreamState();
            ss.startBlock(ContentBlock.Kind.TEXT, 0);
            ss.appendText("Hello");
            ss.appendText(" World");
            assertEquals("Hello World", ss.currentText());
        }

        @Test
        void appendThinkingAccumulates() {
            var ss = new StreamState();
            ss.startBlock(ContentBlock.Kind.THINKING, 0);
            ss.appendThinking("hmm");
            assertTrue(ss.anyContentReceived());
        }

        @Test
        void anyContentReceivedFlag() {
            var ss = new StreamState();
            assertFalse(ss.anyContentReceived());
            ss.startBlock(ContentBlock.Kind.TEXT, 0);
            ss.appendText("x");
            assertTrue(ss.anyContentReceived());
        }

        @Test
        void buildContentBlocksReturnsText() {
            var ss = new StreamState();
            ss.startBlock(ContentBlock.Kind.TEXT, 0);
            ss.appendText("response");
            var blocks = ss.buildContentBlocks();
            assertEquals(1, blocks.size());
            var text = (ContentBlock.Text) blocks.get(0);
            assertEquals("response", text.content());
        }

        @Test
        void containsFakeWrapperDetectsMarkers() {
            assertTrue(StreamState.containsFakeWrapper("[TOOL_CALL]"));
            assertTrue(StreamState.containsFakeWrapper("<tool_call"));
            assertFalse(StreamState.containsFakeWrapper("plain text"));
        }

        @Test
        void filterToolCallDeltaStripsFakeWrappers() {
            String filtered = StreamState.filterToolCallDelta(
                    "before[TOOL_CALL]after", false);
            assertEquals("before", filtered);
        }

        @Test
        void resetClearsAllState() {
            var ss = new StreamState();
            ss.startBlock(ContentBlock.Kind.TEXT, 0);
            ss.appendText("hello");
            ss.reset();
            assertEquals("", ss.currentText());
            assertEquals(ContentBlock.Kind.NONE, ss.currentKind());
            assertFalse(ss.anyContentReceived());
        }
    }

    // ── ToolDispatcher ─────────────────────────────────────────────────

    @Nested
    class ToolDispatcherTests {

        private final ToolDispatcher dispatcher = new ToolDispatcher(
                new java.util.concurrent.LinkedBlockingQueue<>());

        @Test
        void planSingleToolReturnsSerialBatch() {
            var toolUses = List.of(new ContentBlock.ToolUse(
                    "id1", "bash", JsonNodeFactory.instance.objectNode()));
            var batches = dispatcher.planBatches(toolUses);
            assertEquals(1, batches.size());
            assertInstanceOf(ToolDispatcher.ToolBatch.Serial.class, batches.get(0));
        }

        @Test
        void planReadOnlyToolsReturnParallelBatch() {
            var toolUses = List.of(
                    new ContentBlock.ToolUse("id1", "read",
                            JsonNodeFactory.instance.objectNode()),
                    new ContentBlock.ToolUse("id2", "grep",
                            JsonNodeFactory.instance.objectNode())
            );
            var batches = dispatcher.planBatches(toolUses);
            assertEquals(1, batches.size());
            assertInstanceOf(ToolDispatcher.ToolBatch.Parallel.class, batches.get(0));
            var parallel = (ToolDispatcher.ToolBatch.Parallel) batches.get(0);
            assertEquals(2, parallel.plans().size());
        }

        @Test
        void executeToolReturnsSuccess() {
            var plan = new ToolDispatcher.ToolExecutionPlan(
                    "id1", "read", JsonNodeFactory.instance.objectNode());
            var result = dispatcher.executeTool(plan);
            assertTrue(result.success());
            assertEquals("id1", result.id());
        }
    }

    // ── ScrollState ────────────────────────────────────────────────────

    @Nested
    class ScrollStateTests {

        @Test
        void toBottomCreatesTailSentinel() {
            var scroll = ScrollState.toBottom();
            assertTrue(scroll.isAtTail());
        }

        @Test
        void scrolledUpDisablesTail() {
            var scroll = ScrollState.toBottom();
            var result = scroll.scrolledBy(-3, 100, 20);
            assertFalse(result.isAtTail());
        }

        @Test
        void scrolledToBottomReenablesTail() {
            var scroll = ScrollState.at(10);
            var result = scroll.scrolledBy(1000, 100, 20);
            assertTrue(result.isAtTail());
        }

        @Test
        void resolveAtTailGivesMaxStart() {
            var scroll = ScrollState.toBottom();
            var resolved = scroll.resolve(100, 20);
            assertEquals(80, resolved.effectiveOffset()); // 100-20
            assertTrue(resolved.state().isAtTail());
        }

        @Test
        void anchorAtSpecificLine() {
            var scroll = ScrollState.at(42);
            assertEquals(42, scroll.offset());
            assertFalse(scroll.isAtTail());
        }

        @Test
        void scrollToTop() {
            var scroll = ScrollState.toBottom();
            var result = scroll.scrollToTop();
            assertEquals(0, result.offset());
            assertFalse(result.isAtTail());
        }
    }

    // ── OutputRowsCache ────────────────────────────────────────────────

    @Nested
    class OutputRowsCacheTests {

        @Test
        void getNonExistentReturnsEmpty() {
            var cache = new OutputRowsCache(10);
            assertTrue(cache.get("hash1", 80).isEmpty());
        }

        @Test
        void putAndGet() {
            var cache = new OutputRowsCache(10);
            cache.put("hash1", 80, List.of("line1", "line2"));
            var result = cache.get("hash1", 80);
            assertTrue(result.isPresent());
            assertEquals(2, result.get().size());
        }

        @Test
        void differentWidthDifferentEntry() {
            var cache = new OutputRowsCache(10);
            cache.put("hash1", 80, List.of("wide"));
            assertTrue(cache.get("hash1", 40).isEmpty());
        }

        @Test
        void evictionWhenFull() {
            var cache = new OutputRowsCache(3);
            cache.put("a", 80, List.of("a"));
            cache.put("b", 80, List.of("b"));
            cache.put("c", 80, List.of("c"));
            cache.put("d", 80, List.of("d")); // evicts "a"
            assertEquals(3, cache.size());
            assertTrue(cache.get("a", 80).isEmpty());
        }

        @Test
        void contentHashStable() {
            String h1 = OutputRowsCache.contentHash("hello");
            String h2 = OutputRowsCache.contentHash("hello");
            assertEquals(h1, h2);
            assertNotEquals(h1, OutputRowsCache.contentHash("world"));
        }
    }

    // ── TranscriptViewCache ────────────────────────────────────────────

    @Nested
    class TranscriptViewCacheTests {

        @Test
        void emptyInitially() {
            var cache = new TranscriptViewCache();
            assertEquals(0, cache.totalLines());
        }

        @Test
        void ensureRendersCells() {
            var cache = new TranscriptViewCache();
            var cells = List.of("hello", "world");
            var revisions = List.of(1L, 1L);

            cache.ensure(cells, revisions, 80, new HashSet<>());
            assertTrue(cache.totalLines() > 0);
            assertTrue(cache.lines().stream().anyMatch(l -> l.contains("hello")));
        }

        @Test
        void cacheHitReusesLines() {
            var cache = new TranscriptViewCache();
            var cells = List.of("cell1", "cell2");
            var revisions = List.of(1L, 1L);

            // First render
            cache.ensure(cells, revisions, 80, new HashSet<>());
            int lines1 = cache.totalLines();

            // Second render — same revisions, should reuse
            cache.ensure(cells, revisions, 80, new HashSet<>());
            assertEquals(lines1, cache.totalLines());
        }

        @Test
        void revisionChangeTriggersRerender() {
            var cache = new TranscriptViewCache();
            var cells = List.of("old content");
            var revisions = new ArrayList<>(List.of(1L));

            cache.ensure(cells, revisions, 80, new HashSet<>());

            // Change revision
            revisions.set(0, 2L);
            cache.ensure(List.of("new content"), revisions, 80, new HashSet<>());
            assertTrue(cache.lines().stream().anyMatch(l -> l.contains("new content")));
        }

        @Test
        void widthChangeInvalidatesAll() {
            var cache = new TranscriptViewCache();
            var cells = List.of("text");
            var revisions = List.of(1L);

            cache.ensure(cells, revisions, 80, new HashSet<>());
            int count80 = cache.totalLines();

            // Different width forces re-render
            cache.ensure(cells, revisions, 40, new HashSet<>());
            // Lines should be different (wider = fewer lines for same text)
            assertTrue(cache.totalLines() > 0);
        }

        @Test
        void invalidateClearsEverything() {
            var cache = new TranscriptViewCache();
            cache.ensure(List.of("a"), List.of(1L), 80, new HashSet<>());
            assertTrue(cache.totalLines() > 0);
            cache.invalidate();
            assertEquals(0, cache.totalLines());
        }
    }

    // ── SseStreamEvent Parsing ─────────────────────────────────────────

    @Nested
    class SseStreamEventTests {

        @Test
        void parseNullReturnsNull() {
            assertNull(SseStreamEvent.parseData(null));
        }

        @Test
        void parseDoneReturnsNull() {
            assertNull(SseStreamEvent.parseData("[DONE]"));
        }

        @Test
        void parseContentDelta() {
            String json = """
                    {"choices":[{"index":0,"delta":{"content":"Hello"}}]}""";
            var event = SseStreamEvent.parseData(json);
            assertNotNull(event);
            assertInstanceOf(SseStreamEvent.ContentBlockDelta.class, event);
            var delta = (SseStreamEvent.ContentBlockDelta) event;
            assertInstanceOf(SseStreamEvent.Delta.TextDelta.class, delta.delta());
        }

        @Test
        void parseThinkingDelta() {
            String json = """
                    {"choices":[{"index":0,"delta":{"reasoning_content":"hmm"}}]}""";
            var event = SseStreamEvent.parseData(json);
            assertInstanceOf(SseStreamEvent.ContentBlockDelta.class, event);
            var delta = (SseStreamEvent.ContentBlockDelta) event;
            assertInstanceOf(SseStreamEvent.Delta.ThinkingDelta.class, delta.delta());
        }

        @Test
        void parseError() {
            String json = """
                    {"error":{"message":"rate limited","code":"429"}}""";
            var event = SseStreamEvent.parseData(json);
            assertInstanceOf(SseStreamEvent.StreamError.class, event);
            assertTrue(((SseStreamEvent.StreamError) event).retryable());
        }
    }
}
