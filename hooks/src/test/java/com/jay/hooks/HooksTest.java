package com.jay.hooks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jay.hooks.sink.JsonlHookSink;
import com.jay.hooks.sink.StdoutHookSink;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class HooksTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    // ── HookEvent serialization ─────────────────────────────────────

    @Nested
    class HookEventSerialization {

        @Test
        void responseStartSerializesWithCorrectType() throws Exception {
            var event = new HookEvent.ResponseStart("resp-1");
            var json = mapper.readTree(mapper.writeValueAsString(event));
            assertEquals("response_start", json.get("type").asText());
            assertEquals("resp-1", json.get("responseId").asText());
        }

        @Test
        void responseDeltaSerializesCorrectly() throws Exception {
            var event = new HookEvent.ResponseDelta("resp-1", "Hello");
            var json = mapper.readTree(mapper.writeValueAsString(event));
            assertEquals("response_delta", json.get("type").asText());
            assertEquals("resp-1", json.get("responseId").asText());
            assertEquals("Hello", json.get("delta").asText());
        }

        @Test
        void responseEndSerializesCorrectly() throws Exception {
            var event = new HookEvent.ResponseEnd("resp-1");
            var json = mapper.readTree(mapper.writeValueAsString(event));
            assertEquals("response_end", json.get("type").asText());
        }

        @Test
        void toolLifecycleSerializesWithSnakeCaseTypeAndPayload() throws Exception {
            var payload = mapper.createObjectNode().put("status", "ok");
            var event = new HookEvent.ToolLifecycle("resp-1", "exec_shell", "start", payload);
            var json = mapper.readTree(mapper.writeValueAsString(event));
            assertEquals("tool_lifecycle", json.get("type").asText());
            assertEquals("resp-1", json.get("responseId").asText());
            assertEquals("exec_shell", json.get("toolName").asText());
            assertEquals("start", json.get("phase").asText());
            assertEquals("ok", json.get("payload").get("status").asText());
        }

        @Test
        void jobLifecycleSerializesCorrectly() throws Exception {
            var event = new HookEvent.JobLifecycle("job-42", "running", 50, "halfway");
            var json = mapper.readTree(mapper.writeValueAsString(event));
            assertEquals("job_lifecycle", json.get("type").asText());
            assertEquals("job-42", json.get("jobId").asText());
            assertEquals("running", json.get("phase").asText());
            assertEquals(50, json.get("progress").asInt());
            assertEquals("halfway", json.get("detail").asText());
        }

        @Test
        void approvalLifecycleSerializesCorrectly() throws Exception {
            var event = new HookEvent.ApprovalLifecycle("appr-7", "approved", "looks good");
            var json = mapper.readTree(mapper.writeValueAsString(event));
            assertEquals("approval_lifecycle", json.get("type").asText());
            assertEquals("appr-7", json.get("approvalId").asText());
            assertEquals("approved", json.get("phase").asText());
            assertEquals("looks good", json.get("reason").asText());
        }
    }

    // ── StdoutHookSink ──────────────────────────────────────────────

    @Nested
    class StdoutHookSinkTests {

        @Test
        void emitDoesNotThrow() {
            var sink = new StdoutHookSink();
            assertDoesNotThrow(() -> sink.emit(new HookEvent.ResponseStart("test")));
        }
    }

    // ── JsonlHookSink ───────────────────────────────────────────────

    @Nested
    class JsonlHookSinkTests {

        @TempDir
        Path tempDir;

        @Test
        void createsParentDirAndAppendsEvents() throws Exception {
            Path filePath = tempDir.resolve("sub/events.jsonl");
            var sink = new JsonlHookSink(filePath);

            sink.emit(new HookEvent.ResponseStart("resp-1"));
            sink.emit(new HookEvent.ResponseEnd("resp-1"));

            assertTrue(Files.exists(filePath));
            List<String> lines = Files.readAllLines(filePath);
            assertEquals(2, lines.size());

            for (String line : lines) {
                var json = mapper.readTree(line);
                assertTrue(json.has("at"));
                assertTrue(json.has("event"));
                assertTrue(json.get("event").has("type"));
            }
        }
    }

    // ── HookDispatcher ──────────────────────────────────────────────

    @Nested
    class HookDispatcherTests {

        @Test
        void emitsToAllSinks() {
            var dispatcher = new HookDispatcher();
            var sink1 = new RecordingSink();
            var sink2 = new RecordingSink();
            dispatcher.addSink(sink1);
            dispatcher.addSink(sink2);

            var event = new HookEvent.ResponseStart("resp-1");
            dispatcher.emit(event);

            assertEquals(1, sink1.events.size());
            assertEquals(1, sink2.events.size());
            assertEquals("resp-1", ((HookEvent.ResponseStart) sink1.events.get(0)).responseId());
            assertEquals("resp-1", ((HookEvent.ResponseStart) sink2.events.get(0)).responseId());
        }

        @Test
        void continuesAfterSinkError() {
            var dispatcher = new HookDispatcher();
            var sink1 = new RecordingSink();
            var sink2 = new RecordingSink();
            dispatcher.addSink(sink1);
            dispatcher.addSink(new FailingSink());
            dispatcher.addSink(sink2);

            var event = new HookEvent.ResponseEnd("resp-1");
            dispatcher.emit(event);

            // Both recording sinks should receive the event despite the failing one
            assertEquals(1, sink1.events.size());
            assertEquals(1, sink2.events.size());
        }

        @Test
        void sinkCountReflectsRegistration() {
            var dispatcher = new HookDispatcher();
            assertEquals(0, dispatcher.sinkCount());
            dispatcher.addSink(new RecordingSink());
            assertEquals(1, dispatcher.sinkCount());
            dispatcher.addSink(new RecordingSink());
            assertEquals(2, dispatcher.sinkCount());
        }

        @Test
        void removeSinkUnregisters() {
            var dispatcher = new HookDispatcher();
            var sink = new RecordingSink();
            dispatcher.addSink(sink);
            dispatcher.removeSink(sink);
            assertEquals(0, dispatcher.sinkCount());
            dispatcher.emit(new HookEvent.ResponseStart("x"));
            assertEquals(0, sink.events.size());
        }

        @Test
        void dispatchesMultipleEventTypes() {
            var dispatcher = new HookDispatcher();
            var sink = new RecordingSink();
            dispatcher.addSink(sink);

            dispatcher.emit(new HookEvent.ResponseStart("a"));
            dispatcher.emit(new HookEvent.ResponseDelta("a", "chunk"));

            var payload = mapper.createObjectNode().put("ok", true);
            dispatcher.emit(new HookEvent.ToolLifecycle("b", "echo", "start", payload));
            dispatcher.emit(new HookEvent.JobLifecycle("j1", "done", 100, null));
            dispatcher.emit(new HookEvent.ApprovalLifecycle("apr", "denied", "risky"));

            assertEquals(5, sink.events.size());
        }
    }

    // ── Test helpers ────────────────────────────────────────────────

    static class RecordingSink implements HookSink {
        final List<HookEvent> events = new ArrayList<>();

        @Override
        public void emit(HookEvent event) {
            events.add(event);
        }
    }

    static class FailingSink implements HookSink {
        @Override
        public void emit(HookEvent event) {
            throw new RuntimeException("sink failed");
        }
    }
}
