package com.jay.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.jay.protocol.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for tools module — ported from Rust tools crate tests.
 */
class ToolsTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    // ── ToolResult tests ─────────────────────────────────────

    @Test
    void toolResultSuccessSetsPlainContent() {
        var result = ToolResult.success("operation completed successfully");
        assertTrue(result.success());
        assertEquals("operation completed successfully", result.content());
        assertNull(result.metadata());
    }

    @Test
    void toolResultJsonRoundTripsContent() throws Exception {
        var node = mapper.createObjectNode().put("ok", true);
        var result = ToolResult.json(node);
        assertTrue(result.success());
        assertTrue(result.content().contains("\"ok\" : true"));
    }

    @Test
    void toolResultWithMetadata() {
        var meta = JsonNodeFactory.instance.objectNode().put("elapsed_ms", 42);
        var result = ToolResult.success("done").withMetadata(meta);
        assertEquals(42, result.metadata().get("elapsed_ms").asInt());
    }

    // ── ToolError tests ──────────────────────────────────────

    @Test
    void toolErrorMissingField() {
        var err = ToolError.missingField("path");
        assertInstanceOf(ToolError.MissingField.class, err);
        assertEquals("path", ((ToolError.MissingField) err).field());
        assertTrue(err.message().contains("missing required field 'path'"));
    }

    @Test
    void toolErrorInvalidInput() {
        var err = ToolError.invalidInput("test invalid message");
        assertInstanceOf(ToolError.InvalidInput.class, err);
        assertEquals("test invalid message", ((ToolError.InvalidInput) err).message());
    }

    @Test
    void toolErrorExecutionFailed() {
        var err = ToolError.executionFailed("process crashed");
        assertInstanceOf(ToolError.ExecutionFailed.class, err);
        assertTrue(err.message().contains("process crashed"));
    }

    @Test
    void toolErrorNotAvailable() {
        var err = ToolError.notAvailable("custom tool not found");
        assertInstanceOf(ToolError.NotAvailable.class, err);
        assertTrue(err.message().contains("custom tool not found"));
    }

    @Test
    void toolErrorPermissionDenied() {
        var err = ToolError.permissionDenied("unauthorized user");
        assertInstanceOf(ToolError.PermissionDenied.class, err);
        assertTrue(err.message().contains("unauthorized user"));
    }

    @Test
    void toolErrorPathEscape() {
        var err = ToolError.pathEscape("../outside");
        assertInstanceOf(ToolError.PathEscape.class, err);
        assertTrue(err.message().contains("../outside"));
        assertTrue(err.message().contains("path escapes workspace"));
    }

    @Test
    void toolErrorTimeout() {
        var err = new ToolError.Timeout(30);
        assertTrue(err.message().contains("30s"));
    }

    // ── Input extractor tests ────────────────────────────────

    @Test
    void requiredStrExtractsCorrectly() {
        var input = mapper.createObjectNode().put("name", "demo");
        assertEquals("demo", InputExtractors.requiredStr(input, "name"));
    }

    @Test
    void requiredStrReportsProvidedFieldsOnMissing() {
        var input = mapper.createObjectNode()
                .put("path", "src/lib.rs")
                .put("content", "new body");
        try {
            InputExtractors.requiredStr(input, "replace");
            fail("expected ToolErrorException");
        } catch (ToolError.ToolErrorException e) {
            var msg = e.getMessage();
            assertTrue(msg.contains("missing required field 'replace'"));
            assertTrue(msg.contains("Input provided:"));
            assertTrue(msg.contains("path"));
            assertTrue(msg.contains("content"));
        }
    }

    @Test
    void optionalStrReturnsEmptyForMissing() {
        var input = mapper.createObjectNode().put("count", 7);
        assertTrue(InputExtractors.optionalStr(input, "missing").isEmpty());
        assertTrue(InputExtractors.optionalStr(input, "count").isEmpty()); // not textual
        var nullNode = mapper.createObjectNode().putNull("name");
        assertTrue(InputExtractors.optionalStr(nullNode, "name").isEmpty());
    }

    @Test
    void requiredU64RejectsMissingOrNonInteger() {
        assertThrows(ToolError.ToolErrorException.class,
                () -> InputExtractors.requiredU64(mapper.createObjectNode(), "count"));

        assertEquals(42, InputExtractors.requiredU64(
                mapper.createObjectNode().put("count", 42), "count"));

        assertEquals(Long.MAX_VALUE, InputExtractors.requiredU64(
                mapper.createObjectNode().put("count", Long.MAX_VALUE), "count"));

        // Negative values should be rejected
        assertThrows(ToolError.ToolErrorException.class,
                () -> InputExtractors.requiredU64(
                        mapper.createObjectNode().put("count", -1), "count"));
    }

    @Test
    void optionalU64UsesDefault() {
        var input = mapper.createObjectNode().put("count", 7).put("enabled", true);
        assertEquals(7, InputExtractors.optionalU64(input, "count", 0));
        assertEquals(0, InputExtractors.optionalU64(input, "missing", 0));
    }

    @Test
    void optionalBoolUsesDefault() {
        var input = mapper.createObjectNode().put("enabled", true);
        assertTrue(InputExtractors.optionalBool(input, "enabled", false));
        assertFalse(InputExtractors.optionalBool(input, "missing", false));
        // Non-boolean returns default
        assertFalse(InputExtractors.optionalBool(
                mapper.createObjectNode().put("enabled", 1), "enabled", false));
    }

    // ── ToolCall execution_subject tests ────────────────────

    @Test
    void toolCallExecutionSubjectUsesLocalShellCommandAndCwd() {
        var params = new LocalShellParams("ls -l", "/custom/dir", null);
        var payload = new ToolPayload.LocalShell(params);
        var call = new ToolCall("shell", payload, ToolCallSource.DIRECT, null);

        var subject = call.executionSubject("/fallback/dir");
        assertEquals("ls -l", subject.command());
        assertEquals("/custom/dir", subject.cwd());
        assertEquals("shell", subject.kind());
    }

    @Test
    void toolCallExecutionSubjectFallsBackForShellWithoutCwd() {
        var params = new LocalShellParams("echo hello", null, null);
        var payload = new ToolPayload.LocalShell(params);
        var call = new ToolCall("shell", payload, ToolCallSource.DIRECT, null);

        var subject = call.executionSubject("/fallback/dir");
        assertEquals("echo hello", subject.command());
        assertEquals("/fallback/dir", subject.cwd());
        assertEquals("shell", subject.kind());
    }

    @Test
    void toolCallExecutionSubjectUsesToolNameForNonShellPayloads() {
        var payload = new ToolPayload.Function("{}");
        var call = new ToolCall("my_tool", payload, ToolCallSource.DIRECT, null);

        var subject = call.executionSubject("/fallback/dir");
        assertEquals("my_tool", subject.command());
        assertEquals("/fallback/dir", subject.cwd());
        assertEquals("tool", subject.kind());
    }

    // ── ToolRegistry tests ──────────────────────────────────

    @Test
    void registryRegisterAndListSpecs() {
        var registry = new ToolRegistry();
        var schema = mapper.createObjectNode().put("type", "object");
        var spec = new ToolSpec("test-tool", schema, schema, false, null);
        var handler = new TestToolHandler();

        registry.register(spec, handler);
        var specs = registry.listSpecs();
        assertEquals(1, specs.size());
        assertEquals("test-tool", specs.get(0).spec().name());
    }

    @Test
    void registryDispatchResolvesCorrectHandler() {
        var registry = new ToolRegistry();
        var schema = mapper.createObjectNode().put("type", "object");
        var handler = new TestToolHandler("test-output");
        registry.register(new ToolSpec("test-tool", schema, schema, false, null), handler);

        var payload = new ToolPayload.Function("{}");
        var call = new ToolCall("test-tool", payload, ToolCallSource.DIRECT, "call-1");
        var output = registry.dispatch(call, true);

        assertInstanceOf(ToolOutput.Function.class, output);
        var funcOut = (ToolOutput.Function) output;
        assertTrue(funcOut.success());
    }

    @Test
    void registryDispatchThrowsOnUnknownTool() {
        var registry = new ToolRegistry();
        var call = new ToolCall("nonexistent",
                new ToolPayload.Function("{}"), ToolCallSource.DIRECT, null);

        assertThrows(FunctionCallError.FceException.class,
                () -> registry.dispatch(call, true));
    }

    @Test
    void registryEnforcesMutatingGuard() {
        var registry = new ToolRegistry();
        var schema = mapper.createObjectNode().put("type", "object");
        var handler = new MutatingToolHandler();
        registry.register(new ToolSpec("dangerous", schema, schema, false, null), handler);

        var call = new ToolCall("dangerous",
                new ToolPayload.Function("{}"), ToolCallSource.DIRECT, null);

        assertThrows(FunctionCallError.FceException.class,
                () -> registry.dispatch(call, false));
    }

    // ── test handlers ───────────────────────────────────────

    static class TestToolHandler implements ToolHandler {
        private final String outputContent;
        TestToolHandler() { this("test-output"); }
        TestToolHandler(String content) { this.outputContent = content; }

        @Override public ToolKind kind() { return ToolKind.FUNCTION; }
        @Override public ToolOutput handle(ToolInvocation inv) {
            return new ToolOutput.Function(
                    com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.textNode(outputContent), true);
        }
    }

    static class MutatingToolHandler implements ToolHandler {
        @Override public ToolKind kind() { return ToolKind.FUNCTION; }
        @Override public boolean isMutating() { return true; }
        @Override public ToolOutput handle(ToolInvocation inv) {
            return new ToolOutput.Function(
                    com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.textNode("ok"), true);
        }
    }
}
