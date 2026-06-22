package com.jay.protocol.params;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ThreadSetNameParams(@JsonProperty("thread_id") String threadId, String name) { }
