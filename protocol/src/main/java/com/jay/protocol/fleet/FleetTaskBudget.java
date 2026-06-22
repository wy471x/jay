package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.annotation.JsonProperty;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)

public record FleetTaskBudget(@JsonProperty("max_tokens") Long maxTokens,

        @JsonProperty("max_tool_calls") Integer maxToolCalls, @JsonProperty("max_seconds") Long maxSeconds) { }

// ---- FleetArtifactKind (serializes as plain string) ----
