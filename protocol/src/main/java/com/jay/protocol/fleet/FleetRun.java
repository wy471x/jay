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
public record FleetRun(
        FleetRunId id, String name, FleetRunStatus status,
        @JsonInclude(NON_EMPTY) List<FleetTaskSpec> taskSpecs,
        @JsonInclude(NON_EMPTY) List<FleetWorkerSpec> workerSpecs,
        Map<String, String> labels,
        FleetSecurityPolicy securityPolicy,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt,
        @JsonProperty("completed_at") String completedAt) {}
