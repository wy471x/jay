package com.jay.protocol;

import com.jay.protocol.runtime.RuntimeEventEnvelope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration-level parity tests ported from
 * CodeWhale crates/protocol/tests/parity_protocol.rs
 */
class ParityProtocolTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    void threadResumeParamsRoundTrip() throws Exception {
        var params = new ThreadResumeParams(
                "thread-123", null, null,
                "deepseek-v4-pro", "deepseek",
                null, "on-request", "workspace-write",
                null, "base", "dev", "default", true);
        var request = new ThreadRequest.Resume(params);

        var encoded = mapper.writeValueAsString((ThreadRequest) request);
        var decoded = mapper.readValue(encoded, ThreadRequest.class);

        assertInstanceOf(ThreadRequest.Resume.class, decoded);
        var resume = (ThreadRequest.Resume) decoded;
        assertEquals("thread-123", resume.params().threadId());
        assertEquals("deepseek-v4-pro", resume.params().model());
        assertTrue(resume.params().persistExtendedHistory());
    }

    @Test
    void threadListParamsDefaultsAreSerializable() throws Exception {
        var request = new ThreadRequest.List(
                new ThreadListParams(false, 20));

        var encoded = mapper.writeValueAsString((ThreadRequest) request);
        assertTrue(encoded.contains("include_archived"));
    }

    @Test
    void eventFrameSerializationContainsExpectedTag() throws Exception {
        var frame = new EventFrame.TurnComplete("turn-1");
        var encoded = mapper.writeValueAsString((EventFrame) frame);
        assertTrue(encoded.contains("turn_complete"));
    }

    @Test
    void threadGoalSetRequestRoundTrip() throws Exception {
        var request = new ThreadRequest.GoalSet(
                new ThreadGoalSetParams("thread-123", "Release 0.8.59", 42_000L));

        var encoded = mapper.writeValueAsString((ThreadRequest) request);
        assertTrue(encoded.contains("goal_set"));

        var decoded = mapper.readValue(encoded, ThreadRequest.class);
        assertInstanceOf(ThreadRequest.GoalSet.class, decoded);
        var goal = (ThreadRequest.GoalSet) decoded;
        assertEquals("thread-123", goal.params().threadId());
        assertEquals("Release 0.8.59", goal.params().objective());
        assertEquals(42_000L, goal.params().tokenBudget());
    }

    @Test
    void threadGoalEventSerializesStatusAndAccounting() throws Exception {
        var goal = new ThreadGoal(
                "thread-123", "goal-1", "Release 0.8.59",
                ThreadGoalStatus.BUDGET_LIMITED,
                42_000L, 42_001L, 3600L, 7L, 1L, 2L);
        var frame = new EventFrame.ThreadGoalUpdated(goal);

        var encoded = mapper.valueToTree((EventFrame) frame);
        assertEquals("thread_goal_updated", encoded.get("event").asText());
        assertEquals("budget_limited", encoded.get("goal").get("status").asText());
        assertEquals(42_001, encoded.get("goal").get("tokens_used").asLong());
        assertEquals(7, encoded.get("goal").get("continuation_count").asLong());
    }

    @Test
    void threadGoalProgressRequestRoundTrip() throws Exception {
        var request = new ThreadRequest.GoalRecordProgress(
                new ThreadGoalProgressParams("thread-123", 750L, 9L, true));

        var encoded = mapper.writeValueAsString((ThreadRequest) request);
        assertTrue(encoded.contains("goal_record_progress"));

        var decoded = mapper.readValue(encoded, ThreadRequest.class);
        assertInstanceOf(ThreadRequest.GoalRecordProgress.class, decoded);
        var progress = (ThreadRequest.GoalRecordProgress) decoded;
        assertEquals("thread-123", progress.params().threadId());
        assertEquals(750L, progress.params().tokenDelta());
        assertEquals(9L, progress.params().timeDeltaSeconds());
        assertTrue(progress.params().recordContinuation());
    }

    // ---- RuntimeEventEnvelope tests ----

    @Test
    void runtimeEventEnvelopeRoundtrip() throws Exception {
        var payload = mapper.createObjectNode()
                .put("delta", "ok")
                .put("kind", "agent_message");
        var envelope = new RuntimeEventEnvelope(
                1, 12L, "item.delta", "item.delta",
                "thr_123", "turn_456", "item_789",
                "2026-02-11T20:18:49.123Z", "2026-02-11T20:18:49.123Z",
                payload, Map.of());

        var encoded = mapper.valueToTree(envelope);
        assertEquals(1, encoded.get("schema_version").asInt());
        assertEquals(12, encoded.get("seq").asLong());
        assertEquals("item.delta", encoded.get("event").asText());
        assertEquals("item.delta", encoded.get("kind").asText());
        assertEquals("thr_123", encoded.get("thread_id").asText());
        assertEquals("turn_456", encoded.get("turn_id").asText());
        assertEquals("item_789", encoded.get("item_id").asText());
        assertEquals("2026-02-11T20:18:49.123Z", encoded.get("timestamp").asText());
        assertEquals("2026-02-11T20:18:49.123Z", encoded.get("created_at").asText());
        assertEquals("ok", encoded.get("payload").get("delta").asText());
        assertEquals("agent_message", encoded.get("payload").get("kind").asText());
    }

    @Test
    void runtimeEventEnvelopeDefaultsToApiSchemaVersion() throws Exception {
        var json = mapper.createObjectNode()
                .put("seq", 15)
                .put("event", "thread.started")
                .put("kind", "thread.started")
                .put("thread_id", "thr_default_version")
                .put("timestamp", "2026-02-11T20:18:49.123Z")
                .set("payload", mapper.createObjectNode());

        var envelope = mapper.treeToValue(json, RuntimeEventEnvelope.class);
        assertEquals(1, envelope.schemaVersion());
    }

    @Test
    void runtimeEventEnvelopeThreadLevelKeepsTurnAndItemIds() throws Exception {
        var payload = mapper.createObjectNode()
                .set("thread", mapper.createObjectNode().put("id", "thr_thread"));
        var envelope = new RuntimeEventEnvelope(
                1, 14L, "thread.started", "thread.started",
                "thr_thread", null, null,
                "2026-02-11T20:18:49.123Z", null,
                payload, Map.of());

        var encoded = mapper.valueToTree(envelope);
        // turn_id and item_id should be present as null
        assertTrue(encoded.has("turn_id"));
        assertTrue(encoded.has("item_id"));
        assertTrue(encoded.get("turn_id").isNull());
        assertTrue(encoded.get("item_id").isNull());
    }

    @Test
    void runtimeEventEnvelopePreservesUnknownFields() throws Exception {
        var json = mapper.createObjectNode();
        json.put("schema_version", 1);
        json.put("seq", 13);
        json.put("event", "turn.completed");
        json.put("kind", "turn.completed");
        json.put("thread_id", "thr_unknown");
        json.put("timestamp", "2026-02-11T20:18:49.123Z");
        json.putNull("turn_id");
        json.putNull("item_id");
        json.set("payload", mapper.createObjectNode());

        // @JsonIgnoreProperties(ignoreUnknown = true) allows unknown fields to be silently dropped
        json.put("forward_compatibility_hint", "v2-ready");
        var envelope = mapper.treeToValue(json, RuntimeEventEnvelope.class);
        assertEquals(1, envelope.schemaVersion());
        assertEquals(13, envelope.seq());
        assertEquals("turn.completed", envelope.event());
        assertEquals("thr_unknown", envelope.threadId());

        var encoded = mapper.valueToTree(envelope);
        assertEquals(1, encoded.get("schema_version").asInt());
        assertEquals(13, encoded.get("seq").asLong());
        assertEquals("turn.completed", encoded.get("event").asText());
        assertEquals("thr_unknown", encoded.get("thread_id").asText());
    }

    // ---- Additional integration tests ----

    @Test
    void fullThreadLifecycleRequestVariants() throws Exception {
        // Create
        var create = new ThreadRequest.Create(mapper.createObjectNode());
        var createJson = mapper.writeValueAsString((ThreadRequest) create);
        assertTrue(createJson.contains("\"kind\":\"create\""));

        // Message
        var msg = new ThreadRequest.Message("thr_1", "hello world");
        var msgJson = mapper.writeValueAsString((ThreadRequest) msg);
        assertTrue(msgJson.contains("\"kind\":\"message\""));
        assertTrue(msgJson.contains("\"input\":\"hello world\""));

        // Fork
        var fork = new ThreadRequest.Fork(new ThreadForkParams(
                "thr_1", "/tmp", "sonnet", "anthropic",
                "/workspace", "on-request", "workspace-write",
                null, "fork-base", "fork-dev", true));
        var forkJson = mapper.writeValueAsString((ThreadRequest) fork);
        assertTrue(forkJson.contains("\"kind\":\"fork\""));
        assertTrue(forkJson.contains("\"base_instructions\":\"fork-base\""));
    }

    @Test
    void eventFrameAllVariantsHaveCorrectTags() throws Exception {
        var tests = List.<EventFrame>of(
                new EventFrame.ResponseStart("r1"),
                new EventFrame.ResponseEnd("r1"),
                new EventFrame.TurnStarted("t1"),
                new EventFrame.TurnAborted("t1", "timeout"),
                new EventFrame.ThreadGoalCleared("thr_1"),
                new EventFrame.Error("r1", "fail"),
                new EventFrame.PatchApplyBegin("/tmp/file.txt"),
                new EventFrame.PatchApplyEnd("/tmp/file.txt", true),
                new EventFrame.ExecCommandBegin("ls", "/tmp"),
                new EventFrame.ExecCommandEnd("ls", 0),
                new EventFrame.ExecCommandOutputDelta("ls", "file.txt\n"),
                new EventFrame.ElicitationRequest("srv", "req_1", "confirm?"),
                new EventFrame.McpToolCallBegin("srv", "tool1"),
                new EventFrame.McpToolCallEnd("srv", "tool1", true),
                new EventFrame.McpStartupUpdate(new McpStartupUpdateEvent("srv", new McpStartupStatus.Ready())),
                new EventFrame.McpStartupComplete(new McpStartupCompleteEvent(List.of("srv"), List.of(), List.of()))
        );

        for (var frame : tests) {
            var json = mapper.writeValueAsString((EventFrame) frame);
            var back = mapper.readValue(json, EventFrame.class);
            assertEquals(frame.getClass(), back.getClass(),
                    "Roundtrip failed for " + frame.getClass().getSimpleName());
            var tree = mapper.readTree(json);
            assertTrue(tree.has("event"), "Missing 'event' tag in " + frame.getClass().getSimpleName());
        }
    }

    @Test
    void appRequestAllVariantsHaveCorrectTags() throws Exception {
        var tests = List.<AppRequest>of(
                new AppRequest.Capabilities(),
                new AppRequest.ConfigGet("key"),
                new AppRequest.ConfigSet("key", "val"),
                new AppRequest.ConfigUnset("key"),
                new AppRequest.ConfigList(),
                new AppRequest.Models(),
                new AppRequest.ThreadLoadedList()
        );

        for (var req : tests) {
            var json = mapper.writeValueAsString((AppRequest) req);
            var back = mapper.readValue(json, AppRequest.class);
            assertEquals(req.getClass(), back.getClass(),
                    "Roundtrip failed for " + req.getClass().getSimpleName());
        }
    }

    @Test
    void threadGoalProgressDefaultsToZeroWhenFieldsOmitted() throws Exception {
        var json = "{\"kind\":\"goal_record_progress\",\"params\":{\"thread_id\":\"thr_1\"}}";
        var decoded = mapper.readValue(json, ThreadRequest.class);
        assertInstanceOf(ThreadRequest.GoalRecordProgress.class, decoded);
        var progress = (ThreadRequest.GoalRecordProgress) decoded;
        assertEquals(0L, progress.params().tokenDelta());
        assertEquals(0L, progress.params().timeDeltaSeconds());
        assertFalse(progress.params().recordContinuation());
    }

    @Test
    void threadStartParamsOmitsNoneFieldsFromThreadRequest() throws Exception {
        var json = "{" +
                "\"kind\":\"start\"," +
                "\"params\":{" +
                "\"model\":\"deepseek-v4-pro\"," +
                "\"model_provider\":\"deepseek\"," +
                "\"cwd\":\"/tmp/test\"" +
                "}}";
        var decoded = mapper.readValue(json, ThreadRequest.class);
        assertInstanceOf(ThreadRequest.Start.class, decoded);
        var start = (ThreadRequest.Start) decoded;
        assertEquals("deepseek-v4-pro", start.params().model());
        assertEquals("/tmp/test", start.params().cwd());
        assertFalse(start.params().persistExtendedHistory()); // default
    }
}
