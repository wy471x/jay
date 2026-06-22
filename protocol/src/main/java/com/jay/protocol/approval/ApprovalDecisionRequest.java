package com.jay.protocol.approval;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record ApprovalDecisionRequest(String decision,
        @JsonProperty(value = "remember", defaultValue = "false") boolean remember) { }
