package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FleetRetryPolicy(
        @JsonProperty(value = "max_attempts", defaultValue = "3") int maxAttempts,
        @JsonProperty(value = "initial_backoff_seconds", defaultValue = "5") long initialBackoffSeconds,
        @JsonProperty(value = "max_backoff_seconds", defaultValue = "300") long maxBackoffSeconds,
        @JsonProperty(value = "backoff_multiplier", defaultValue = "2") int backoffMultiplier) {
    public FleetRetryPolicy() { this(3, 5, 300, 2); }
}
