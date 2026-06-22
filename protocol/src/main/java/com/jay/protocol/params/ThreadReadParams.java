package com.jay.protocol.params;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ThreadReadParams(@JsonProperty("thread_id") String threadId) { }
