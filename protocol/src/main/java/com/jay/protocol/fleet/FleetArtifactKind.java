package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.annotation.JsonValue;

public record FleetArtifactKind(String value) {

    public static final FleetArtifactKind LOG = new FleetArtifactKind("log");

    public static final FleetArtifactKind PATCH = new FleetArtifactKind("patch");

    public static final FleetArtifactKind TEST_RESULT = new FleetArtifactKind("test_result");

    public static final FleetArtifactKind REPORT = new FleetArtifactKind("report");

    public static final FleetArtifactKind CHECKPOINT = new FleetArtifactKind("checkpoint");

    public static final FleetArtifactKind RECEIPT = new FleetArtifactKind("receipt");

    @JsonCreator public static FleetArtifactKind of(String value) { return new FleetArtifactKind(value); }

    @JsonValue public String value() { return value; }

}
