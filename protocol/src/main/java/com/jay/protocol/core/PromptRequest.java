package com.jay.protocol.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PromptRequest(@JsonProperty("thread_id") String threadId, String prompt, String model) { }
