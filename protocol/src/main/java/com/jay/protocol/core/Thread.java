package com.jay.protocol.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record Thread(
        String id, String preview, boolean ephemeral,
        @JsonProperty("model_provider") String modelProvider,
        @JsonProperty("created_at") long createdAt,
        @JsonProperty("updated_at") long updatedAt,
        ThreadStatus status, String path, String cwd,
        @JsonProperty("cli_version") String cliVersion,
        SessionSource source, String name) { }
