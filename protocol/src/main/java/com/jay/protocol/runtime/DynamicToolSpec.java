package com.jay.protocol.runtime;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.JsonNode;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)

public record DynamicToolSpec(String namespace, String name, String description,

        @JsonProperty("input_schema") JsonNode inputSchema,

        @JsonProperty("defer_loading") boolean deferLoading) { }
