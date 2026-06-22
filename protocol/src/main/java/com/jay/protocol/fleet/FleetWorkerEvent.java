package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)

public record FleetWorkerEvent(long seq, @JsonProperty("run_id") FleetRunId runId,

        @JsonProperty("worker_id") String workerId, @JsonProperty("task_id") String taskId,

        String timestamp, FleetWorkerEventPayload payload, Map<String, JsonNode> extra) { }
