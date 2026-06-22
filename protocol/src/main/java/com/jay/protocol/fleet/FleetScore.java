package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)

public record FleetScore(double value, Double max, String notes) { }
