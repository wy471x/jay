package com.jay.state;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Persisted message record with tree-structured branching.
 * Equivalent to Rust's MessageRecord.
 */
@Table("messages")
public record MessageEntity(
        @Id Long id,
        String threadId,
        String role,
        String content,
        String itemJson,
        long createdAt,
        Long parentEntryId
) {}
