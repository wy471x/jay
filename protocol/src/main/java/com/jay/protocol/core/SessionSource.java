package com.jay.protocol.core;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum SessionSource {
    @JsonProperty("interactive") INTERACTIVE,
    @JsonProperty("resume") RESUME,
    @JsonProperty("fork") FORK,
    @JsonProperty("api") API,
    @JsonProperty("unknown") UNKNOWN
}
