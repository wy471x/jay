package com.jay.state.model;

import org.springframework.data.relational.core.mapping.Table;

/**
 * Per-thread dynamically registered tool.
 */
@Table("thread_dynamic_tools")
public record DynamicToolEntity(
        String threadId,
        int position,
        String name,
        String description,
        String inputSchema
) {}
