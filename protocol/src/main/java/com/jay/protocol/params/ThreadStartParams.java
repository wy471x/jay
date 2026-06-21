package com.jay.protocol.params;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record ThreadStartParams(String model, @JsonProperty("model_provider") String modelProvider,
        String cwd, @JsonProperty(value = "persist_extended_history", defaultValue = "false") boolean persistExtendedHistory) {}
