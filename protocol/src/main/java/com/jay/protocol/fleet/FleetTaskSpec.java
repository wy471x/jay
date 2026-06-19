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
public record FleetTaskSpec(
        String id, String name, String description,
        String objective, String instructions,
        FleetTaskWorkerProfile worker,
        FleetWorkspaceRequirements workspace,
        @JsonInclude(NON_EMPTY) List<String> inputFiles,
        @JsonInclude(NON_EMPTY) List<String> context,
        FleetTaskBudget budget,
        @JsonInclude(NON_EMPTY) List<String> tags,
        @JsonInclude(NON_EMPTY) List<FleetArtifactKind> expectedArtifacts,
        FleetScorerSpec scorer,
        FleetRetryPolicy retryPolicy,
        FleetAlertPolicy alertPolicy,
        @JsonProperty("timeout_seconds") Long timeoutSeconds,
        Map<String, JsonNode> metadata) {}
