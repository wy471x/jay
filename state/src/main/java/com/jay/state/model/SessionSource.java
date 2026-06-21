package com.jay.state.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SessionSource {
    INTERACTIVE, RESUME, FORK, API, UNKNOWN;

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }

    public static SessionSource from(String s) {
        if (s == null) return null;
        return valueOf(s.toUpperCase());
    }
}
