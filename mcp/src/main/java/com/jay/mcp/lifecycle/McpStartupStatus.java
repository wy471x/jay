package com.jay.mcp.lifecycle;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = McpStartupStatus.Starting.class, name = "starting"),
    @JsonSubTypes.Type(value = McpStartupStatus.Ready.class, name = "ready"),
    @JsonSubTypes.Type(value = McpStartupStatus.Failed.class, name = "failed"),
    @JsonSubTypes.Type(value = McpStartupStatus.Cancelled.class, name = "cancelled")
})
public sealed interface McpStartupStatus {
    record Starting() implements McpStartupStatus {}
    record Ready() implements McpStartupStatus {}
    record Failed(String error) implements McpStartupStatus {}
    record Cancelled() implements McpStartupStatus {}
}
