package com.jay.protocol.runtime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

public record RuntimeCapabilities(
        boolean threads, boolean turns, @JsonProperty("turn_steer") boolean turnSteer,
        @JsonProperty("turn_interrupt") boolean turnInterrupt, @JsonProperty("event_replay") boolean eventReplay,
        @JsonProperty("external_tools") boolean externalTools, boolean environments,
        @JsonProperty("worker_runtime") boolean workerRuntime) {}
