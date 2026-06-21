package com.jay.core.job;

public enum JobStatus {
    QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED;

    public String toStoreValue() { return name().toLowerCase(); }

    public static JobStatus fromStoreValue(String s) {
        if (s == null) return null;
        return valueOf(s.toUpperCase());
    }
}
