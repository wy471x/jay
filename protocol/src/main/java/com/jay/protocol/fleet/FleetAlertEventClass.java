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

public enum FleetAlertEventClass {
    @JsonProperty("stale") STALE,
    @JsonProperty("restart_exhausted") RESTART_EXHAUSTED,
    @JsonProperty("needs_human") NEEDS_HUMAN,
    @JsonProperty("budget_exceeded") BUDGET_EXCEEDED,
    @JsonProperty("verifier_failed") VERIFIER_FAILED,
    @JsonProperty("run_completed") RUN_COMPLETED
}
