package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FleetAlertEventClass {

    @JsonProperty("stale") STALE,

    @JsonProperty("restart_exhausted") RESTART_EXHAUSTED,

    @JsonProperty("needs_human") NEEDS_HUMAN,

    @JsonProperty("budget_exceeded") BUDGET_EXCEEDED,

    @JsonProperty("verifier_failed") VERIFIER_FAILED,

    @JsonProperty("run_completed") RUN_COMPLETED

}
