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

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "state")
@JsonSubTypes({
    @JsonSubTypes.Type(value = FleetWorkerEventPayload.Queued.class, name = "queued"),
    @JsonSubTypes.Type(value = FleetWorkerEventPayload.Leased.class, name = "leased"),
    @JsonSubTypes.Type(value = FleetWorkerEventPayload.Starting.class, name = "starting"),
    @JsonSubTypes.Type(value = FleetWorkerEventPayload.Running.class, name = "running"),
    @JsonSubTypes.Type(value = FleetWorkerEventPayload.ModelWait.class, name = "model_wait"),
    @JsonSubTypes.Type(value = FleetWorkerEventPayload.RunningTool.class, name = "running_tool"),
    @JsonSubTypes.Type(value = FleetWorkerEventPayload.Heartbeat.class, name = "heartbeat"),
    @JsonSubTypes.Type(value = FleetWorkerEventPayload.Artifact.class, name = "artifact"),
    @JsonSubTypes.Type(value = FleetWorkerEventPayload.Completed.class, name = "completed"),
    @JsonSubTypes.Type(value = FleetWorkerEventPayload.Failed.class, name = "failed"),
    @JsonSubTypes.Type(value = FleetWorkerEventPayload.Cancelled.class, name = "cancelled"),
    @JsonSubTypes.Type(value = FleetWorkerEventPayload.Interrupted.class, name = "interrupted"),
    @JsonSubTypes.Type(value = FleetWorkerEventPayload.Stale.class, name = "stale"),
    @JsonSubTypes.Type(value = FleetWorkerEventPayload.Restarted.class, name = "restarted"),
    @JsonSubTypes.Type(value = FleetWorkerEventPayload.Escalated.class, name = "escalated"),
})
public sealed interface FleetWorkerEventPayload {
    record Queued() implements FleetWorkerEventPayload {}
    record Leased(@JsonProperty("lease_expires_at") String leaseExpiresAt) implements FleetWorkerEventPayload {}
    record Starting() implements FleetWorkerEventPayload {}
    record Running() implements FleetWorkerEventPayload {}
    record ModelWait(String model) implements FleetWorkerEventPayload {}
    record RunningTool(String tool, @JsonProperty("call_id") String callId) implements FleetWorkerEventPayload {}
    record Heartbeat(@JsonProperty("cpu_percent") Float cpuPercent, @JsonProperty("memory_mb") Long memoryMb) implements FleetWorkerEventPayload {}
    record Artifact(FleetArtifactRef artifact) implements FleetWorkerEventPayload {}
    record Completed(@JsonProperty("exit_code") Integer exitCode, String summary) implements FleetWorkerEventPayload {}
    record Failed(String reason, boolean recoverable) implements FleetWorkerEventPayload {}
    record Cancelled(@JsonProperty("cancelled_by") String cancelledBy) implements FleetWorkerEventPayload {}
    record Interrupted(String signal) implements FleetWorkerEventPayload {}
    record Stale(@JsonProperty("last_heartbeat_at") String lastHeartbeatAt) implements FleetWorkerEventPayload {}
    record Restarted(@JsonProperty("restart_count") int restartCount) implements FleetWorkerEventPayload {}
    record Escalated(String channel, @JsonProperty("alert_id") String alertId) implements FleetWorkerEventPayload {}
}

// ---- FleetRetryPolicy ----
