package com.jay.jayflow.policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

/** Full student replay result. Equivalent to Rust's StudentReplayResult. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudentReplayResult(
        @JsonProperty("trace_id") String traceId,
        @JsonProperty("candidate_id") String candidateId,
        StudentReplayMetrics baseline,
        StudentReplayMetrics candidate,
        @JsonInclude(NON_EMPTY) @JsonProperty("required_tests") List<StudentReplayTestResult> requiredTests,
        @JsonInclude(NON_EMPTY) @JsonProperty("policy_violations") List<String> policyViolations,
        boolean stale,
        String notes) {

    public int scoreDelta() { return candidate.score() - baseline.score(); }
    public long costDeltaMicrousd() { return candidate.costMicrousd() - baseline.costMicrousd(); }
}
