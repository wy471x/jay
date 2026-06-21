package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

public enum FleetTrustLevel {
    @JsonProperty("sandbox") SANDBOX(0),
    @JsonProperty("local") LOCAL(1),
    @JsonProperty("remote_verified") REMOTE_VERIFIED(2),
    @JsonProperty("operator") OPERATOR(3);

    private final int level;
    FleetTrustLevel(int level) { this.level = level; }

    public boolean mayAccessSecrets() { return level >= REMOTE_VERIFIED.level; }
    public boolean mayWriteWorkspace() { return level >= LOCAL.level; }
    public boolean mayAccessNetwork() { return level >= LOCAL.level; }
}

// ---- FleetSecurityPolicy ----
