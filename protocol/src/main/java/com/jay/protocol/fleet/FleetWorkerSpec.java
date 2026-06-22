package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@JsonInclude(NON_NULL)

public record FleetWorkerSpec(String id, String name, FleetHostSpec host,

        @JsonProperty("trust_level") FleetTrustLevel trustLevel,

        Map<String, String> labels, @JsonInclude(NON_EMPTY) List<String> capabilities,

        @JsonProperty("max_concurrent_tasks") Integer maxConcurrentTasks) { }
