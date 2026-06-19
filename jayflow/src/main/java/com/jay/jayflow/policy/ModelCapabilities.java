package com.jay.jayflow.policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Model capability flags. Equivalent to Rust's ModelCapabilities. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record ModelCapabilities(
        @JsonProperty("tool_calls") boolean toolCalls,
        @JsonProperty("json_mode") boolean jsonMode,
        @JsonProperty("prompt_cache") boolean promptCache,
        @JsonProperty("large_context") boolean largeContext,
        @JsonProperty("streaming") boolean streaming) {

    public ModelCapabilities() { this(false, false, false, false, false); }

    public boolean satisfies(ModelCapabilities required) {
        return (!required.toolCalls || toolCalls)
                && (!required.jsonMode || jsonMode)
                && (!required.promptCache || promptCache)
                && (!required.largeContext || largeContext)
                && (!required.streaming || streaming);
    }
}
