package com.jay.protocol.core;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ThreadStatus {
    @JsonProperty("running") RUNNING,
    @JsonProperty("idle") IDLE,
    @JsonProperty("completed") COMPLETED,
    @JsonProperty("failed") FAILED,
    @JsonProperty("paused") PAUSED,
    @JsonProperty("archived") ARCHIVED
}
