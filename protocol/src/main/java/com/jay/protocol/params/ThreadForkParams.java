package com.jay.protocol.params;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record ThreadForkParams(@JsonProperty("thread_id") String threadId, String path,
        String model, @JsonProperty("model_provider") String modelProvider, String cwd,
        @JsonProperty("approval_policy") String approvalPolicy, String sandbox, JsonNode config,
        @JsonProperty("base_instructions") String baseInstructions,
        @JsonProperty("developer_instructions") String developerInstructions,
        @JsonProperty(value = "persist_extended_history", defaultValue = "false") boolean persistExtendedHistory) {}
