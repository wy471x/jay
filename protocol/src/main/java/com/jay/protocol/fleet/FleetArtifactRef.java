package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.annotation.JsonProperty;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)

public record FleetArtifactRef(FleetArtifactKind kind, String path, String checksum,

        @JsonProperty("mime_type") String mimeType, @JsonProperty("size_bytes") Long sizeBytes) { }
