package com.jay.state.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Persisted background job state.
 */
@Table("jobs")
public record JobEntity(
        @Id String id,
        String name,
        String status,
        Integer progress,
        String detail,
        long createdAt,
        long updatedAt
) { }
