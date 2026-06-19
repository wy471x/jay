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

@JsonInclude(NON_NULL)
public record FleetSecurityPolicy(
        @JsonProperty("default_trust_level") FleetTrustLevel defaultTrustLevel,
        @JsonInclude(NON_EMPTY) @JsonProperty("allowed_secrets") List<FleetSecretRef> allowedSecrets,
        @JsonInclude(NON_EMPTY) @JsonProperty("capability_grants") List<FleetCapabilityGrant> capabilityGrants,
        @JsonProperty("max_trust_level") FleetTrustLevel maxTrustLevel,
        @JsonProperty("require_identity_verification") boolean requireIdentityVerification,
        @JsonProperty("allow_parallel_reads") boolean allowParallelReads) {}
