package com.jay.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

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

// ---- Thread params ----
