package com.jay.state;

import org.springframework.data.relational.core.mapping.Table;

/**
 * Named checkpoint for thread state. Equivalent to Rust's CheckpointRecord.
 */
@Table("checkpoints")
public record CheckpointEntity(
        String threadId,
        String checkpointId,
        String stateJson,
        long createdAt
) {}
