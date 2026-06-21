package com.jay.state.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ThreadStatus {
    RUNNING, IDLE, COMPLETED, FAILED, PAUSED, ARCHIVED;

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }

    public static ThreadStatus from(String s) {
        if (s == null) return null;
        return valueOf(s.toUpperCase());
    }
}
