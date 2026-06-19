package com.jay.jayflow.ir;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum IsolationMode {
    @JsonProperty("shared") SHARED,
    @JsonProperty("worktree") WORKTREE
}
