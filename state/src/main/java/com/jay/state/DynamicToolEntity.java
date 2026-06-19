package com.jay.state;

import org.springframework.data.relational.core.mapping.Table;

/**
 * Per-thread dynamically registered tool. Equivalent to Rust's DynamicToolRecord.
 */
@Table("thread_dynamic_tools")
public record DynamicToolEntity(
        String threadId,
        int position,
        String name,
        String description,
        String inputSchema
) {}
