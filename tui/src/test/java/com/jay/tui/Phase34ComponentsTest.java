package com.jay.tui;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.jay.tui.cache.OutputRowsCache;
import com.jay.tui.cache.TranscriptViewCache;
import com.jay.tui.client.ContentBlock;
import com.jay.tui.client.SseStreamEvent;
import com.jay.tui.core.ToolParser;
import com.jay.tui.core.StreamState;
import com.jay.tui.core.compaction.CompactionConfig;
import com.jay.tui.core.compaction.CompactionPlanner;
import com.jay.tui.core.turn.ToolDispatcher;
import com.jay.tui.state.ScrollState;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Phase34ComponentsTest {

    // ── CompactionPlanner ───────────────────────────────────────────────

    @Nested
    class CompactionTests {

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
        void estimateTokensZeroForEmpty() {
            assertEquals(0, CompactionPlanner.estimateTokens(List.of()));
        }

        @Test
        void estimateTokensUsesCharsPer4() {
            var msgs = List.of(
                    com.jay.tui.client.ChatMessage.user("12345678"), // 8 chars
                    com.jay.tui.client.ChatMessage.assistant("abcd")  // 4 chars
            );
            // 12 chars / 4 = 3 tokens
            assertEquals(3, CompactionPlanner.estimateTokens(msgs));
        }

        @Test
        void planPinsRecentMessages() {
            var messages = new ArrayList<com.jay.tui.client.ChatMessage>();
            for (int i = 0; i < 30; i++) {
                messages.add(com.jay.tui.client.ChatMessage.user("msg" + i));
            }

            var config = CompactionConfig.of(true, 1000, "model");
            var planner = new CompactionPlanner(config);
            var plan = planner.plan(messages);

            // First 20 should be summarized (30 - 10 pinned)
            assertEquals(20, plan.toSummarizeIndices().size());
            // Last 10 should be pinned
            assertEquals(10, plan.pinnedIndices().size());
            assertEquals(30, plan.messagesBefore());
            assertEquals(11, plan.messagesAfter()); // 10 pinned + 1 summary
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
