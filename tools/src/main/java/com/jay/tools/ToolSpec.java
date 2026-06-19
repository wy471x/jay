package com.jay.tools;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Specification describing a tool in the registry. Equivalent to Rust's ToolSpec.
 */
@JsonInclude(NON_NULL)
public record ToolSpec(
        String name,
        JsonNode inputSchema,
        JsonNode outputSchema,
        boolean supportsParallelToolCalls,
        Long timeoutMs
) {}
