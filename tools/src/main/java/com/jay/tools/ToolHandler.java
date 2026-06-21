package com.jay.tools;

import com.jay.protocol.core.ToolKind;
import com.jay.protocol.core.ToolOutput;

/**
 * Interface for concrete tool handlers. Equivalent to Rust's ToolHandler trait.
 */
public interface ToolHandler {

    ToolKind kind();

    default boolean matchesKind(ToolKind kind) {
        return kind() == kind;
    }

    default boolean isMutating() {
        return false;
    }

    ToolOutput handle(ToolInvocation invocation);
}
