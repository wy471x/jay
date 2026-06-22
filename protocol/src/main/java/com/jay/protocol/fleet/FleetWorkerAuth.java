package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonSubTypes;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "method")

@JsonSubTypes({

    @JsonSubTypes.Type(value = FleetWorkerAuth.None.class, name = "none"),

    @JsonSubTypes.Type(value = FleetWorkerAuth.SshKey.class, name = "ssh_key"),

    @JsonSubTypes.Type(value = FleetWorkerAuth.Token.class, name = "token"),

    @JsonSubTypes.Type(value = FleetWorkerAuth.Mtls.class, name = "mtls"),

})

public sealed interface FleetWorkerAuth {

    record None() implements FleetWorkerAuth { }

    record SshKey(String identity, @JsonProperty("known_hosts") String knownHosts,

                  @JsonProperty("host_key_fingerprint") String hostKeyFingerprint,

                  String user) implements FleetWorkerAuth { }

    record Token(@JsonProperty("token_ref") FleetSecretRef tokenRef) implements FleetWorkerAuth { }

    record Mtls(@JsonProperty("cert_path") String certPath, @JsonProperty("key_ref") FleetSecretRef keyRef) implements FleetWorkerAuth { }

}
