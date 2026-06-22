package com.jay.protocol.runtime;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TurnEnvironmentParams(@JsonProperty("environment_id") String environmentId, String cwd) { }
