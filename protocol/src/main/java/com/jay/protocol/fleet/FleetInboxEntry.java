package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.annotation.JsonProperty;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)

public record FleetInboxEntry(@JsonProperty("run_id") FleetRunId runId, @JsonProperty("task_id") String taskId,

        int priority, @JsonProperty("enqueued_at") String enqueuedAt,

        @JsonProperty("lease_deadline") String leaseDeadline, int attempts) { }

// ---- FleetWorkerEvent ----
