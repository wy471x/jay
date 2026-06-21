package com.jay.protocol.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record ThreadGoal(
        @JsonProperty("thread_id") String threadId,
        @JsonProperty("goal_id") String goalId,
        String objective, ThreadGoalStatus status,
        @JsonProperty("token_budget") Long tokenBudget,
        @JsonProperty("tokens_used") long tokensUsed,
        @JsonProperty("time_used_seconds") long timeUsedSeconds,
        @JsonProperty("continuation_count") long continuationCount,
        @JsonProperty("created_at") long createdAt,
        @JsonProperty("updated_at") long updatedAt) {}
