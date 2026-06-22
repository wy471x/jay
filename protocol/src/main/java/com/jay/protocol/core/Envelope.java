package com.jay.protocol.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record Envelope<T>(String requestId, String threadId, T body) { }
