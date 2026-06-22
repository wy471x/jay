package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@JsonInclude(NON_NULL)

public record FleetReceipt(@JsonProperty("run_id") FleetRunId runId, @JsonProperty("task_id") String taskId,

        @JsonProperty("worker_id") String workerId, @JsonProperty("completed_at") String completedAt,

        FleetTaskResult result, @JsonProperty("failure_kind") FleetTaskFailureKind failureKind,

        @JsonInclude(NON_EMPTY) List<FleetArtifactRef> artifacts, FleetScore score) { }
