package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonSubTypes;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")

@JsonSubTypes({

    @JsonSubTypes.Type(value = FleetHostSpec.Local.class, name = "local"),

    @JsonSubTypes.Type(value = FleetHostSpec.Ssh.class, name = "ssh"),

    @JsonSubTypes.Type(value = FleetHostSpec.Docker.class, name = "docker"),

    @JsonSubTypes.Type(value = FleetHostSpec.Docker.class, name = "container"),

})

public sealed interface FleetHostSpec {

    record Local() implements FleetHostSpec { }

    @JsonInclude(NON_NULL)

    record Ssh(String host, Integer port, String user, String identity,

               @JsonProperty("known_hosts") String knownHosts,

               @JsonProperty("host_key_fingerprint") String hostKeyFingerprint,

               @JsonProperty("working_directory") String workingDirectory,

               @JsonInclude(NON_EMPTY) @JsonProperty("env_allowlist") List<String> envAllowlist,

               @JsonProperty("codewhale_binary") String binary) implements FleetHostSpec { }

    record Docker(String image, @JsonInclude(NON_EMPTY) List<String> args) implements FleetHostSpec { }

}

// ---- FleetTrustLevel ----
