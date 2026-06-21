package com.jay.state.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Persisted thread goal state.
 */
@Table("thread_goals")
public record GoalEntity(
        @Id String threadId,
        String goalId,
        String objective,
        String status,
        Long tokenBudget,
        long tokensUsed,
        long timeUsedSeconds,
        long continuationCount,
        long createdAt,
        long updatedAt
) {}
