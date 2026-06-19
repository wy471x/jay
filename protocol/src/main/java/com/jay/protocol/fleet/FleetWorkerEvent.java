package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@JsonInclude(NON_NULL)
public record FleetWorkerEvent(long seq, @JsonProperty("run_id") FleetRunId runId,
        @JsonProperty("worker_id") String workerId, @JsonProperty("task_id") String taskId,
        String timestamp, FleetWorkerEventPayload payload, Map<String, JsonNode> extra) {}
