package com.jay.state.model;

import org.springframework.data.relational.core.mapping.Table;

/**
 * Named checkpoint for thread state.
 */
@Table("checkpoints")
public record CheckpointEntity(
        String threadId,
        String checkpointId,
        String stateJson,
        long createdAt
) { }
