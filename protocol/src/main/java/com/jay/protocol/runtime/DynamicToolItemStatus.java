package com.jay.protocol.runtime;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum DynamicToolItemStatus {

    @JsonProperty("in_progress") IN_PROGRESS,

    @JsonProperty("completed") COMPLETED,

    @JsonProperty("failed") FAILED

}

// ---- DynamicToolCallParams / Result / Content ----
