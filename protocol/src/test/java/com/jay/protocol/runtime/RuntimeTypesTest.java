package com.jay.protocol.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Roundtrip serialization tests for Runtime API protocol types.
 * Ported from Rust's runtime/mod.rs tests (lines 183-364).
 */
class RuntimeTypesTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    void dynamicToolSpecRoundtrip() throws Exception {
        var spec = new DynamicToolSpec(
                "tau_bench", "get_reservation",
                "Look up an airline reservation.",
                mapper.createObjectNode()
                        .put("type", "object")
                        .set("properties", mapper.createObjectNode()
                                .set("reservation_id", mapper.createObjectNode()
                                        .put("type", "string"))),
                false);

        var json = mapper.writeValueAsString(spec);
        var back = mapper.readValue(json, DynamicToolSpec.class);

        assertEquals("tau_bench", back.namespace());
        assertEquals("get_reservation", back.name());
        assertEquals("Look up an airline reservation.", back.description());
        assertFalse(back.deferLoading());
    }

    @Test
    void dynamicToolSpecOmitsDeferLoadingDefaultsFalse() throws Exception {
        var json = "{" +
                "\"namespace\":\"tau_bench\"," +
                "\"name\":\"get_reservation\"," +
                "\"description\":\"Look up an airline reservation.\"," +
                "\"input_schema\":{\"type\":\"object\"}" +
                "}";

        var spec = mapper.readValue(json, DynamicToolSpec.class);
        assertEquals("tau_bench", spec.namespace());
        assertEquals("get_reservation", spec.name());
        assertFalse(spec.deferLoading());
    }

    @Test
    void dynamicToolItemStatusSnakeCase() throws Exception {
        assertEquals("\"in_progress\"",
                mapper.writeValueAsString(DynamicToolItemStatus.IN_PROGRESS));
        assertEquals(DynamicToolItemStatus.COMPLETED,
                mapper.readValue("\"completed\"", DynamicToolItemStatus.class));
        assertEquals(DynamicToolItemStatus.FAILED,
                mapper.readValue("\"failed\"", DynamicToolItemStatus.class));
    }

    @Test
    void dynamicToolCallParamsRoundtrip() throws Exception {
        var args = mapper.createObjectNode().put("reservation_id", "ABC123");
        var params = new DynamicToolCallParams(
                "thr_123", "turn_456", "call_abc",
                "tau_bench", "get_reservation", args);

        var json = mapper.writeValueAsString(params);
        var back = mapper.readValue(json, DynamicToolCallParams.class);

        assertEquals("thr_123", back.threadId());
        assertEquals("tau_bench", back.namespace());
        assertEquals("get_reservation", back.tool());
        assertEquals("ABC123", back.arguments().get("reservation_id").asText());
    }

    @Test
    void dynamicToolCallContentRoundtrip() throws Exception {
        var item1 = new DynamicToolCallContent.InputText("{\"status\":\"confirmed\"}");
        var item2 = new DynamicToolCallContent.InputImage("http://example.com/receipt.png");

        // Test individual roundtrips
        var json1 = mapper.writeValueAsString((DynamicToolCallContent) item1);
        var back1 = mapper.readValue(json1, DynamicToolCallContent.class);
        assertInstanceOf(DynamicToolCallContent.InputText.class, back1);

        var json2 = mapper.writeValueAsString((DynamicToolCallContent) item2);
        var back2 = mapper.readValue(json2, DynamicToolCallContent.class);
        assertInstanceOf(DynamicToolCallContent.InputImage.class, back2);
        assertEquals("http://example.com/receipt.png",
                ((DynamicToolCallContent.InputImage) back2).imageUrl());
    }

    @Test
    void dynamicToolCallContentExactJsonTags() throws Exception {
        var text = new DynamicToolCallContent.InputText("x");
        assertEquals("{\"type\":\"input_text\",\"text\":\"x\"}",
                mapper.writeValueAsString(text));

        var image = new DynamicToolCallContent.InputImage("y");
        assertEquals("{\"type\":\"input_image\",\"image_url\":\"y\"}",
                mapper.writeValueAsString(image));
    }

    @Test
    void dynamicToolCallResultDefaultsEmptyContent() throws Exception {
        var json = "{\"success\":false}";
        var result = mapper.readValue(json, DynamicToolCallResult.class);
        assertFalse(result.success());
        assertTrue(result.content().isEmpty());
    }

    @Test
    void dynamicToolCallResultRoundtripWithContent() throws Exception {
        var result = new DynamicToolCallResult(true,
                List.of(new DynamicToolCallContent.InputText("done")));

        var json = mapper.writeValueAsString(result);
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"text\":\"done\""));

        var back = mapper.readValue(json, DynamicToolCallResult.class);
        assertTrue(back.success());
        assertEquals(1, back.content().size());
    }

    @Test
    void turnEnvironmentParamsRoundtrip() throws Exception {
        var env = new TurnEnvironmentParams("local", "/workspace");

        var json = mapper.writeValueAsString(env);
        var back = mapper.readValue(json, TurnEnvironmentParams.class);
        assertEquals("local", back.environmentId());
        assertEquals("/workspace", back.cwd());

        // Verify deserialization from spec shape
        var fromSpec = "{\"environment_id\":\"local\",\"cwd\":\"/workspace\"}";
        var parsed = mapper.readValue(fromSpec, TurnEnvironmentParams.class);
        assertEquals("local", parsed.environmentId());
        assertEquals("/workspace", parsed.cwd());
    }

    @Test
    void runtimeCapabilitiesSerializesExpectedShape() throws Exception {
        var caps = new RuntimeCapabilities(true, true, true, true, true, false, false, false);

        var tree = mapper.valueToTree(caps);
        assertTrue(tree.get("threads").asBoolean());
        assertFalse(tree.get("external_tools").asBoolean());
        assertTrue(tree.has("worker_runtime"));
        assertTrue(tree.has("turn_steer"));
    }

    @Test
    void runtimeEventEnvelopeSchemaVersionDefault() throws Exception {
        var json = "{" +
                "\"seq\":1," +
                "\"event\":\"test\"," +
                "\"kind\":\"test\"," +
                "\"thread_id\":\"thr_1\"," +
                "\"timestamp\":\"2026-06-12T00:00:00Z\"," +
                "\"payload\":{}" +
                "}";
        var envelope = mapper.readValue(json, RuntimeEventEnvelope.class);
        assertEquals(1, envelope.schemaVersion());
        assertEquals("thr_1", envelope.threadId());
    }

    @Test
    void runtimeEventEnvelopeFullRoundtrip() throws Exception {
        var payload = mapper.createObjectNode().put("message", "hello");
        var envelope = new RuntimeEventEnvelope(
                1, 42L, "response_delta", "text",
                "thr_1", "turn_1", "item_1",
                "2026-06-12T00:00:00Z", "2026-06-12T00:00:00Z",
                payload, Map.of());

        var json = mapper.writeValueAsString(envelope);
        assertTrue(json.contains("\"schema_version\":1"));
        assertTrue(json.contains("\"seq\":42"));
        assertTrue(json.contains("\"event\":\"response_delta\""));
        assertTrue(json.contains("\"kind\":\"text\""));

        var back = mapper.readValue(json, RuntimeEventEnvelope.class);
        assertEquals(1, back.schemaVersion());
        assertEquals(42L, back.seq());
        assertEquals("response_delta", back.event());
        assertEquals("hello", back.payload().get("message").asText());
    }

    @Test
    void runtimeExperimentalCapabilitiesDefaultsEnvironmentToFalse() throws Exception {
        var json = "{}";
        var caps = mapper.readValue(json, RuntimeExperimentalCapabilities.class);
        assertFalse(caps.environments());
    }
}
