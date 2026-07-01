package com.jay.tui;

import com.jay.tui.client.ChatMessage;
import com.jay.tui.client.ContentBlock;
import com.jay.tui.core.*;
import com.jay.tui.core.compaction.CompactionConfig;
import com.jay.tui.core.compaction.CompactionPlanner;
import com.jay.tui.core.turn.SseTurnLoop;
import com.jay.tui.core.turn.ToolDispatcher;
import com.jay.tui.state.ScrollState;
import org.junit.jupiter.api.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full logic pipeline tests — exercises the engine, session, SSE turn loop,
 * and tool dispatch WITHOUT needing a terminal.
 *
 * <p>Runs as plain JUnit tests, stdout is fully available for debugging.
 * Uses {@code System.out.println()} freely for visibility into the pipeline.
 */
@DisplayName("Logic Pipeline")
class LogicPipelineTest {

    // ═══════════════════════════════════════════════════════════════════
    // 1. Full Engine Lifecycle (Spawn → Send → Drain → Shutdown)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Engine Lifecycle")
    class EngineLifecycle {

        @Test
        @DisplayName("spawn emits EngineInitialized, shutdown stops engine")
        void spawnEmitsInitializedAndShutdownWorks() throws Exception {
            System.out.println("=== TEST: Engine spawn & shutdown ===");

            EngineConfig config = EngineConfig.builder()
                    .model("deepseek-v4-pro")
                    .workspace(Path.of("/tmp/test-workspace"))
                    .build();

            EngineHandle handle = TuiEngine.spawn(config);
            assertTrue(handle.isRunning(), "Engine should be running after spawn");

            // Drain the EngineInitialized event
            List<TuiEngineEvent> events = drainWithWait(handle, 500);
            printEvents("Initial", events);
            assertTrue(events.stream().anyMatch(e -> e instanceof TuiEngineEvent.EngineInitialized),
                    "Should emit EngineInitialized on spawn");

            // Shutdown
            handle.shutdown();
            Thread.sleep(300);
            assertFalse(handle.isRunning(), "Engine should stop after shutdown");
            System.out.println("=== PASS: spawn & shutdown ===\n");
        }

        @Test
        @DisplayName("SendMessage flows through full turn lifecycle")
        void sendMessageFlow() throws Exception {
            System.out.println("=== TEST: SendMessage turn lifecycle ===");

            EngineConfig config = EngineConfig.builder()
                    .model("deepseek-v4-pro").workspace(Path.of(".")).build();
            EngineHandle handle = TuiEngine.spawn(config);
            drainWithWait(handle, 200); // clear init event

            // Send a message
            handle.sendOp(new TuiEngineOp.SendMessage(
                    "Hello, world!", "agent", "deepseek-v4-pro",
                    true, false, false, false, List.of()));

            // Let the engine process
            List<TuiEngineEvent> events = drainWithWait(handle, 500);
            printEvents("SendMessage", events);

            // Verify the full turn lifecycle
            boolean hasTurnStarted = events.stream()
                    .anyMatch(e -> e instanceof TuiEngineEvent.TurnStarted);
            boolean hasMessageDelta = events.stream()
                    .anyMatch(e -> e instanceof TuiEngineEvent.MessageDelta);
            boolean hasMessageComplete = events.stream()
                    .anyMatch(e -> e instanceof TuiEngineEvent.MessageComplete);
            boolean hasTurnComplete = events.stream()
                    .anyMatch(e -> e instanceof TuiEngineEvent.TurnComplete);

            System.out.printf("  TurnStarted: %s%n", hasTurnStarted);
            System.out.printf("  MessageDelta: %s%n", hasMessageDelta);
            System.out.printf("  MessageComplete: %s%n", hasMessageComplete);
            System.out.printf("  TurnComplete: %s%n", hasTurnComplete);

            assertTrue(hasTurnStarted, "Should emit TurnStarted");
            assertTrue(hasMessageDelta, "Should emit MessageDelta");
            assertTrue(hasTurnComplete, "Should emit TurnComplete");

            handle.shutdown();
            System.out.println("=== PASS: SendMessage lifecycle ===\n");
        }

        @Test
        @DisplayName("multiple messages accumulate in session")
        void multipleMessagesAccumulate() throws Exception {
            System.out.println("=== TEST: Multiple messages ===");

            var config = EngineConfig.builder().workspace(Path.of(".")).build();
            EngineHandle handle = TuiEngine.spawn(config);
            drainWithWait(handle, 200);

            for (int i = 1; i <= 3; i++) {
                handle.sendOp(new TuiEngineOp.SendMessage(
                        "message " + i, "agent", "m",
                        true, false, false, false, List.of()));
                Thread.sleep(200);
                var events = handle.drainEvents();
                long completes = events.stream()
                        .filter(e -> e instanceof TuiEngineEvent.TurnComplete).count();
                System.out.printf("  Message %d: %d TurnComplete events%n", i, completes);
                assertEquals(1, completes, "Each send should produce one TurnComplete");
            }

            handle.shutdown();
            System.out.println("=== PASS: Multiple messages ===\n");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 2. Cancellation & Error Handling
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Cancellation & Errors")
    class CancellationAndErrors {

        @Test
        @DisplayName("cancel flag is respected")
        void cancelFlag() throws Exception {
            System.out.println("=== TEST: Cancel flag ===");

            var config = EngineConfig.builder().workspace(Path.of(".")).build();
            EngineHandle handle = TuiEngine.spawn(config);
            drainWithWait(handle, 200);

            assertFalse(handle.isCancelled());
            handle.cancel();
            assertTrue(handle.isCancelled());
            handle.resetCancel();
            assertFalse(handle.isCancelled());

            handle.shutdown();
            System.out.println("=== PASS: Cancel flag ===\n");
        }

        @Test
        @DisplayName("error taxonomy classifies errors correctly")
        void errorTaxonomy() {
            System.out.println("=== TEST: Error taxonomy ===");

            // Network errors — recoverable
            var net = ErrorTaxonomy.classify("Connection refused");
            assertEquals(ErrorTaxonomy.Category.NETWORK, net.category());
            assertTrue(net.recoverable());
            System.out.printf("  'Connection refused' → %s (recoverable=%s)%n",
                    net.category(), net.recoverable());

            // Timeout — recoverable
            var timeout = ErrorTaxonomy.classify("Request timed out after 30s");
            assertEquals(ErrorTaxonomy.Category.TIMEOUT, timeout.category());
            assertTrue(timeout.recoverable());

            // Rate limit — recoverable
            var rate = ErrorTaxonomy.classify("429 Too Many Requests: rate limit exceeded");
            assertEquals(ErrorTaxonomy.Category.RATE_LIMIT, rate.category());
            assertTrue(rate.recoverable());

            // Auth — NOT recoverable
            var auth = ErrorTaxonomy.classify("401 Unauthorized: invalid API key");
            assertEquals(ErrorTaxonomy.Category.AUTH, auth.category());
            assertFalse(auth.recoverable());

            // Parse — NOT recoverable
            var parse = ErrorTaxonomy.classify("JSON parse error: malformed response");
            assertEquals(ErrorTaxonomy.Category.PARSE, parse.category());
            assertFalse(parse.recoverable());

            // Default fallback
            var unknown = ErrorTaxonomy.classify("something strange happened");
            assertEquals(ErrorTaxonomy.Category.INTERNAL, unknown.category());
            assertFalse(unknown.recoverable());

            System.out.println("=== PASS: Error taxonomy ===\n");
        }

        @Test
        @DisplayName("error taxonomy null handling")
        void errorTaxonomyNull() {
            var result = ErrorTaxonomy.classify(null);
            assertEquals(ErrorTaxonomy.Category.INTERNAL, result.category());
            assertFalse(result.recoverable());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 3. SseTurnLoop Integration
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("SSE Turn Loop")
    class SseTurnLoopTests {

        @Test
        @DisplayName("turn loop completes with placeholder response")
        void turnLoopCompletes() throws Exception {
            System.out.println("=== TEST: SSE turn loop execution ===");

            BlockingQueue<TuiEngineEvent> eventQueue = new LinkedBlockingQueue<>();
            AtomicBoolean cancelToken = new AtomicBoolean(false);
            var compactionConfig = CompactionConfig.disabled();
            List<ChatMessage> messages = new ArrayList<>();

            var loop = new SseTurnLoop(eventQueue, cancelToken,
                    compactionConfig, messages);
            var turn = new TurnContext(10);

            int steps = loop.executeTurn(turn, "What is 2+2?");
            System.out.printf("  Turn completed in %d steps%n", steps);

            // Drain events from the loop
            List<TuiEngineEvent> events = new ArrayList<>();
            eventQueue.drainTo(events);
            printEvents("SseTurnLoop", events);

            assertTrue(events.stream().anyMatch(e -> e instanceof TuiEngineEvent.TurnStarted
                    || e instanceof TuiEngineEvent.MessageStarted
                    || e instanceof TuiEngineEvent.TurnComplete),
                    "Should produce turn lifecycle events");

            // Session should have at least 2 messages (user + assistant)
            assertTrue(messages.size() >= 2,
                    "Session should have user + assistant messages, got " + messages.size());
            messages.forEach(m -> System.out.printf("  [%s] %s%n",
                    m.role(), m.content() != null ? m.content().substring(0,
                            Math.min(50, m.content().length())) : "(null)"));

            System.out.println("=== PASS: SSE turn loop ===\n");
        }

        @Test
        @DisplayName("turn loop with compaction check does not crash")
        void compactionCheckDuringTurn() throws Exception {
            System.out.println("=== TEST: Compaction check during turn ===");

            BlockingQueue<TuiEngineEvent> eventQueue = new LinkedBlockingQueue<>();
            AtomicBoolean cancelToken = new AtomicBoolean(false);
            var compactionConfig = CompactionConfig.of(true, 100, "model"); // low threshold
            List<ChatMessage> messages = new ArrayList<>();

            // Add many messages to trigger compaction
            for (int i = 0; i < 50; i++) {
                messages.add(ChatMessage.user("This is a fairly long message number "
                        + i + " with some extra padding to increase token count "
                        + "and trigger the compaction threshold during the turn."));
                messages.add(ChatMessage.assistant("Response " + i));
            }

            var loop = new SseTurnLoop(eventQueue, cancelToken,
                    compactionConfig, messages);
            var turn = new TurnContext(10);

            assertDoesNotThrow(() -> loop.executeTurn(turn, "Final question?"));
            System.out.printf("  Turn completed, session has %d messages%n", messages.size());

            List<TuiEngineEvent> events = new ArrayList<>();
            eventQueue.drainTo(events);
            boolean hasCompactionEvent = events.stream()
                    .anyMatch(e -> e instanceof TuiEngineEvent.CompactionStarted);
            System.out.printf("  Compaction triggered: %s%n", hasCompactionEvent);
            System.out.println("=== PASS: Compaction check ===\n");
        }

        @Test
        @DisplayName("cancelled turn returns early")
        void cancelledTurn() throws Exception {
            System.out.println("=== TEST: Cancelled turn ===");

            BlockingQueue<TuiEngineEvent> eventQueue = new LinkedBlockingQueue<>();
            AtomicBoolean cancelToken = new AtomicBoolean(true); // already cancelled
            var compactionConfig = CompactionConfig.disabled();
            List<ChatMessage> messages = new ArrayList<>();

            var loop = new SseTurnLoop(eventQueue, cancelToken,
                    compactionConfig, messages);
            var turn = new TurnContext(10);

            int steps = loop.executeTurn(turn, "Should abort");
            System.out.printf("  Steps taken (should be 0): %d%n", steps);

            List<TuiEngineEvent> events = new ArrayList<>();
            eventQueue.drainTo(events);
            boolean hasAbort = events.stream()
                    .anyMatch(e -> e instanceof TuiEngineEvent.TurnAborted);
            System.out.printf("  TurnAborted event: %s%n", hasAbort);

            assertTrue(hasAbort || steps == 0,
                    "Cancelled turn should abort or take 0 steps");
            System.out.println("=== PASS: Cancelled turn ===\n");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 4. Tool Dispatch Pipeline
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Tool Dispatch")
    class ToolDispatchTests {

        @Test
        @DisplayName("single read tool → serial batch")
        void serialBatchForMutation() throws Exception {
            System.out.println("=== TEST: Tool dispatch batching ===");

            var dispatcher = new ToolDispatcher(new LinkedBlockingQueue<>());
            var toolUses = List.of(
                    ContentBlock.toolUse("id1", "bash",
                            com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()
                                    .put("command", "ls"))
            );

            var batches = dispatcher.planBatches(toolUses);
            System.out.printf("  %d tool use(s) → %d batch(es)%n", toolUses.size(), batches.size());
            batches.forEach(b -> System.out.printf("    %s%n",
                    b instanceof ToolDispatcher.ToolBatch.Serial s
                            ? "Serial: " + s.plan().name()
                            : "Parallel: " + ((ToolDispatcher.ToolBatch.Parallel) b).plans().size()
                                    + " tools"));

            assertEquals(1, batches.size());
            assertInstanceOf(ToolDispatcher.ToolBatch.Serial.class, batches.get(0),
                    "Bash should be serial");
            System.out.println("=== PASS: Tool batching ===\n");
        }

        @Test
        @DisplayName("multiple read-only tools → parallel batch")
        void parallelBatchForReadOnly() throws Exception {
            System.out.println("=== TEST: Parallel tool batching ===");

            var dispatcher = new ToolDispatcher(new LinkedBlockingQueue<>());
            var toolUses = List.of(
                    ContentBlock.toolUse("id1", "read",
                            com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()
                                    .put("path", "/tmp/a")),
                    ContentBlock.toolUse("id2", "grep",
                            com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()
                                    .put("pattern", "hello")),
                    ContentBlock.toolUse("id3", "web_search",
                            com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()
                                    .put("query", "test"))
            );

            var batches = dispatcher.planBatches(toolUses);
            System.out.printf("  %d read-only tools → %d batch(es)%n",
                    toolUses.size(), batches.size());

            assertEquals(1, batches.size());
            assertInstanceOf(ToolDispatcher.ToolBatch.Parallel.class, batches.get(0));
            var parallel = (ToolDispatcher.ToolBatch.Parallel) batches.get(0);
            assertEquals(3, parallel.plans().size());
            System.out.println("=== PASS: Parallel tools ===\n");
        }

        @Test
        @DisplayName("mixed tools → serial + parallel batches")
        void mixedBatching() throws Exception {
            System.out.println("=== TEST: Mixed tool batching ===");

            var dispatcher = new ToolDispatcher(new LinkedBlockingQueue<>());
            var toolUses = List.of(
                    ContentBlock.toolUse("id1", "read",
                            com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()),
                    ContentBlock.toolUse("id2", "grep",
                            com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()),
                    ContentBlock.toolUse("id3", "bash",
                            com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()
                                    .put("command", "make"))
            );

            var batches = dispatcher.planBatches(toolUses);
            System.out.printf("  %d mixed tools → %d batch(es)%n",
                    toolUses.size(), batches.size());
            batches.forEach(b -> System.out.printf("    %s%n",
                    b instanceof ToolDispatcher.ToolBatch.Serial s
                            ? "Serial: " + s.plan().name()
                            : "Parallel: " + ((ToolDispatcher.ToolBatch.Parallel) b).plans().size()
                                    + " tools"));

            // First batch: parallel (read + grep), Second batch: serial (bash)
            assertEquals(2, batches.size());
            assertInstanceOf(ToolDispatcher.ToolBatch.Parallel.class, batches.get(0));
            assertInstanceOf(ToolDispatcher.ToolBatch.Serial.class, batches.get(1));
            System.out.println("=== PASS: Mixed batching ===\n");
        }

        @Test
        @DisplayName("tool execution emits ToolCallStarted and ToolCallComplete")
        void toolExecutionEmitsEvents() throws Exception {
            System.out.println("=== TEST: Tool execution events ===");

            BlockingQueue<TuiEngineEvent> eventQueue = new LinkedBlockingQueue<>();
            var dispatcher = new ToolDispatcher(eventQueue);
            var plan = new ToolDispatcher.ToolExecutionPlan("t1", "read",
                    com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()
                            .put("path", "/tmp/test"));

            var result = dispatcher.executeTool(plan);
            System.out.printf("  Result: %s success=%s%n", result.name(), result.success());

            List<TuiEngineEvent> events = new ArrayList<>();
            eventQueue.drainTo(events);
            printEvents("ToolExecution", events);

            boolean hasStarted = events.stream()
                    .anyMatch(e -> e instanceof TuiEngineEvent.ToolCallStarted);
            boolean hasComplete = events.stream()
                    .anyMatch(e -> e instanceof TuiEngineEvent.ToolCallComplete);

            assertTrue(hasStarted, "Should emit ToolCallStarted");
            assertTrue(hasComplete, "Should emit ToolCallComplete");
            assertTrue(result.success());
            System.out.println("=== PASS: Tool execution events ===\n");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 5. StreamState Content Block Tracking
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("StreamState Content Block Tracking")
    class StreamStateTracking {

        @Test
        @DisplayName("text block: start → append → finalize")
        void textBlockLifecycle() {
            System.out.println("=== TEST: Text block lifecycle ===");

            var ss = new StreamState();
            ss.startBlock(ContentBlock.Kind.TEXT, 0);
            System.out.printf("  Kind after start: %s%n", ss.currentKind());

            ss.appendText("Hello");
            ss.appendText(" World");
            System.out.printf("  Accumulated text: '%s'%n", ss.currentText());

            var block = ss.finalizeText();
            System.out.printf("  Finalized: '%s'%n", block.content());
            assertEquals("Hello World", block.content());
            System.out.println("=== PASS: Text block ===\n");
        }

        @Test
        @DisplayName("thinking block: start → append → finalize")
        void thinkingBlockLifecycle() {
            System.out.println("=== TEST: Thinking block lifecycle ===");

            var ss = new StreamState();
            ss.startBlock(ContentBlock.Kind.THINKING, 0);
            ss.appendThinking("Let me analyze this...");
            ss.setSignature("sig-abc123");

            var block = ss.finalizeThinking();
            System.out.printf("  Thinking: '%s'%n", block.content());
            System.out.printf("  Signature: '%s'%n", block.signature());
            assertEquals("Let me analyze this...", block.content());
            assertEquals("sig-abc123", block.signature());
            System.out.println("=== PASS: Thinking block ===\n");
        }

        @Test
        @DisplayName("tool use block: create → buffer → finalize")
        void toolUseBlockLifecycle() {
            System.out.println("=== TEST: Tool use block lifecycle ===");

            var ss = new StreamState();
            var tu = ss.addToolUse("call_1", "read_file");
            tu.inputBuffer().append("{\"path\": \"/tmp/test\"}");

            System.out.printf("  Tool: id=%s name=%s%n", tu.id(), tu.name());
            System.out.printf("  Input buffer: '%s'%n", tu.inputBuffer().toString());

            var input = tu.finalizeInput();
            System.out.printf("  Finalized input: %s%n", input);
            assertEquals("/tmp/test", input.get("path").asText());
            assertEquals(1, ss.toolUses().size());
            System.out.println("=== PASS: Tool use block ===\n");
        }

        @Test
        @DisplayName("multiple blocks in sequence")
        void multipleBlocksSequence() {
            System.out.println("=== TEST: Multiple block sequence ===");

            var ss = new StreamState();

            // Block 1: Thinking
            ss.startBlock(ContentBlock.Kind.THINKING, 0);
            ss.appendThinking("hmm...");
            var th = ss.finalizeThinking();

            // Block 2: Text
            ss.startBlock(ContentBlock.Kind.TEXT, 1);
            ss.appendText("The answer is 42.");
            var txt = ss.finalizeText();

            // Block 3: Tool use
            var tu = ss.addToolUse("t1", "bash");
            tu.inputBuffer().append("{\"command\":\"echo hi\"}");

            var blocks = ss.buildContentBlocks();
            System.out.printf("  Total blocks: %d%n", blocks.size());
            for (int i = 0; i < blocks.size(); i++) {
                System.out.printf("    [%d] %s%n", i,
                        blocks.get(i).getClass().getSimpleName());
            }

            assertEquals(3, blocks.size());
            System.out.println("=== PASS: Multiple blocks ===\n");
        }

        @Test
        @DisplayName("fake wrapper detection")
        void fakeWrapperDetection() {
            System.out.println("=== TEST: Fake wrapper detection ===");

            assertTrue(StreamState.containsFakeWrapper("[TOOL_CALL]"));
            assertTrue(StreamState.containsFakeWrapper("<codewhale:tool_call"));
            assertTrue(StreamState.containsFakeWrapper("<invoke name=\"bash\">"));
            assertFalse(StreamState.containsFakeWrapper("normal text"));
            assertFalse(StreamState.containsFakeWrapper("print('hello')"));

            // Filter test
            String filtered1 = StreamState.filterToolCallDelta(
                    "before[TOOL_CALL]after", false);
            System.out.printf("  Filtered: '%s' → '%s'%n", "before[TOOL_CALL]after", filtered1);
            assertEquals("before", filtered1);

            String filtered2 = StreamState.filterToolCallDelta(
                    "clean text", false);
            System.out.printf("  Filtered: '%s' → '%s'%n", "clean text", filtered2);
            assertEquals("clean text", filtered2);

            System.out.println("=== PASS: Fake wrapper detection ===\n");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 6. Session & Turn Context
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Session & Turn Context")
    class SessionAndTurn {

        @Test
        @DisplayName("session accumulates messages with revision tracking")
        void sessionMessageTracking() {
            System.out.println("=== TEST: Session message tracking ===");

            var session = new Session("m", Path.of("."), true, false);
            long rev0 = session.messagesRevision();

            session.addMessage(ChatMessage.system("You are helpful"));
            long rev1 = session.messagesRevision();
            assertTrue(rev1 > rev0, "Revision should bump after add");
            System.out.printf("  Added system → revision: %d, messages: %d%n",
                    rev1, session.messages().size());

            session.addMessage(ChatMessage.user("Hello"));
            session.addMessage(ChatMessage.assistant("Hi there!"));
            long rev2 = session.messagesRevision();
            assertTrue(rev2 > rev1, "Revision should bump after each add");
            System.out.printf("  Added user+assistant → revision: %d, messages: %d%n",
                    rev2, session.messages().size());
            assertEquals(3, session.messages().size());

            // Usage accumulation
            session.addUsage(100, 50, 20L, 80L);
            session.addUsage(200, 100, null, 40L);
            System.out.printf("  Total usage: input=%d output=%d total=%d%n",
                    session.totalUsage().inputTokens(),
                    session.totalUsage().outputTokens(),
                    session.totalUsage().totalTokens());
            assertEquals(300, session.totalUsage().inputTokens());
            assertEquals(150, session.totalUsage().outputTokens());
            assertEquals(20L, session.totalUsage().cacheCreationInputTokens());
            assertEquals(120L, session.totalUsage().cacheReadInputTokens());

            System.out.println("=== PASS: Session tracking ===\n");
        }

        @Test
        @DisplayName("turn context tracks steps and cancellation")
        void turnContextTracking() {
            System.out.println("=== TEST: Turn context ===");

            var turn = new TurnContext(5);
            System.out.printf("  Initial: step=%d maxSteps=%d%n", turn.step(), turn.maxSteps());

            assertTrue(turn.nextStep()); // step 1
            assertTrue(turn.nextStep()); // step 2
            System.out.printf("  After 2 steps: step=%d atMax=%s%n",
                    turn.step(), turn.atMaxSteps());
            assertEquals(2, turn.step());
            assertFalse(turn.atMaxSteps());

            turn.recordToolCall();
            turn.recordToolCall();
            System.out.printf("  Tool calls: %d hasTools=%s%n",
                    turn.toolCallCount(), turn.hasToolCalls());
            assertTrue(turn.hasToolCalls());

            // Cancel
            assertFalse(turn.isCancelled());
            turn.cancel();
            assertTrue(turn.isCancelled());
            turn.resetCancel();
            assertFalse(turn.isCancelled());
            System.out.println("  Cancel/reset works");

            // Usage
            turn.addUsage(100, 50);
            System.out.printf("  Usage: input=%d output=%d%n",
                    turn.inputTokens(), turn.outputTokens());

            System.out.println("=== PASS: Turn context ===\n");
        }

        @Test
        @DisplayName("turn reaches max steps")
        void turnMaxSteps() {
            var turn = new TurnContext(2);
            assertTrue(turn.nextStep()); // step 1
            assertFalse(turn.atMaxSteps());
            assertFalse(turn.nextStep()); // step 2, exhausted
            assertTrue(turn.atMaxSteps());
        }

        @Test
        @DisplayName("session trim and replace")
        void sessionTrimAndReplace() {
            var session = new Session("m", Path.of("."), true, false);
            session.addMessage(ChatMessage.user("1"));
            session.addMessage(ChatMessage.assistant("2"));
            session.addMessage(ChatMessage.user("3"));
            session.addMessage(ChatMessage.assistant("4"));
            assertEquals(4, session.messages().size());

            // Trim last 2 (removes last user+assistant exchange)
            session.trimLast(2);
            assertEquals(2, session.messages().size());

            // Replace all
            long revBefore = session.messagesRevision();
            session.replaceMessages(List.of(ChatMessage.system("reset")));
            assertEquals(1, session.messages().size());
            assertTrue(session.messagesRevision() > revBefore);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 7. ContentBlock / SseStreamEvent Parsing
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("SSE Parsing")
    class SseParsingTests {

        @Test
        @DisplayName("parse content delta")
        void parseContentDelta() {
            System.out.println("=== TEST: SSE content delta parsing ===");

            var event = com.jay.tui.client.SseStreamEvent.parseData("""
                    {"choices":[{"index":0,"delta":{"content":"Hello"}}]}""");
            System.out.printf("  Event type: %s%n", event.getClass().getSimpleName());
            assertInstanceOf(com.jay.tui.client.SseStreamEvent.ContentBlockDelta.class, event);
            var delta = (com.jay.tui.client.SseStreamEvent.ContentBlockDelta) event;
            var textDelta = (com.jay.tui.client.SseStreamEvent.Delta.TextDelta) delta.delta();
            assertEquals("Hello", textDelta.text());
            System.out.println("=== PASS: Content delta ===\n");
        }

        @Test
        @DisplayName("parse thinking delta")
        void parseThinkingDelta() {
            var event = com.jay.tui.client.SseStreamEvent.parseData("""
                    {"choices":[{"index":0,"delta":{"reasoning_content":"hmm"}}]}""");
            assertInstanceOf(com.jay.tui.client.SseStreamEvent.ContentBlockDelta.class, event);
            var delta = (com.jay.tui.client.SseStreamEvent.ContentBlockDelta) event;
            assertInstanceOf(com.jay.tui.client.SseStreamEvent.Delta.ThinkingDelta.class, delta.delta());
            var td = (com.jay.tui.client.SseStreamEvent.Delta.ThinkingDelta) delta.delta();
            assertEquals("hmm", td.text());
        }

        @Test
        @DisplayName("parse error frame")
        void parseErrorFrame() {
            var event = com.jay.tui.client.SseStreamEvent.parseData("""
                    {"error":{"message":"rate limit exceeded","code":"rate_limit"}}""");
            assertInstanceOf(com.jay.tui.client.SseStreamEvent.StreamError.class, event);
            var err = (com.jay.tui.client.SseStreamEvent.StreamError) event;
            assertTrue(err.message().contains("rate limit"));
            assertTrue(err.retryable());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 8. Compaction Pipeline
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Compaction")
    class CompactionPipeline {

        @Test
        @DisplayName("shouldCompact triggers above threshold")
        void shouldCompactTriggers() {
            var config = CompactionConfig.of(true, 20, "model");
            var planner = new CompactionPlanner(config);

            // Need >= MIN_SUMMARIZE_MESSAGES (6) messages, plus KEEP_RECENT (4) pinned
            // 15 messages → 4 pinned, 11 to summarize
            var messages = new ArrayList<ChatMessage>();
            for (int i = 0; i < 15; i++) {
                messages.add(ChatMessage.user("This is a long message number "
                        + i + " with extra text to push past token threshold"));
            }
            assertTrue(planner.shouldCompact(messages));

            // Empty messages
            assertFalse(planner.shouldCompact(List.of()));

            // Disabled
            var disabled = new CompactionPlanner(CompactionConfig.disabled());
            assertFalse(disabled.shouldCompact(messages));
        }

        @Test
        @DisplayName("plan preserves recent messages")
        void planPreservesRecent() {
            var messages = new ArrayList<ChatMessage>();
            for (int i = 0; i < 25; i++) {
                messages.add(ChatMessage.user("msg" + i));
            }

            var config = CompactionConfig.of(true, 1000, "model");
            var planner = new CompactionPlanner(config);
            var plan = planner.planCompaction(messages);

            // 25 messages → 4 pinned (KEEP_RECENT=4), 21 summarized
            assertEquals(21, plan.summarizeIndices().size());
            assertEquals(4, plan.pinnedIndices().size());
            // Pinned indices should be [21, 22, 23, 24]
            assertEquals(21, plan.pinnedIndices().get(0));
            assertEquals(24, plan.pinnedIndices().get(plan.pinnedIndices().size() - 1));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════

    /** Drain events with a small wait. */
    static List<TuiEngineEvent> drainWithWait(EngineHandle handle, int waitMs)
            throws InterruptedException {
        Thread.sleep(waitMs);
        return handle.drainEvents();
    }

    /** Print events to stdout for debugging visibility. */
    static void printEvents(String label, List<?> events) {
        if (events.isEmpty()) {
            System.out.printf("  [%s] (no events)%n", label);
            return;
        }
        System.out.printf("  [%s] %d events:%n", label, events.size());
        for (var e : events) {
            String detail = switch (e) {
                case TuiEngineEvent.TurnStarted ts -> "TurnStarted: " + ts.turnId();
                case TuiEngineEvent.TurnComplete tc -> "TurnComplete: " + tc.turnId()
                        + " status=" + tc.status();
                case TuiEngineEvent.TurnAborted ta -> "TurnAborted: " + ta.reason();
                case TuiEngineEvent.MessageDelta md ->
                        "MessageDelta: " + truncate(md.content(), 60);
                case TuiEngineEvent.MessageComplete mc -> "MessageComplete: " + mc.kind();
                case TuiEngineEvent.MessageStarted ms -> "MessageStarted: " + ms.kind();
                case TuiEngineEvent.EngineInitialized ei -> "EngineInitialized: " + ei.model();
                case TuiEngineEvent.StatusMessage sm -> "Status: " + sm.text();
                case TuiEngineEvent.EngineError ee -> "Error: " + ee.message();
                case TuiEngineEvent.ToolCallStarted ts2 -> "ToolCallStarted: " + ts2.name();
                case TuiEngineEvent.ToolCallComplete tc2 ->
                        "ToolCallComplete: " + tc2.name() + " ok=" + tc2.result().success();
                case TuiEngineEvent.CompactionStarted cs -> "CompactionStarted: " + cs.message();
                case TuiEngineEvent.CompactionCompleted cc -> "CompactionCompleted: "
                        + cc.messagesBefore() + "→" + cc.messagesAfter();
                case TuiEngineEvent.ModelChanged mc2 -> "ModelChanged: " + mc2.modelName();
                default -> e.getClass().getSimpleName();
            };
            System.out.printf("    %s%n", detail);
        }
    }

    static String truncate(String s, int max) {
        if (s == null) return "(null)";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
