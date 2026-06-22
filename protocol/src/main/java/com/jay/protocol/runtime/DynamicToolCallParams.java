package com.jay.protocol.runtime;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.JsonNode;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)

public record DynamicToolCallParams(

        @JsonProperty("thread_id") String threadId,

        @JsonProperty("turn_id") String turnId,

        @JsonProperty("call_id") String callId,

        String namespace, String tool, JsonNode arguments) { }
