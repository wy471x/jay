package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.annotation.JsonSubTypes;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")

@JsonSubTypes({

    @JsonSubTypes.Type(value = FleetScorerSpec.ExitCode.class, name = "exit_code"),

    @JsonSubTypes.Type(value = FleetScorerSpec.FileExists.class, name = "file_exists"),

    @JsonSubTypes.Type(value = FleetScorerSpec.RegexMatch.class, name = "regex_match"),

    @JsonSubTypes.Type(value = FleetScorerSpec.JsonPath.class, name = "json_path"),

    @JsonSubTypes.Type(value = FleetScorerSpec.Command.class, name = "command"),

    @JsonSubTypes.Type(value = FleetScorerSpec.CodeWhaleVerifier.class, name = "codewhale_verifier_prompt"),

    @JsonSubTypes.Type(value = FleetScorerSpec.Manual.class, name = "manual"),

})

public sealed interface FleetScorerSpec {

    record ExitCode() implements FleetScorerSpec { }

    record FileExists(String path) implements FleetScorerSpec { }

    record RegexMatch(String path, String pattern) implements FleetScorerSpec { }

    record JsonPath(String path, String expression) implements FleetScorerSpec { }

    record Command(String command, @JsonInclude(NON_EMPTY) List<String> args) implements FleetScorerSpec { }

    record CodeWhaleVerifier(String prompt) implements FleetScorerSpec { }

    record Manual() implements FleetScorerSpec { }

}

// ---- FleetWorkerSpec / FleetHostSpec ----
