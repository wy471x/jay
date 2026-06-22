package com.jay.state.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Persisted message record with tree-structured branching.
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
) { }
