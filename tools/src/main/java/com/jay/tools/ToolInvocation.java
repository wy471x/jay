package com.jay.tools;

import com.jay.protocol.ToolPayload;

/**
 * A validated tool invocation ready to be handled. Equivalent to Rust's ToolInvocation.
 */
public record ToolInvocation(
        String callId,
        String toolName,
        ToolPayload payload,
        ToolCallSource source
) {}
