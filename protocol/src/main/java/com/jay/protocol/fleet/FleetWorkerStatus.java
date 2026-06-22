package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FleetWorkerStatus {

    @JsonProperty("unknown") UNKNOWN,

    @JsonProperty("online") ONLINE,

    @JsonProperty("busy") BUSY,

    @JsonProperty("offline") OFFLINE,

    @JsonProperty("unhealthy") UNHEALTHY,

    @JsonProperty("draining") DRAINING,

    @JsonProperty("retired") RETIRED

}
