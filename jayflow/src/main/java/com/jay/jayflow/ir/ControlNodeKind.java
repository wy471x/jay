package com.jay.jayflow.ir;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ControlNodeKind {
    @JsonProperty("branch_set") BRANCH_SET,
    @JsonProperty("sequence") SEQUENCE,
    @JsonProperty("reduce") REDUCE,
    @JsonProperty("teacher_review") TEACHER_REVIEW,
    @JsonProperty("loop_until") LOOP_UNTIL,
    @JsonProperty("cond") COND,
    @JsonProperty("expand") EXPAND
}

/** Teacher candidate source kind. */
