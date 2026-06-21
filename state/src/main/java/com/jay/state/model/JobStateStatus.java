package com.jay.state.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum JobStateStatus {
    QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED, STALE;

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }

    public static JobStateStatus from(String s) {
        if (s == null) return null;
        return valueOf(s.toUpperCase());
    }
}
