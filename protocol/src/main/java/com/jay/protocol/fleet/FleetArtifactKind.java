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
