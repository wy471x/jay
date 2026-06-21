package com.jay.state.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ThreadGoalStatus {
    ACTIVE, PAUSED, BLOCKED, USAGE_LIMITED, BUDGET_LIMITED, COMPLETE;

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }

    public static ThreadGoalStatus from(String s) {
        if (s == null) return null;
        return valueOf(s.toUpperCase());
    }
}
