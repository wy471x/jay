package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@JsonInclude(NON_NULL)

public record FleetTaskWorkerProfile(String role, @JsonProperty("tool_profile") String toolProfile,

        @JsonInclude(NON_EMPTY) List<String> tools, @JsonInclude(NON_EMPTY) List<String> capabilities) { }
