package com.jay.protocol.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ToolPayload.Function.class, name = "function"),
    @JsonSubTypes.Type(value = ToolPayload.Custom.class, name = "custom"),
    @JsonSubTypes.Type(value = ToolPayload.LocalShell.class, name = "local_shell"),
    @JsonSubTypes.Type(value = ToolPayload.Mcp.class, name = "mcp"),
})
public sealed interface ToolPayload {
    record Function(String arguments) implements ToolPayload {}
    record Custom(String input) implements ToolPayload {}
    record LocalShell(LocalShellParams params) implements ToolPayload {}
    record Mcp(String server, String tool, @JsonProperty("raw_arguments") JsonNode rawArguments,
               @JsonProperty("raw_tool_call_id") String rawToolCallId) implements ToolPayload {}
}
