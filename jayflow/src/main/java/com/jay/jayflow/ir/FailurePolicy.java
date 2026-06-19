package com.jay.jayflow.ir;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FailurePolicy {
    @JsonProperty("skip_continue") SKIP_CONTINUE,
    @JsonProperty("abort") ABORT
}
