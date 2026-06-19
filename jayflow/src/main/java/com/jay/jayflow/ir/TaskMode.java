package com.jay.jayflow.ir;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum TaskMode {
    @JsonProperty("read_only") READ_ONLY,
    @JsonProperty("read_write") READ_WRITE
}
