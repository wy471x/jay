package com.jay.mcp.rpc;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.node.ArrayNode;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.jay.mcp.client.InMemoryMcpClient;

import com.jay.mcp.config.McpServerConfig;

import com.jay.mcp.config.McpServerDefinition;

import com.jay.mcp.config.ToolFilter;

import com.jay.mcp.descriptor.McpResourceDescriptor;

import com.jay.mcp.descriptor.McpToolDescriptor;

import com.jay.mcp.state.StdioMcpState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

public class JsonRpcDispatcher {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final StdioMcpState state;

    public JsonRpcDispatcher(StdioMcpState state) {

        this.state = state;

    }

    public DispatchResult dispatch(String method, JsonNode params) {

        return switch (method) {

            case "initialize", "capabilities" -> initCapabilities();

            case "healthz" -> healthz();

            case "tools/list" -> toolsList(coerceParams(params, ToolsListParams.class));

            case "tools/call" -> toolsCall(coerceParams(params, ToolsCallParams.class));

            case "resources/list" -> resourcesList(coerceParams(params, ResourcesListParams.class));

            case "resources/read" -> resourcesRead(coerceParams(params, ResourcesReadParams.class));

            case "server/list", "servers/list" -> serverList();

            case "server/register", "servers/register" -> serverRegister(coerceParams(params, ServerRegisterParams.class));

            case "server/start", "servers/start" -> serverStart(coerceParams(params, ServerNameParams.class));

            case "server/stop", "servers/stop" -> serverStop(coerceParams(params, ServerNameParams.class));

            case "server/unregister", "servers/unregister" -> serverUnregister(coerceParams(params, ServerNameParams.class));

            case "shutdown" -> shutdown();

            default -> throw new MethodNotFoundException(method);

        };

    }

    // --- RPC method implementations ---

    private DispatchResult initCapabilities() {

        ObjectNode result = MAPPER.createObjectNode();

        result.put("server", "deepseek-mcp");

        result.put("transport", "stdio");

        ArrayNode methods = MAPPER.createArrayNode();

        for (String m : defaultRpcMethods()) methods.add(m);

        result.set("methods", methods);

        result.set("lifecycle", lifecycleSnapshot());

        return new DispatchResult(result, false);

    }

    private DispatchResult healthz() {

        ObjectNode result = MAPPER.createObjectNode();

        result.put("status", "ok");

        result.put("service", "deepseek-mcp");

        result.put("transport", "stdio");

        result.set("lifecycle", lifecycleSnapshot());

        return new DispatchResult(result, false);

    }

    private DispatchResult toolsList(ToolsListParams p) {

        List<McpToolDescriptor> tools = state.manager().listTools();

        if (p.server() != null) {

            tools = tools.stream().filter(t -> t.serverName().equals(p.server())).toList();

        }

        ObjectNode result = MAPPER.createObjectNode();

        result.set("tools", MAPPER.valueToTree(tools));

        return new DispatchResult(result, false);

    }

    private DispatchResult toolsCall(ToolsCallParams p) {

        String toolName = p.effectiveName();

        if (toolName == null) throw new JsonRpcException(JsonRpcError.invalidParams("missing tool name"));

        JsonNode args = p.arguments() != null && !p.arguments().isNull() ? p.arguments() : MAPPER.createObjectNode();

        JsonNode callResult;

        if (toolName.startsWith("mcp__")) {

            callResult = state.manager().callQualifiedTool(toolName, args);

        } else {

            if (p.server() == null) throw new JsonRpcException(JsonRpcError.invalidParams("missing server for unqualified tool"));

            callResult = state.manager().callTool(p.server(), toolName, args);

        }

        ObjectNode result = MAPPER.createObjectNode();

        result.set("result", callResult);

        return new DispatchResult(result, false);

    }

    private DispatchResult resourcesList(ResourcesListParams p) {

        List<McpResourceDescriptor> resources = state.manager().listResources();

        if (p.server() != null) {

            resources = resources.stream().filter(r -> r.serverName().equals(p.server())).toList();

        }

        ObjectNode result = MAPPER.createObjectNode();

        result.set("resources", MAPPER.valueToTree(resources));

        return new DispatchResult(result, false);

    }

    private DispatchResult resourcesRead(ResourcesReadParams p) {

        String serverName = p.server();

        if (serverName == null) {

            serverName = parseServerFromUri(p.uri());

        }

        if (serverName == null) throw new JsonRpcException(JsonRpcError.invalidParams("missing server for resource read"));

        JsonNode value = state.manager().readResource(serverName, p.uri());

        ObjectNode result = MAPPER.createObjectNode();

        result.set("resource", value);

        return new DispatchResult(result, false);

    }

    private DispatchResult serverList() {

        ObjectNode result = MAPPER.createObjectNode();

        result.set("lifecycle", lifecycleSnapshot());

        return new DispatchResult(result, false);

    }

    private DispatchResult serverRegister(ServerRegisterParams p) {

        String name = p.server().name();

        if (name == null || name.trim().isEmpty()) {

            throw new JsonRpcException(JsonRpcError.invalidParams("server.name must not be empty"));

        }

        if (state.definitions().containsKey(name)) {

            try { state.manager().unregisterServer(name); } catch (NoSuchElementException ignored) { }

        }

        ToolFilter filter = p.filter() != null ? p.filter() : ToolFilter.EMPTY;

        McpServerDefinition def = new McpServerDefinition(p.server(), filter);

        state.definitions().put(name, def);

        boolean shouldRun = p.start() && p.server().enabled();

        if (shouldRun) {

            state.manager().registerServer(p.server(), filter, defaultStdioClient(name));

        }

        state.running().put(name, shouldRun);

        ObjectNode result = MAPPER.createObjectNode();

        result.set("lifecycle", lifecycleSnapshot());

        return new DispatchResult(result, false);

    }

    private DispatchResult serverStart(ServerNameParams p) {

        McpServerDefinition definition = state.definitions().get(p.name());

        if (definition == null) {

            throw new JsonRpcException(JsonRpcError.invalidParams("server '" + p.name() + "' is not defined"));

        }

        if (!definition.config().enabled()) {

            throw new JsonRpcException(JsonRpcError.invalidParams("server '" + p.name() + "' is disabled"));

        }

        if (!state.running().getOrDefault(p.name(), false)) {

            state.manager().registerServer(definition.config(), definition.filter(), defaultStdioClient(p.name()));

            state.running().put(p.name(), true);

        }

        ObjectNode result = MAPPER.createObjectNode();

        result.set("lifecycle", lifecycleSnapshot());

        return new DispatchResult(result, false);

    }

    private DispatchResult serverStop(ServerNameParams p) {

        if (state.running().getOrDefault(p.name(), false)) {

            state.manager().stopServer(p.name());

        }

        state.running().put(p.name(), false);

        ObjectNode result = MAPPER.createObjectNode();

        result.set("lifecycle", lifecycleSnapshot());

        return new DispatchResult(result, false);

    }

    private DispatchResult serverUnregister(ServerNameParams p) {

        if (!state.definitions().containsKey(p.name())) {

            throw new JsonRpcException(JsonRpcError.invalidParams("server '" + p.name() + "' is not defined"));

        }

        state.definitions().remove(p.name());

        try { state.manager().unregisterServer(p.name()); } catch (NoSuchElementException ignored) { }

        state.running().remove(p.name());

        ObjectNode result = MAPPER.createObjectNode();

        result.set("lifecycle", lifecycleSnapshot());

        return new DispatchResult(result, false);

    }

    private DispatchResult shutdown() {

        state.setLifecycleState("shutting_down");

        ObjectNode result = MAPPER.createObjectNode();

        result.put("ok", true);

        result.set("lifecycle", lifecycleSnapshot());

        state.setLifecycleState("stopped");

        return new DispatchResult(result, true);

    }

    // --- Helpers ---

    private JsonNode lifecycleSnapshot() {

        ArrayNode servers = MAPPER.createArrayNode();

        List<String> sortedNames = new ArrayList<>(state.definitions().keySet());

        sortedNames.sort(Comparator.naturalOrder());

        for (String name : sortedNames) {

            McpServerDefinition def = state.definitions().get(name);

            boolean isRunning = state.running().getOrDefault(name, false);

            ObjectNode srv = MAPPER.createObjectNode();

            srv.put("name", name);

            srv.put("enabled", def.config().enabled());

            srv.put("running", isRunning);

            srv.put("command", def.config().command());

            ArrayNode args = MAPPER.createArrayNode();

            for (String a : def.config().args()) args.add(a);

            srv.set("args", args);

            servers.add(srv);

        }

        long runningCount = state.running().values().stream().filter(Boolean::booleanValue).count();

        ObjectNode snapshot = MAPPER.createObjectNode();

        snapshot.put("status", state.lifecycleState());

        snapshot.set("servers", servers);

        ObjectNode counts = MAPPER.createObjectNode();

        counts.put("defined", state.definitions().size());

        counts.put("running", (int) runningCount);

        snapshot.set("counts", counts);

        return snapshot;

    }

    private static List<String> defaultRpcMethods() {

        return List.of(

            "initialize", "healthz", "capabilities",

            "tools/list", "tools/call",

            "resources/list", "resources/read",

            "server/list", "server/register", "server/start", "server/stop", "server/unregister",

            "shutdown"

        );

    }

    public static InMemoryMcpClient defaultStdioClient(String serverName) {

        ObjectMapper m = new ObjectMapper();

        String healthUri = "mcp://" + serverName + "/health";

        String capabilitiesUri = "mcp://" + serverName + "/capabilities";

        InMemoryMcpClient client = new InMemoryMcpClient(serverName);

        ObjectNode healthResult = m.createObjectNode();

        healthResult.put("status", "ok");

        healthResult.put("server_name", serverName);

        client.withTool("health", "Health check", healthResult);

        ObjectNode capsResult = m.createObjectNode();

        ArrayNode toolsArr = m.createArrayNode();

        toolsArr.add("health").add("capabilities");

        ArrayNode resourcesArr = m.createArrayNode();

        resourcesArr.add(healthUri).add(capabilitiesUri);

        capsResult.set("tools", toolsArr);

        capsResult.set("resources", resourcesArr);

        client.withTool("capabilities", "Server capabilities", capsResult);

        client.withResource(healthUri, "Health endpoint", healthResult);

        ObjectNode capsResource = m.createObjectNode();

        capsResource.put("server_name", serverName);

        ArrayNode methodsArr = m.createArrayNode();

        for (String method : defaultRpcMethods()) methodsArr.add(method);

        capsResource.set("methods", methodsArr);

        client.withResource(capabilitiesUri, "Capabilities endpoint", capsResource);

        return client;

    }

    public static String parseServerFromUri(String uri) {

        if (uri == null || !uri.startsWith("mcp://")) return null;

        String rest = uri.substring(6);

        int slash = rest.indexOf('/');

        String server = slash >= 0 ? rest.substring(0, slash) : rest;

        return server.isEmpty() ? null : server;

    }

    private <T> T coerceParams(JsonNode params, Class<T> type) {

        JsonNode p = (params != null && !params.isNull()) ? params : MAPPER.createObjectNode();

        return MAPPER.convertValue(p, type);

    }

    // --- Inner types ---

    public record DispatchResult(JsonNode result, boolean shouldExit) { }

    public static class JsonRpcException extends RuntimeException {

        private final JsonRpcError error;

        public JsonRpcException(JsonRpcError error) {
                super(error.message());
                this.error = error;
            }

        public JsonRpcError error() { return error; }

    }

    public static class MethodNotFoundException extends JsonRpcException {

        public MethodNotFoundException(String method) { super(JsonRpcError.methodNotFound(method)); }

    }

    // --- Params records ---

    record ToolsListParams(String server) { }

    record ToolsCallParams(String name, String tool, String server, JsonNode arguments) {

        String effectiveName() { return name != null ? name : tool; }

    }

    record ResourcesListParams(String server) { }

    record ResourcesReadParams(String server, String uri) { }

    record ServerRegisterParams(McpServerConfig server, ToolFilter filter, boolean start) {

        public ServerRegisterParams {

            if (server == null) throw new JsonRpcException(JsonRpcError.invalidParams("server config required"));

        }

    }

    record ServerNameParams(String name) {

        public ServerNameParams {

            if (name == null || name.isBlank()) throw new JsonRpcException(JsonRpcError.invalidParams("name required"));

        }

    }

}
