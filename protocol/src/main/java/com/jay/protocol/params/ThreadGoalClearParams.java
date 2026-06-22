package com.jay.protocol.params;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ThreadGoalClearParams(@JsonProperty("thread_id") String threadId) { }
