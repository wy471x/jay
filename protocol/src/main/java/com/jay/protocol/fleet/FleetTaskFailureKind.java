package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FleetTaskFailureKind {

    @JsonProperty("transport") TRANSPORT,

    @JsonProperty("task") TASK,

    @JsonProperty("verifier") VERIFIER

}
