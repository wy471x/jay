package com.jay.protocol.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "status")
@JsonSubTypes({
    @JsonSubTypes.Type(value = McpStartupStatus.Starting.class, name = "starting"),
    @JsonSubTypes.Type(value = McpStartupStatus.Ready.class, name = "ready"),
    @JsonSubTypes.Type(value = McpStartupStatus.Failed.class, name = "failed"),
    @JsonSubTypes.Type(value = McpStartupStatus.Cancelled.class, name = "cancelled"),
})
public sealed interface McpStartupStatus {
    record Starting() implements McpStartupStatus {}
    record Ready() implements McpStartupStatus {}
    record Failed(String error) implements McpStartupStatus {}
    record Cancelled() implements McpStartupStatus {}
}
