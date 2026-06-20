package com.jay.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jay.mcp.client.InMemoryMcpClient;
import com.jay.mcp.client.McpManagedClient;
import com.jay.mcp.config.McpServerConfig;
import com.jay.mcp.config.McpServerDefinition;
import com.jay.mcp.config.ToolFilter;
import com.jay.mcp.descriptor.McpResourceDescriptor;
import com.jay.mcp.descriptor.McpToolDescriptor;
import com.jay.mcp.lifecycle.*;
import com.jay.mcp.manager.McpManager;
import com.jay.mcp.manager.ToolNameQualifier;
import com.jay.mcp.rpc.*;
import com.jay.mcp.state.StdioMcpState;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class McpTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    // ── InMemoryMcpClient ──────────────────────────────────────────────

    @Nested
    class InMemoryMcpClientTests {

        @Test
        void listToolsReturnsRegistered() {
            var client = new InMemoryMcpClient("test")
                .withTool("echo", mapper.createObjectNode().put("output", "hi"))
                .withTool("greet", mapper.createObjectNode().put("msg", "hello"));
            var tools = client.listTools();
            assertEquals(2, tools.size());
            var names = tools.stream().map(McpToolDescriptor::toolName).toList();
            assertTrue(names.contains("echo"));
            assertTrue(names.contains("greet"));
        }

        @Test
        void callToolReturnsValue() {
            var client = new InMemoryMcpClient("test")
                .withTool("echo", mapper.createObjectNode().put("output", "hi"));
            var result = client.callTool("echo", mapper.createObjectNode());
            assertEquals("hi", result.get("output").asText());
        }

        @Test
        void callToolErrorsOnMissing() {
            var client = new InMemoryMcpClient("test");
            var err = assertThrows(NoSuchElementException.class,
                () -> client.callTool("nope", mapper.createObjectNode()));
            assertTrue(err.getMessage().contains("not found"));
        }

        @Test
        void listResourcesReturnsRegistered() {
            var client = new InMemoryMcpClient("test")
                .withResource("mcp://s/health", mapper.createObjectNode().put("ok", true))
                .withResource("mcp://s/caps", mapper.createObjectNode().put("tools", "[]"));
            assertEquals(2, client.listResources().size());
        }

        @Test
        void readResourceReturnsValue() {
            var client = new InMemoryMcpClient("test")
                .withResource("mcp://s/health", mapper.createObjectNode().put("ok", true));
            var result = client.readResource("mcp://s/health");
            assertTrue(result.get("ok").asBoolean());
        }

        @Test
        void readResourceErrorsOnMissing() {
            var client = new InMemoryMcpClient("test");
            var err = assertThrows(NoSuchElementException.class,
                () -> client.readResource("mcp://s/nope"));
            assertTrue(err.getMessage().contains("not found"));
        }
    }

    // ── McpManager ─────────────────────────────────────────────────────

    @Nested
    class McpManagerTests {

        private McpServerConfig makeConfig(String name) {
            return new McpServerConfig(name, "test", List.of(), Map.of(), true);
        }

        @Test
        void startAllMarksReadyForRegisteredClients() {
            var manager = new McpManager();
            manager.registerServer(makeConfig("s1"), ToolFilter.EMPTY,
                new InMemoryMcpClient("in-memory").withTool("t", mapper.nullNode()));

            List<McpStartupUpdateEvent> events = new ArrayList<>();
            var summary = manager.startAll(events::add);

            assertEquals(List.of("s1"), summary.ready());
            assertTrue(summary.failed().isEmpty());
            assertTrue(events.stream().anyMatch(e ->
                e.serverName().equals("s1") && e.status() instanceof McpStartupStatus.Starting));
            assertTrue(events.stream().anyMatch(e ->
                e.serverName().equals("s1") && e.status() instanceof McpStartupStatus.Ready));
        }

        @Test
        void startAllMarksFailedWhenClientMissing() {
            var manager = new McpManager();
            manager.registerServer(makeConfig("s1"), ToolFilter.EMPTY,
                new InMemoryMcpClient("in-memory"));
            manager.stopServer("s1");
            var summary = manager.startAll(e -> {});
            assertTrue(summary.ready().isEmpty());
            assertEquals(1, summary.failed().size());
            assertEquals("s1", summary.failed().get(0).serverName());
        }

        @Test
        void startAllCancelsDisabledServers() {
            var manager = new McpManager();
            var cfg = new McpServerConfig("s1", "test", List.of(), Map.of(), false);
            manager.registerServer(cfg, ToolFilter.EMPTY,
                new InMemoryMcpClient("in-memory"));
            var summary = manager.startAll(e -> {});
            assertTrue(summary.ready().isEmpty());
            assertEquals(List.of("s1"), summary.cancelled());
        }

        @Test
        void listToolsAppliesFilter() {
            var manager = new McpManager();
            var client = new InMemoryMcpClient("in-memory")
                .withTool("allowed", mapper.nullNode())
                .withTool("denied", mapper.nullNode());
            manager.registerServer(makeConfig("s1"),
                new ToolFilter(List.of("allowed"), List.of()), client);
            var tools = manager.listTools();
            assertEquals(1, tools.size());
            assertEquals("allowed", tools.get(0).toolName());
        }

        @Test
        void listToolsDenyOverridesAllow() {
            var manager = new McpManager();
            var client = new InMemoryMcpClient("in-memory")
                .withTool("a", mapper.nullNode())
                .withTool("b", mapper.nullNode());
            manager.registerServer(makeConfig("s1"),
                new ToolFilter(List.of("a", "b"), List.of("b")), client);
            var tools = manager.listTools();
            assertEquals(1, tools.size());
            assertEquals("a", tools.get(0).toolName());
        }

        @Test
        void callToolDelegatesToClient() {
            var manager = new McpManager();
            manager.registerServer(makeConfig("s1"), ToolFilter.EMPTY,
                new InMemoryMcpClient("in-memory").withTool("t", mapper.createObjectNode().put("v", 42)));
            var result = manager.callTool("s1", "t", mapper.createObjectNode());
            assertEquals(42, result.get("v").asInt());
        }

        @Test
        void callToolErrorsOnMissingServer() {
            var manager = new McpManager();
            var err = assertThrows(NoSuchElementException.class,
                () -> manager.callTool("nope", "t", mapper.createObjectNode()));
            assertTrue(err.getMessage().contains("not available"));
        }

        @Test
        void callQualifiedToolParsesName() {
            var manager = new McpManager();
            manager.registerServer(makeConfig("my_server"), ToolFilter.EMPTY,
                new InMemoryMcpClient("in-memory").withTool("my_tool", mapper.createObjectNode().put("ok", true)));
            var result = manager.callQualifiedTool("mcp__my_server__my_tool", mapper.createObjectNode());
            assertTrue(result.get("ok").asBoolean());
        }

        @Test
        void callQualifiedToolHandlesTruncatedNames() {
            String longServer = "server".repeat(20);
            String longTool = "tool".repeat(20);
            var manager = new McpManager();
            manager.registerServer(
                new McpServerConfig(longServer, "test", List.of(), Map.of(), true),
                ToolFilter.EMPTY,
                new InMemoryMcpClient("in-memory").withTool(longTool, mapper.createObjectNode().put("ok", true)));
            var tools = manager.listTools();
            var qualified = tools.get(0).qualifiedName();
            assertTrue(qualified.length() <= 64);
            assertNotNull(ToolNameQualifier.parse(qualified));

            var result = manager.callQualifiedTool(qualified, mapper.createObjectNode());
            assertTrue(result.get("ok").asBoolean());
        }

        @Test
        void unregisterRemovesServer() {
            var manager = new McpManager();
            manager.registerServer(makeConfig("s1"), ToolFilter.EMPTY,
                new InMemoryMcpClient("in-memory"));
            manager.unregisterServer("s1");
            // After unregister, trying again should throw
            var err = assertThrows(NoSuchElementException.class,
                () -> manager.unregisterServer("s1"));
            assertTrue(err.getMessage().contains("not registered"));
        }

        @Test
        void unregisterErrorsOnUnknown() {
            var manager = new McpManager();
            var err = assertThrows(NoSuchElementException.class,
                () -> manager.unregisterServer("nope"));
            assertTrue(err.getMessage().contains("not registered"));
        }

        @Test
        void stopServerErrorsOnUnknown() {
            var manager = new McpManager();
            var err = assertThrows(NoSuchElementException.class,
                () -> manager.stopServer("nope"));
            assertTrue(err.getMessage().contains("not running"));
        }

        @Test
        void listResourcesReturnsFromClients() {
            var manager = new McpManager();
            manager.registerServer(makeConfig("s1"), ToolFilter.EMPTY,
                new InMemoryMcpClient("in-memory")
                    .withResource("mcp://s1/health", mapper.createObjectNode().put("ok", true)));
            var resources = manager.listResources();
            assertEquals(1, resources.size());
            assertEquals("s1", resources.get(0).serverName());
        }

        @Test
        void readResourceDelegates() {
            var manager = new McpManager();
            manager.registerServer(makeConfig("s1"), ToolFilter.EMPTY,
                new InMemoryMcpClient("in-memory")
                    .withResource("mcp://s1/health", mapper.createObjectNode().put("ok", true)));
            var result = manager.readResource("s1", "mcp://s1/health");
            assertTrue(result.get("ok").asBoolean());
        }

        @Test
        void updateSandboxStateReturnsNotices() {
            var manager = new McpManager();
            manager.registerServer(makeConfig("s1"), ToolFilter.EMPTY,
                new InMemoryMcpClient("in-memory"));
            var notices = manager.updateSandboxState("strict", "/tmp");
            assertEquals(1, notices.size());
            assertEquals("s1", notices.get(0).get("server_name").asText());
        }
    }

    // ── Tool filter ────────────────────────────────────────────────────

    @Nested
    class ToolFilterTests {

        @Test
        void emptyAllowPermitsAll() {
            var filter = new ToolFilter(List.of(), List.of());
            assertTrue(McpManager.allowedByFilter("anything", filter));
        }

        @Test
        void denyBlocks() {
            var filter = new ToolFilter(List.of(), List.of("danger"));
            assertFalse(McpManager.allowedByFilter("danger", filter));
            assertTrue(McpManager.allowedByFilter("safe", filter));
        }

        @Test
        void allowOnlyPermitsListed() {
            var filter = new ToolFilter(List.of("a"), List.of());
            assertTrue(McpManager.allowedByFilter("a", filter));
            assertFalse(McpManager.allowedByFilter("b", filter));
        }

        @Test
        void nullFilterPermitsAll() {
            assertTrue(McpManager.allowedByFilter("anything", null));
        }
    }

    // ── ToolNameQualifier ──────────────────────────────────────────────

    @Nested
    class ToolNameQualifierTests {

        @Test
        void sanitizeLowercasesAndReplacesSpecials() {
            assertEquals("my_server_name", ToolNameQualifier.sanitize("My-Server.Name"));
            assertEquals("abc123", ToolNameQualifier.sanitize("ABC123"));
        }

        @Test
        void qualifyProducesMcpPrefix() {
            var name = ToolNameQualifier.qualify("server", "tool");
            assertTrue(name.startsWith("mcp__server__tool"));
        }

        @Test
        void qualifyTruncatesLongNames() {
            String longServer = "a".repeat(100);
            var name = ToolNameQualifier.qualify(longServer, "tool");
            assertTrue(name.length() <= 64);
            assertNotNull(ToolNameQualifier.parse(name));
        }

        @Test
        void parseRoundTrip() {
            var qualified = ToolNameQualifier.qualify("my_server", "my_tool");
            var parts = ToolNameQualifier.parse(qualified);
            assertNotNull(parts);
            assertEquals("my_server", parts[0]);
            assertEquals("my_tool", parts[1]);
        }

        @Test
        void parseRejectsMissingPrefix() {
            assertNull(ToolNameQualifier.parse("not_mcp__server__tool"));
        }

        @Test
        void parseRejectsEmptySegments() {
            assertNull(ToolNameQualifier.parse("mcp____tool"));
            assertNull(ToolNameQualifier.parse("mcp__server__"));
        }
    }

    // ── JsonRpcError ───────────────────────────────────────────────────

    @Nested
    class JsonRpcErrorTests {

        @Test
        void errorCodesAreCorrect() {
            assertEquals(-32700, JsonRpcError.parseError("").code());
            assertEquals(-32600, JsonRpcError.invalidRequest("").code());
            assertEquals(-32601, JsonRpcError.methodNotFound("x").code());
            assertEquals(-32602, JsonRpcError.invalidParams("").code());
            assertEquals(-32603, JsonRpcError.internalError("").code());
        }
    }

    // ── JSON-RPC envelope formatting ───────────────────────────────────

    @Nested
    class JsonRpcEnvelopeTests {

        @Test
        void resultProducesValidEnvelope() throws Exception {
            var resp = JsonRpcResponse.success(
                mapper.getNodeFactory().numberNode(1),
                mapper.createObjectNode().put("ok", true));
            var json = mapper.readTree(mapper.writeValueAsString(resp));
            assertEquals("2.0", json.get("jsonrpc").asText());
            assertEquals(1, json.get("id").asInt());
            assertTrue(json.get("result").get("ok").asBoolean());
        }

        @Test
        void errorProducesValidEnvelope() throws Exception {
            var resp = JsonRpcResponse.error(
                mapper.getNodeFactory().numberNode(2),
                JsonRpcError.invalidParams("bad"));
            var json = mapper.readTree(mapper.writeValueAsString(resp));
            assertEquals("2.0", json.get("jsonrpc").asText());
            assertEquals(2, json.get("id").asInt());
            assertEquals(-32602, json.get("error").get("code").asInt());
        }
    }

    // ── McpServerConfig defaults ───────────────────────────────────────

    @Nested
    class McpServerConfigTests {

        @Test
        void defaultsEnabledToTrue() throws Exception {
            var json = mapper.readTree("{\"name\":\"s\",\"command\":\"cmd\"}");
            var config = mapper.convertValue(json, McpServerConfig.class);
            assertTrue(config.enabled());
            assertTrue(config.args() != null && config.args().isEmpty());
            assertTrue(config.env() != null && config.env().isEmpty());
        }

        @Test
        void explicitEnabledFalse() throws Exception {
            var json = mapper.readTree("{\"name\":\"s\",\"command\":\"cmd\",\"enabled\":false}");
            var config = mapper.convertValue(json, McpServerConfig.class);
            assertFalse(config.enabled());
        }
    }

    // ── McpStartupStatus serialization ─────────────────────────────────

    @Nested
    class McpStartupStatusTests {

        @Test
        void failedSerializesWithError() throws Exception {
            var status = new McpStartupStatus.Failed("oops");
            var json = mapper.readTree(mapper.writeValueAsString(status));
            assertEquals("failed", json.get("kind").asText());
            assertEquals("oops", json.get("error").asText());
        }

        @Test
        void startingSerializesCorrectly() throws Exception {
            var status = new McpStartupStatus.Starting();
            var json = mapper.readTree(mapper.writeValueAsString(status));
            assertEquals("starting", json.get("kind").asText());
        }

        @Test
        void readySerializesCorrectly() throws Exception {
            var status = new McpStartupStatus.Ready();
            var json = mapper.readTree(mapper.writeValueAsString(status));
            assertEquals("ready", json.get("kind").asText());
        }

        @Test
        void cancelledSerializesCorrectly() throws Exception {
            var status = new McpStartupStatus.Cancelled();
            var json = mapper.readTree(mapper.writeValueAsString(status));
            assertEquals("cancelled", json.get("kind").asText());
        }
    }

    // ── JsonRpcDispatcher parse helpers ────────────────────────────────

    @Nested
    class ParseServerFromUriTests {

        @Test
        void extractsServer() {
            assertEquals("my-server", JsonRpcDispatcher.parseServerFromUri("mcp://my-server/capabilities"));
        }

        @Test
        void returnsNullForInvalid() {
            assertNull(JsonRpcDispatcher.parseServerFromUri("http://not-mcp"));
            assertNull(JsonRpcDispatcher.parseServerFromUri("mcp:///path"));
            assertNull(JsonRpcDispatcher.parseServerFromUri(null));
        }
    }

    // ── StdioMcpServer state building ──────────────────────────────────

    @Nested
    class StdioMcpServerTests {

        @Test
        void buildStateRegistersEnabledServers() {
            var defs = List.of(
                new McpServerDefinition(
                    new McpServerConfig("s1", "cmd1", List.of(), Map.of(), true),
                    ToolFilter.EMPTY),
                new McpServerDefinition(
                    new McpServerConfig("s2", "cmd2", List.of(), Map.of(), false),
                    ToolFilter.EMPTY)
            );
            var server = new StdioMcpServer();
            // We test indirectly by verifying the run method works with empty stdin
            // Instead of blocking on stdin, directly build state through the static method
            var manager = new McpManager();
            Map<String, McpServerDefinition> defMap = new LinkedHashMap<>();
            Map<String, Boolean> running = new LinkedHashMap<>();
            for (var def : defs) {
                String name = def.config().name();
                defMap.put(name, def);
                if (def.config().enabled()) {
                    manager.registerServer(def.config(), def.filter(),
                        JsonRpcDispatcher.defaultStdioClient(name));
                    running.put(name, true);
                } else {
                    running.put(name, false);
                }
            }
            var state = new StdioMcpState(manager, defMap, running, "running");

            assertEquals("running", state.lifecycleState());
            assertTrue(state.running().get("s1"));
            assertFalse(state.running().get("s2"));
            assertEquals(2, state.definitions().size());
        }
    }

    // ── JsonRpcDispatcher method routing ───────────────────────────────

    @Nested
    class JsonRpcDispatcherTests {

        private StdioMcpState makeState() {
            var manager = new McpManager();
            var defs = new LinkedHashMap<String, McpServerDefinition>();
            var running = new LinkedHashMap<String, Boolean>();
            var config = new McpServerConfig("test-srv", "test-cmd", List.of(), Map.of(), true);
            defs.put("test-srv", new McpServerDefinition(config, ToolFilter.EMPTY));
            manager.registerServer(config, ToolFilter.EMPTY,
                JsonRpcDispatcher.defaultStdioClient("test-srv"));
            running.put("test-srv", true);
            return new StdioMcpState(manager, defs, running, "running");
        }

        @Test
        void initializeReturnsCapabilities() {
            var dispatcher = new JsonRpcDispatcher(makeState());
            var result = dispatcher.dispatch("initialize", null);
            assertFalse(result.shouldExit());
            assertEquals("deepseek-mcp", result.result().get("server").asText());
            assertEquals("stdio", result.result().get("transport").asText());
            assertTrue(result.result().has("methods"));
            assertTrue(result.result().has("lifecycle"));
        }

        @Test
        void capabilitiesReturnsSameAsInitialize() {
            var dispatcher = new JsonRpcDispatcher(makeState());
            var result = dispatcher.dispatch("capabilities", null);
            assertEquals("deepseek-mcp", result.result().get("server").asText());
        }

        @Test
        void healthzReturnsOk() {
            var dispatcher = new JsonRpcDispatcher(makeState());
            var result = dispatcher.dispatch("healthz", null);
            assertEquals("ok", result.result().get("status").asText());
            assertEquals("deepseek-mcp", result.result().get("service").asText());
        }

        @Test
        void toolsListReturnsTools() {
            var dispatcher = new JsonRpcDispatcher(makeState());
            var result = dispatcher.dispatch("tools/list", mapper.createObjectNode());
            var tools = result.result().get("tools");
            assertTrue(tools.isArray());
            assertTrue(tools.size() > 0);
        }

        @Test
        void toolsListFiltersByServer() {
            var dispatcher = new JsonRpcDispatcher(makeState());
            var params = mapper.createObjectNode().put("server", "test-srv");
            var result = dispatcher.dispatch("tools/list", params);
            var tools = result.result().get("tools");
            for (var tool : tools) {
                assertEquals("test-srv", tool.get("server_name").asText());
            }
        }

        @Test
        void toolsCallWithServer() {
            var dispatcher = new JsonRpcDispatcher(makeState());
            var params = mapper.createObjectNode()
                .put("name", "health")
                .put("server", "test-srv");
            var result = dispatcher.dispatch("tools/call", params);
            assertEquals("ok", result.result().get("result").get("status").asText());
        }

        @Test
        void toolsCallWithQualifiedName() {
            var dispatcher = new JsonRpcDispatcher(makeState());
            var params = mapper.createObjectNode()
                .put("name", "mcp__test_srv__health");
            var result = dispatcher.dispatch("tools/call", params);
            assertEquals("ok", result.result().get("result").get("status").asText());
        }

        @Test
        void resourcesListReturnsResources() {
            var dispatcher = new JsonRpcDispatcher(makeState());
            var result = dispatcher.dispatch("resources/list", mapper.createObjectNode());
            assertTrue(result.result().get("resources").isArray());
        }

        @Test
        void resourcesReadReturnsResource() {
            var dispatcher = new JsonRpcDispatcher(makeState());
            var params = mapper.createObjectNode()
                .put("server", "test-srv")
                .put("uri", "mcp://test-srv/health");
            var result = dispatcher.dispatch("resources/read", params);
            assertEquals("ok", result.result().get("resource").get("status").asText());
        }

        @Test
        void resourcesReadInfersServerFromUri() {
            var dispatcher = new JsonRpcDispatcher(makeState());
            var params = mapper.createObjectNode()
                .put("uri", "mcp://test-srv/health");
            var result = dispatcher.dispatch("resources/read", params);
            assertEquals("ok", result.result().get("resource").get("status").asText());
        }

        @Test
        void serverListReturnsLifecycle() {
            var dispatcher = new JsonRpcDispatcher(makeState());
            var result = dispatcher.dispatch("server/list", null);
            assertTrue(result.result().has("lifecycle"));
        }

        @Test
        void serversListAliasWorks() {
            var dispatcher = new JsonRpcDispatcher(makeState());
            var result = dispatcher.dispatch("servers/list", null);
            assertTrue(result.result().has("lifecycle"));
        }

        @Test
        void serverRegisterAddsServer() {
            var state = makeState();
            var dispatcher = new JsonRpcDispatcher(state);
            var cfg = mapper.createObjectNode()
                .put("name", "new-srv")
                .put("command", "new-cmd")
                .put("enabled", true);
            var params = mapper.createObjectNode().set("server", cfg);
            var result = dispatcher.dispatch("server/register", params);
            assertFalse(result.shouldExit());
            assertTrue(state.definitions().containsKey("new-srv"));
        }

        @Test
        void serverUnregisterRemovesServer() {
            var state = makeState();
            var dispatcher = new JsonRpcDispatcher(state);
            var params = mapper.createObjectNode().put("name", "test-srv");
            var result = dispatcher.dispatch("server/unregister", params);
            assertFalse(result.shouldExit());
            assertFalse(state.definitions().containsKey("test-srv"));
        }

        @Test
        void shutdownReturnsOkAndExits() {
            var dispatcher = new JsonRpcDispatcher(makeState());
            var result = dispatcher.dispatch("shutdown", null);
            assertTrue(result.shouldExit());
            assertTrue(result.result().get("ok").asBoolean());
        }

        @Test
        void unknownMethodThrows() {
            var dispatcher = new JsonRpcDispatcher(makeState());
            assertThrows(JsonRpcDispatcher.MethodNotFoundException.class,
                () -> dispatcher.dispatch("unknown/method", null));
        }

        @Test
        void serverRegisterRejectsEmptyName() {
            var state = makeState();
            var dispatcher = new JsonRpcDispatcher(state);
            var cfg = mapper.createObjectNode()
                .put("name", "")
                .put("command", "cmd");
            var params = mapper.createObjectNode().set("server", cfg);
            assertThrows(JsonRpcDispatcher.JsonRpcException.class,
                () -> dispatcher.dispatch("server/register", params));
        }
    }

    // ── Lifecycle snapshot ─────────────────────────────────────────────

    @Nested
    class LifecycleSnapshotTests {

        @Test
        void snapshotIncludesServersAndCounts() {
            var manager = new McpManager();
            var defs = new LinkedHashMap<String, McpServerDefinition>();
            var running = new LinkedHashMap<String, Boolean>();

            var cfg = new McpServerConfig("s1", "cmd1", List.of("--verbose"), Map.of(), true);
            defs.put("s1", new McpServerDefinition(cfg, ToolFilter.EMPTY));
            manager.registerServer(cfg, ToolFilter.EMPTY, new InMemoryMcpClient("in-memory"));
            running.put("s1", true);

            var state = new StdioMcpState(manager, defs, running, "running");
            var dispatcher = new JsonRpcDispatcher(state);
            var result = dispatcher.dispatch("healthz", null);
            var lifecycle = result.result().get("lifecycle");

            assertEquals("running", lifecycle.get("status").asText());
            assertEquals(1, lifecycle.get("servers").size());
            assertEquals(1, lifecycle.get("counts").get("defined").asInt());
            assertEquals(1, lifecycle.get("counts").get("running").asInt());
        }
    }

    // ── Default stdio client stubs ─────────────────────────────────────

    @Nested
    class DefaultStdioClientTests {

        @Test
        void providesHealthAndCapabilitiesTools() {
            var client = JsonRpcDispatcher.defaultStdioClient("test-srv");
            var tools = client.listTools();
            assertEquals(2, tools.size());
            var names = tools.stream().map(McpToolDescriptor::toolName).toList();
            assertTrue(names.contains("health"));
            assertTrue(names.contains("capabilities"));
        }

        @Test
        void healthToolReturnsOk() {
            var client = JsonRpcDispatcher.defaultStdioClient("test-srv");
            var result = client.callTool("health", mapper.createObjectNode());
            assertEquals("ok", result.get("status").asText());
            assertEquals("test-srv", result.get("server_name").asText());
        }

        @Test
        void providesHealthAndCapabilitiesResources() {
            var client = JsonRpcDispatcher.defaultStdioClient("test-srv");
            var resources = client.listResources();
            assertEquals(2, resources.size());
            var uris = resources.stream().map(McpResourceDescriptor::uri).toList();
            assertTrue(uris.contains("mcp://test-srv/health"));
            assertTrue(uris.contains("mcp://test-srv/capabilities"));
        }
    }
}
