package com.jay.protocol.params;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ThreadGoalGetParams(@JsonProperty("thread_id") String threadId) {}
