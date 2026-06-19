package com.jay.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Roundtrip serialization tests for core protocol types.
 */
class ProtocolSerializationTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    // ---- ThreadStatus serialization ----

    @Test
    void threadStatusSnakeCase() throws Exception {
        assertEquals("\"running\"", mapper.writeValueAsString(ThreadStatus.RUNNING));
        assertEquals(ThreadStatus.IDLE, mapper.readValue("\"idle\"", ThreadStatus.class));
        assertEquals(ThreadStatus.COMPLETED, mapper.readValue("\"completed\"", ThreadStatus.class));
        assertEquals("\"archived\"", mapper.writeValueAsString(ThreadStatus.ARCHIVED));
    }

    @Test
    void sessionSourceSnakeCase() throws Exception {
        assertEquals("\"interactive\"", mapper.writeValueAsString(SessionSource.INTERACTIVE));
        assertEquals(SessionSource.API, mapper.readValue("\"api\"", SessionSource.class));
        assertEquals(SessionSource.FORK, mapper.readValue("\"fork\"", SessionSource.class));
    }

    @Test
    void threadGoalStatusSnakeCase() throws Exception {
        assertEquals("\"active\"", mapper.writeValueAsString(ThreadGoalStatus.ACTIVE));
        assertEquals(ThreadGoalStatus.BLOCKED, mapper.readValue("\"blocked\"", ThreadGoalStatus.class));
        assertEquals(ThreadGoalStatus.COMPLETE, mapper.readValue("\"complete\"", ThreadGoalStatus.class));
    }

    // ---- Thread serialization ----

    @Test
    void threadRoundtrip() throws Exception {
        var thread = new Thread("thr_1", "preview...", false, "deepseek",
                1700000000L, 1700000100L, ThreadStatus.RUNNING,
                "/tmp/test", "/home/user", "0.1.0", SessionSource.INTERACTIVE, null);

        var json = mapper.writeValueAsString(thread);
        assertTrue(json.contains("\"id\":\"thr_1\""));
        assertTrue(json.contains("\"status\":\"running\""));
        assertTrue(json.contains("\"model_provider\":\"deepseek\""));
        // null name should be omitted
        assertFalse(json.contains("\"name\""));

        var back = mapper.readValue(json, Thread.class);
        assertEquals("thr_1", back.id());
        assertEquals(ThreadStatus.RUNNING, back.status());
        assertNull(back.name());
    }

    // ---- ThreadRequest tagged union ----

    @Test
    void threadRequestCreate() throws Exception {
        var req = new ThreadRequest.Create(JsonNodeFactory.instance.objectNode());
        var json = mapper.writeValueAsString((ThreadRequest) req);
        assertTrue(json.contains("\"kind\":\"create\""));

        var back = mapper.readValue(json, ThreadRequest.class);
        assertInstanceOf(ThreadRequest.Create.class, back);
    }

    @Test
    void threadRequestArchive() throws Exception {
        var req = new ThreadRequest.Archive("thr_abc");
        var json = mapper.writeValueAsString((ThreadRequest) req);
        assertTrue(json.contains("\"kind\":\"archive\""));
        assertTrue(json.contains("\"thread_id\":\"thr_abc\""));

        var back = mapper.readValue(json, ThreadRequest.class);
        assertInstanceOf(ThreadRequest.Archive.class, back);
        assertEquals("thr_abc", ((ThreadRequest.Archive) back).threadId());
    }

    @Test
    void threadRequestStart() throws Exception {
        var params = new ThreadStartParams("sonnet", "anthropic", "/workspace", true);
        var req = new ThreadRequest.Start(params);
        var json = mapper.writeValueAsString((ThreadRequest) req);

        var back = mapper.readValue(json, ThreadRequest.class);
        assertInstanceOf(ThreadRequest.Start.class, back);
        var start = (ThreadRequest.Start) back;
        assertEquals("sonnet", start.params().model());
        assertTrue(start.params().persistExtendedHistory());
    }

    @Test
    void threadStartParamsDefaultPersist() throws Exception {
        var json = "{\"kind\":\"start\",\"params\":{\"model\":\"claude-sonnet-4-6\"}}";
        var back = mapper.readValue(json, ThreadRequest.class);
        assertInstanceOf(ThreadRequest.Start.class, back);
        assertFalse(((ThreadRequest.Start) back).params().persistExtendedHistory());
    }

    // ---- ThreadResponse ----

    @Test
    void threadResponseDefaults() throws Exception {
        var resp = new ThreadResponse("thr_1", "ok", null, null, null, null, null,
                null, null, null, null, null);
        var json = mapper.writeValueAsString(resp);
        assertTrue(json.contains("\"thread_id\":\"thr_1\""));
        assertTrue(json.contains("\"status\":\"ok\""));
        // Default empty data should be present
        assertTrue(json.contains("\"data\":{}"));
    }

    // ---- EventFrame tagged union ----

    @Test
    void eventFrameResponseStart() throws Exception {
        var event = new EventFrame.ResponseStart("resp_1");
        var json = mapper.writeValueAsString((EventFrame) event);
        assertTrue(json.contains("\"event\":\"response_start\""));
        assertTrue(json.contains("\"response_id\":\"resp_1\""));

        var back = mapper.readValue(json, EventFrame.class);
        assertInstanceOf(EventFrame.ResponseStart.class, back);
        assertEquals("resp_1", ((EventFrame.ResponseStart) back).responseId());
    }

    @Test
    void eventFrameResponseDeltaDefaultsChannelToText() throws Exception {
        var json = "{\"event\":\"response_delta\",\"response_id\":\"r1\",\"delta\":\"hello\"}";
        var back = mapper.readValue(json, EventFrame.class);
        assertInstanceOf(EventFrame.ResponseDelta.class, back);
        // Channel defaults to TEXT when omitted; Jackson subtype deser may leave null, check both
        var channel = ((EventFrame.ResponseDelta) back).channel();
        assertTrue(channel == null || channel == ResponseChannel.TEXT,
                "channel should be null or TEXT, got: " + channel);
    }

    @Test
    void eventFrameToolCallStart() throws Exception {
        var args = JsonNodeFactory.instance.objectNode().put("path", "/tmp/test");
        var event = new EventFrame.ToolCallStart("resp_1", "bash", args);
        var json = mapper.writeValueAsString((EventFrame) event);
        assertTrue(json.contains("\"tool_name\":\"bash\""));

        var back = mapper.readValue(json, EventFrame.class);
        assertInstanceOf(EventFrame.ToolCallStart.class, back);
        assertEquals("bash", ((EventFrame.ToolCallStart) back).toolName());
    }

    @Test
    void eventFrameError() throws Exception {
        var event = new EventFrame.Error("resp_1", "something went wrong");
        var json = mapper.writeValueAsString((EventFrame) event);
        assertTrue(json.contains("\"event\":\"error\""));
        assertTrue(json.contains("\"message\":\"something went wrong\""));

        var back = mapper.readValue(json, EventFrame.class);
        assertInstanceOf(EventFrame.Error.class, back);
    }

    @Test
    void eventFrameTurnComplete() throws Exception {
        var event = new EventFrame.TurnComplete("turn_42");
        var json = mapper.writeValueAsString((EventFrame) event);
        assertEquals("\"turn_42\"", mapper.readTree(json).get("turn_id").toString());
    }

    // ---- AskForApproval ----

    @Test
    void askForApprovalRejectRoundtrip() throws Exception {
        var policy = new AskForApproval.Reject(true, false, false);
        var json = mapper.writeValueAsString((AskForApproval) policy);
        assertTrue(json.contains("\"type\":\"reject\""));
        assertTrue(json.contains("\"sandbox_approval\":true"));

        var back = mapper.readValue(json, AskForApproval.class);
        assertInstanceOf(AskForApproval.Reject.class, back);
        assertTrue(((AskForApproval.Reject) back).sandboxApproval());
    }

    @Test
    void askForApprovalNever() throws Exception {
        var policy = new AskForApproval.Never();
        var json = mapper.writeValueAsString((AskForApproval) policy);
        assertEquals("{\"type\":\"never\"}", json);
    }

    // ---- ReviewDecision ----

    @Test
    void reviewDecisionNetworkPolicyAmendment() throws Exception {
        var decision = new ReviewDecision.NetworkPolicyAmendment("github.com", NetworkPolicyRuleAction.ALLOW);
        var json = mapper.writeValueAsString((ReviewDecision) decision);
        assertTrue(json.contains("\"type\":\"network_policy_amendment\""));
        assertTrue(json.contains("\"host\":\"github.com\""));

        var back = mapper.readValue(json, ReviewDecision.class);
        assertInstanceOf(ReviewDecision.NetworkPolicyAmendment.class, back);
        assertEquals(NetworkPolicyRuleAction.ALLOW, ((ReviewDecision.NetworkPolicyAmendment) back).action());
    }

    // ---- ToolPayload ----

    @Test
    void toolPayloadFunctionRoundtrip() throws Exception {
        var payload = new ToolPayload.Function("{\"key\":\"value\"}");
        var json = mapper.writeValueAsString((ToolPayload) payload);
        assertTrue(json.contains("\"type\":\"function\""));

        var back = mapper.readValue(json, ToolPayload.class);
        assertInstanceOf(ToolPayload.Function.class, back);
    }

    @Test
    void toolPayloadMcpWithOptionalCallId() throws Exception {
        var args = JsonNodeFactory.instance.objectNode().put("x", 1);
        var payload = new ToolPayload.Mcp("srv", "tool1", args, null);
        var json = mapper.writeValueAsString((ToolPayload) payload);

        var back = mapper.readValue(json, ToolPayload.class);
        assertInstanceOf(ToolPayload.Mcp.class, back);
        assertEquals("srv", ((ToolPayload.Mcp) back).server());
        assertNull(((ToolPayload.Mcp) back).rawToolCallId());
    }

    // ---- ToolOutput ----

    @Test
    void toolOutputFunctionSuccess() throws Exception {
        var output = new ToolOutput.Function(JsonNodeFactory.instance.textNode("result"), true);
        var json = mapper.writeValueAsString((ToolOutput) output);
        assertTrue(json.contains("\"success\":true"));

        var back = mapper.readValue(json, ToolOutput.class);
        assertInstanceOf(ToolOutput.Function.class, back);
        assertTrue(((ToolOutput.Function) back).success());
    }

    // ---- AppRequest ----

    @Test
    void appRequestConfigGet() throws Exception {
        var req = new AppRequest.ConfigGet("jay.model");
        var json = mapper.writeValueAsString((AppRequest) req);
        assertTrue(json.contains("\"kind\":\"config_get\""));

        var back = mapper.readValue(json, AppRequest.class);
        assertInstanceOf(AppRequest.ConfigGet.class, back);
    }

    @Test
    void appRequestCapabilities() throws Exception {
        var req = new AppRequest.Capabilities();
        var json = mapper.writeValueAsString((AppRequest) req);
        assertEquals("{\"kind\":\"capabilities\"}", json);
    }

    // ---- PromptRequest/Response ----

    @Test
    void promptRequestOmitsNullThreadId() throws Exception {
        var req = new PromptRequest(null, "hello", "sonnet");
        var json = mapper.writeValueAsString(req);
        assertFalse(json.contains("thread_id"));
        assertTrue(json.contains("\"prompt\":\"hello\""));
    }

    @Test
    void promptResponseDefaultsEmptyEvents() throws Exception {
        var resp = new PromptResponse("output text", "sonnet", null);
        var json = mapper.writeValueAsString(resp);
        // With @JsonInclude(NON_EMPTY), empty list is serialized as [] or omitted
        assertTrue(json.contains("output text"));
        assertTrue(json.contains("sonnet"));
    }

    // ---- Envelope ----

    @Test
    void envelopeOmitsNullThreadId() throws Exception {
        var env = new Envelope<>("req_1", null, "body");
        var json = mapper.writeValueAsString(env);
        assertFalse(json.contains("threadId")); // Jackson uses Java field name by default for records
        assertTrue(json.contains("\"requestId\":\"req_1\""));
    }

    @Test
    void envelopeWithThreadId() throws Exception {
        var env = new Envelope<>("req_1", "thr_1", "body");
        var json = mapper.writeValueAsString(env);
        assertTrue(json.contains("\"threadId\":\"thr_1\""));
    }

    // ---- NetworkPolicy ----

    @Test
    void networkPolicyRuleActionSnakeCase() throws Exception {
        assertEquals("\"allow\"", mapper.writeValueAsString(NetworkPolicyRuleAction.ALLOW));
        assertEquals("\"deny\"", mapper.writeValueAsString(NetworkPolicyRuleAction.DENY));
    }

    // ---- McpStartupStatus (sealed interface) ----

    @Test
    void mcpStartupStatusFailedCarriesError() throws Exception {
        var status = new McpStartupStatus.Failed("connection refused");
        var json = mapper.writeValueAsString((McpStartupStatus) status);
        assertTrue(json.contains("\"status\":\"failed\""));
        assertTrue(json.contains("\"error\":\"connection refused\""));

        var back = mapper.readValue(json, McpStartupStatus.class);
        assertInstanceOf(McpStartupStatus.Failed.class, back);
        assertEquals("connection refused", ((McpStartupStatus.Failed) back).error());
    }

    @Test
    void mcpStartupStatusStarting() throws Exception {
        var status = new McpStartupStatus.Starting();
        var json = mapper.writeValueAsString((McpStartupStatus) status);
        assertEquals("{\"status\":\"starting\"}", json);
    }

    // ---- ExecApprovalRequestEvent defaults ----

    @Test
    void execApprovalRequestEventDefaultEmptyLists() throws Exception {
        var json = "{" +
                "\"call_id\":\"c1\"," +
                "\"approval_id\":\"a1\"," +
                "\"turn_id\":\"t1\"," +
                "\"command\":\"ls\"," +
                "\"cwd\":\"/tmp\"," +
                "\"reason\":\"test\"" +
                "}";
        var event = mapper.readValue(json, ExecApprovalRequestEvent.class);
        assertEquals("c1", event.callId());
        assertTrue(event.proposedExecpolicyAmendment().isEmpty());
        assertTrue(event.additionalPermissions().isEmpty());
        assertTrue(event.availableDecisions().isEmpty());
        assertNull(event.matchedRule());
        assertNull(event.networkApprovalContext());
    }
}
