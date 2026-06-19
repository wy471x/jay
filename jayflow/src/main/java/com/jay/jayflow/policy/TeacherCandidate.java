package com.jay.jayflow.policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

/** Full teacher candidate record. Equivalent to Rust's TeacherCandidate. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TeacherCandidate(
        @JsonProperty("candidate_id") String candidateId,
        TeacherCandidateKind kind,
        TeacherCandidateStatus status,
        @JsonProperty("source_node_id") String sourceNodeId,
        @JsonProperty("source_branch_id") String sourceBranchId,
        String summary,
        @JsonInclude(NON_EMPTY) List<String> evidence,
        @JsonProperty("replay_results") @JsonInclude(NON_EMPTY) List<StudentReplayResult> replayResults) {}
