package com.jay.protocol.runtime;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RuntimeCapabilities(

        boolean threads, boolean turns, @JsonProperty("turn_steer") boolean turnSteer,

        @JsonProperty("turn_interrupt") boolean turnInterrupt, @JsonProperty("event_replay") boolean eventReplay,

        @JsonProperty("external_tools") boolean externalTools, boolean environments,

        @JsonProperty("worker_runtime") boolean workerRuntime) { }
