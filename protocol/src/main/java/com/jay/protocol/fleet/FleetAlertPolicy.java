package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@JsonInclude(NON_NULL)

public record FleetAlertPolicy(@JsonInclude(NON_EMPTY) @JsonProperty("events") List<FleetAlertEventClass> events,

        @JsonInclude(NON_EMPTY) List<FleetAlertChannel> channels,

        @JsonProperty("after_attempts") Integer afterAttempts,

        @JsonProperty("after_minutes_stale") Long afterMinutesStale) { }
