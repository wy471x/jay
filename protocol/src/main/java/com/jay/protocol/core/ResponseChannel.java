package com.jay.protocol.core;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ResponseChannel {
    @JsonProperty("text") TEXT,
    @JsonProperty("reasoning") REASONING
}
