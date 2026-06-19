package com.jay.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

public enum ThreadStatus {
    @JsonProperty("running") RUNNING,
    @JsonProperty("idle") IDLE,
    @JsonProperty("completed") COMPLETED,
    @JsonProperty("failed") FAILED,
    @JsonProperty("paused") PAUSED,
    @JsonProperty("archived") ARCHIVED
}
