package com.jay.jayflow.ir;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import com.jay.jayflow.validation.WorkflowPlan;

/**
 * User-facing workflow configuration. Equivalent to Rust's WorkflowConfig + Phase + Task.
 */
@JsonInclude(NON_NULL)
public record WorkflowConfig(
        String goal,
        @JsonProperty("max_concurrent") int maxConcurrent,
        String description,
        List<Phase> phases) {

    public WorkflowConfig {
        if (maxConcurrent == 0) maxConcurrent = 4;
        if (phases == null) phases = List.of();
    }

    public WorkflowPlan compile() { return WorkflowPlan.fromConfig(this); }

    @JsonInclude(NON_NULL)
    public record BudgetSpec(@JsonProperty("max_steps") Integer maxSteps,
                              @JsonProperty("timeout_secs") Long timeoutSecs,
                              @JsonProperty("max_parallel") Integer maxParallel) {}

    @JsonInclude(NON_NULL)
    public record PermissionSpec(@JsonProperty("allow_write") boolean allowWrite,
                                  @JsonProperty("allow_network") boolean allowNetwork,
                                  @JsonProperty("allowed_tools") List<String> allowedTools,
                                  @JsonProperty("file_scope") List<String> fileScope) {}

    @JsonInclude(NON_NULL)
    public record ModelPolicy(String provider, String model,
                               @JsonProperty("fallback_models") List<String> fallbackModels) {}

    @JsonInclude(NON_NULL)
    public record PromotionPolicy(PromotionStrategy strategy,
                                   @JsonProperty("require_teacher_review") boolean requireTeacherReview,
                                   @JsonProperty("min_successful_branches") Integer minSuccessfulBranches,
                                   @JsonProperty("promotion_gate") PromotionGateSpec promotionGate) {}

    @JsonInclude(NON_NULL)
    public record PromotionGateSpec(@JsonProperty("min_score_delta") int minScoreDelta,
                                     @JsonProperty("max_cost_delta_microusd") Long maxCostDeltaMicrousd,
                                     @JsonProperty("require_all_tests_pass") boolean requireAllTestsPass,
                                     @JsonProperty("reject_policy_violations") boolean rejectPolicyViolations,
                                     @JsonProperty("reject_stale_replay") boolean rejectStaleReplay) {
        public PromotionGateSpec() { this(1, null, true, true, true); }
    }

    @JsonInclude(NON_NULL)
    public record Phase(String name, String description, @JsonProperty("depends_on") List<String> dependsOn,
                         boolean parallel, @JsonProperty("on_failure") FailurePolicy onFailure,
                         List<Task> tasks) {
        public Phase(String name, boolean parallel, FailurePolicy onFailure,
                      List<String> dependsOn, List<Task> tasks) {
            this(name, null, dependsOn != null ? dependsOn : List.of(), parallel,
                    onFailure != null ? onFailure : FailurePolicy.SKIP_CONTINUE,
                    tasks != null ? tasks : List.of());
        }
    }

    @JsonInclude(NON_NULL)
    public record Task(String id, String prompt, AgentType agentType, TaskMode mode,
                        IsolationMode isolation, @JsonProperty("file_scope") List<String> fileScope,
                        @JsonProperty("depends_on_results") List<String> dependsOnResults,
                        @JsonProperty("max_steps") Integer maxSteps,
                        @JsonProperty("timeout_secs") Long timeoutSecs) {
        public Task {
            if (agentType == null) agentType = AgentType.GENERAL;
            if (mode == null) mode = TaskMode.READ_ONLY;
            if (isolation == null) isolation = IsolationMode.SHARED;
        }
        // Legacy convenience constructor
        public Task(String id, String prompt) { this(id, prompt, AgentType.GENERAL, TaskMode.READ_ONLY,
                IsolationMode.SHARED, List.of(), List.of(), null, null); }
    }
}
