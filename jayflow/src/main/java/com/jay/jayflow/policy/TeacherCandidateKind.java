package com.jay.jayflow.policy;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Kind of teacher candidate. Equivalent to Rust's TeacherCandidateKind. */
public enum TeacherCandidateKind {
    @JsonProperty("note") NOTE,
    @JsonProperty("workflow_recipe") WORKFLOW_RECIPE,
    @JsonProperty("skill_patch") SKILL_PATCH,
    @JsonProperty("regression_test") REGRESSION_TEST,
    @JsonProperty("cache_policy_patch") CACHE_POLICY_PATCH,
    @JsonProperty("branch_heuristic") BRANCH_HEURISTIC,
    @JsonProperty("starlark_authoring_prompt_patch") STARLARK_AUTHORING_PROMPT_PATCH
}
