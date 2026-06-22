package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FleetTaskResult {

    @JsonProperty("pass") PASS,

    @JsonProperty("partial") PARTIAL,

    @JsonProperty("fail") FAIL,

    @JsonProperty("skip") SKIP,

    @JsonProperty("timeout") TIMEOUT

}
