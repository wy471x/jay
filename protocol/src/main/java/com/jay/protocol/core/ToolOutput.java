package com.jay.protocol.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ToolOutput.Function.class, name = "function"),
    @JsonSubTypes.Type(value = ToolOutput.Mcp.class, name = "mcp"),
})
public sealed interface ToolOutput {
    @JsonInclude(NON_NULL)
    record Function(JsonNode body, boolean success) implements ToolOutput { }

    record Mcp(JsonNode result) implements ToolOutput { }
}
