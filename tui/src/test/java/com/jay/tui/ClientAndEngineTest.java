package com.jay.tui;

import com.jay.agent.ProviderKind;
import com.jay.tui.client.*;
import com.jay.tui.core.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClientAndEngineTest {

    // ── ChatMessage ────────────────────────────────────────────────────

    @Nested
    class ChatMessageTests {

        @Test
        void userFactory() {
            var msg = ChatMessage.user("hello");
            assertEquals("user", msg.role());
            assertEquals("hello", msg.content());
            assertNull(msg.toolCalls());
            assertNull(msg.toolCallId());
        }

        @Test
        void assistantFactory() {
            var msg = ChatMessage.assistant("response");
            assertEquals("assistant", msg.role());
            assertEquals("response", msg.content());
        }

        @Test
        void systemFactory() {
            var msg = ChatMessage.system("you are helpful");
            assertEquals("system", msg.role());
        }

        @Test
        void toolFactory() {
            var msg = ChatMessage.tool("call-1", "result");
            assertEquals("tool", msg.role());
            assertEquals("result", msg.content());
            assertEquals("call-1", msg.toolCallId());
        }

        @Test
        void toJsonProducesValidJson() {
            var msg = ChatMessage.user("test");
            var json = msg.toJson();
            assertTrue(json.contains("\"role\""));
            assertTrue(json.contains("\"user\""));
            assertTrue(json.contains("\"content\""));
            assertTrue(json.contains("\"test\""));
        }
    }

    // ── StreamEvent ────────────────────────────────────────────────────

    @Nested
    class StreamEventTests {

        @Test
        void contentDelta() {
            var e = new StreamEvent.ContentDelta("hello", "think");
            assertEquals("hello", e.content());
            assertEquals("think", e.reasoning());
        }

        @Test
        void contentDeltaNullReasoning() {
            var e = new StreamEvent.ContentDelta("hello", null);
            assertNull(e.reasoning());
        }

        @Test
        void toolCallDelta() {
            var e = new StreamEvent.ToolCallDelta("id1", "read", "{\"path\":\"/\"}");
            assertEquals("id1", e.id());
            assertEquals("read", e.name());
            assertEquals("{\"path\":\"/\"}", e.arguments());
        }

        @Test
        void done() {
            var e = new StreamEvent.Done("stop");
            assertEquals("stop", e.stopReason());
        }

        @Test
        void error() {
            var e = new StreamEvent.Error("timeout");
            assertEquals("timeout", e.message());
        }

        @Test
        void usage() {
            var e = new StreamEvent.Usage(100, 50);
            assertEquals(100, e.promptTokens());
            assertEquals(50, e.completionTokens());
        }
    }

    // ── StreamCollector ────────────────────────────────────────────────

    @Nested
    class StreamCollectorTests {

        @Test
        void collectsContentDeltas() {
            var collector = new StreamCollector();
            collector.onSubscribe(new SimpleSubscription());
            collector.onNext(new StreamEvent.ContentDelta("Hello", null));
            collector.onNext(new StreamEvent.ContentDelta(" World", null));
            assertEquals("Hello World", collector.currentContent());
        }

        @Test
        void collectsReasoning() {
            var collector = new StreamCollector();
            collector.onSubscribe(new SimpleSubscription());
            collector.onNext(new StreamEvent.ContentDelta("answer", "thinking"));
            var display = collector.displayContent();
            assertTrue(display.contains("<thinking>"));
            assertTrue(display.contains("thinking"));
            assertTrue(display.contains("answer"));
        }

        @Test
        void collectsToolCalls() {
            var collector = new StreamCollector();
            collector.onSubscribe(new SimpleSubscription());
            collector.onNext(new StreamEvent.ToolCallDelta("id1", "read", "{\"path\":\"file\"}"));
            collector.onComplete();

            var result = collector.await();
            assertTrue(result.success());
            assertEquals(1, result.toolCalls().size());
            assertEquals("read", result.toolCalls().get(0).function().name());
        }

        @Test
        void handlesError() {
            var collector = new StreamCollector();
            collector.onSubscribe(new SimpleSubscription());
            collector.onNext(new StreamEvent.Error("connection refused"));
            collector.onComplete();

            var result = collector.await();
            assertFalse(result.success());
            assertEquals("connection refused", result.error());
        }

        @Test
        void handlesSubscriberError() {
            var collector = new StreamCollector();
            collector.onSubscribe(new SimpleSubscription());
            collector.onError(new RuntimeException("crash"));

            var result = collector.await();
            assertFalse(result.success());
            assertEquals("crash", result.error());
        }

        @Test
        void isCompletedTracksState() {
            var collector = new StreamCollector();
            collector.onSubscribe(new SimpleSubscription());
            assertFalse(collector.isCompleted());
            collector.onComplete();
            assertTrue(collector.isCompleted());
        }

        @Test
        void displayContentWithoutReasoning() {
            var collector = new StreamCollector();
            collector.onSubscribe(new SimpleSubscription());
            collector.onNext(new StreamEvent.ContentDelta("text", null));
            assertEquals("text", collector.displayContent());
        }

        private static class SimpleSubscription implements java.util.concurrent.Flow.Subscription {
            @Override public void request(long n) {}
            @Override public void cancel() {}
        }
    }

    // ── LlmClient.ChatResult ──────────────────────────────────────────

    @Nested
    class ChatResultTests {

        @Test
        void successResult() {
            var result = LlmClient.ChatResult.success("content", List.of(),
                    new StreamEvent.Usage(10, 5));
            assertTrue(result.success());
            assertEquals("content", result.content());
            assertNull(result.error());
            assertNotNull(result.usage());
        }

        @Test
        void errorResult() {
            var result = LlmClient.ChatResult.error("failed");
            assertFalse(result.success());
            assertEquals("failed", result.error());
            assertNull(result.content());
        }
    }

    // ── TuiEngine (new API via EngineHandle) ────────────────────────────

    @Nested
    class TuiEngineTests {

        @Test
        void spawnAndShutdown() {
            var config = EngineConfig.builder()
                    .model("test-model")
                    .workspace(Path.of("."))
                    .build();
            EngineHandle handle = TuiEngine.spawn(config);
            assertTrue(handle.isRunning());
            handle.shutdown();
        }

        @Test
        void engineInitializedEventEmitted() throws InterruptedException {
            var config = EngineConfig.builder()
                    .model("deepseek-v4-pro")
                    .workspace(Path.of("."))
                    .build();
            EngineHandle handle = TuiEngine.spawn(config);

            // Poll for the EngineInitialized event
            TuiEngineEvent event = handle.pollEvent(2000);
            assertNotNull(event);
            assertInstanceOf(TuiEngineEvent.EngineInitialized.class, event);
            var init = (TuiEngineEvent.EngineInitialized) event;
            assertEquals("deepseek-v4-pro", init.model());

            handle.shutdown();
        }

        @Test
        void sendMessageGeneratesTurnEvents() throws InterruptedException {
            var config = EngineConfig.builder().workspace(Path.of(".")).build();
            EngineHandle handle = TuiEngine.spawn(config);

            handle.sendOp(new TuiEngineOp.SendMessage(
                    "hello", "agent", "test-model", true, false, false, false, List.of()));

            // Should get TurnStarted, MessageStarted, MessageDelta, MessageComplete, TurnComplete
            List<TuiEngineEvent> events = collectEvents(handle, 200);
            assertTrue(events.stream().anyMatch(e -> e instanceof TuiEngineEvent.TurnStarted));
            assertTrue(events.stream().anyMatch(e -> e instanceof TuiEngineEvent.MessageDelta));
            assertTrue(events.stream().anyMatch(e -> e instanceof TuiEngineEvent.TurnComplete));

            handle.shutdown();
        }

        @Test
        void cancelRequestSetsCancelFlag() {
            var config = EngineConfig.builder().workspace(Path.of(".")).build();
            EngineHandle handle = TuiEngine.spawn(config);

            assertFalse(handle.isCancelled());
            handle.cancel();
            assertTrue(handle.isCancelled());
            handle.resetCancel();
            assertFalse(handle.isCancelled());

            handle.shutdown();
        }

        @Test
        void setModelGeneratesModelChangedEvent() throws InterruptedException {
            var config = EngineConfig.builder().workspace(Path.of(".")).build();
            EngineHandle handle = TuiEngine.spawn(config);

            handle.sendOp(new TuiEngineOp.SetModel("gpt-5", "agent"));

            Thread.sleep(100);
            List<TuiEngineEvent> events = handle.drainEvents();
            assertTrue(events.stream().anyMatch(e -> e instanceof TuiEngineEvent.ModelChanged));

            handle.shutdown();
        }

        @Test
        void shutdownOpStopsEngine() throws InterruptedException {
            var config = EngineConfig.builder().workspace(Path.of(".")).build();
            EngineHandle handle = TuiEngine.spawn(config);

            assertTrue(handle.isRunning());
            handle.shutdown();
            Thread.sleep(200);
            assertFalse(handle.isRunning());
        }

        /** Drain events with a small wait for async processing. */
        private List<TuiEngineEvent> collectEvents(EngineHandle handle, int waitMs)
                throws InterruptedException {
            Thread.sleep(waitMs);
            return handle.drainEvents();
        }
    }
}
