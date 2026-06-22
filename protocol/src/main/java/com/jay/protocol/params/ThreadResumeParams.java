package com.jay.protocol.params;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record ThreadResumeParams(@JsonProperty("thread_id") String threadId, List<JsonNode> history,
        String path, String model, @JsonProperty("model_provider") String modelProvider,
        String cwd, @JsonProperty("approval_policy") String approvalPolicy,
        String sandbox, JsonNode config, @JsonProperty("base_instructions") String baseInstructions,
        @JsonProperty("developer_instructions") String developerInstructions,
        String personality, @JsonProperty(value = "persist_extended_history", defaultValue = "false") boolean persistExtendedHistory) { }
