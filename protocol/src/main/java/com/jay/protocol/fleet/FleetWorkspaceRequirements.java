package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@JsonInclude(NON_NULL)

public record FleetWorkspaceRequirements(String root, @JsonInclude(NON_EMPTY) List<String> requiredFiles,

        @JsonInclude(NON_EMPTY) List<String> writablePaths, FleetEnvironmentRequirements environment) { }
