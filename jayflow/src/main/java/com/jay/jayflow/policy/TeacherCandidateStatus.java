package com.jay.jayflow.policy;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Teacher candidate lifecycle status. Equivalent to Rust's TeacherCandidateStatus. */
public enum TeacherCandidateStatus {
    @JsonProperty("proposed") PROPOSED,
    @JsonProperty("accepted") ACCEPTED,
    @JsonProperty("rejected") REJECTED,
    @JsonProperty("promoted") PROMOTED
}
