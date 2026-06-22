package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FleetRunStatus {

    @JsonProperty("pending") PENDING,

    @JsonProperty("queued") QUEUED,

    @JsonProperty("running") RUNNING,

    @JsonProperty("paused") PAUSED,

    @JsonProperty("completed") COMPLETED,

    @JsonProperty("failed") FAILED,

    @JsonProperty("cancelled") CANCELLED

}

// ---- FleetTaskSpec ----
