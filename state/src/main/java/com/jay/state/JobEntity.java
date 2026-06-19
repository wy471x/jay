package com.jay.state;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Persisted background job state. Equivalent to Rust's JobStateRecord.
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
) {}
