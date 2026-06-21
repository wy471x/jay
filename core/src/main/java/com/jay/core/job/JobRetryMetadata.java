package com.jay.core.job;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JobRetryMetadata {
    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    public static final long DEFAULT_BACKOFF_BASE_MS = 500;

    @JsonProperty("attempt") int attempt;
    @JsonProperty("max_attempts") int maxAttempts = DEFAULT_MAX_ATTEMPTS;
    @JsonProperty("backoff_base_ms") long backoffBaseMs = DEFAULT_BACKOFF_BASE_MS;
    @JsonProperty("next_backoff_ms") long nextBackoffMs;
    @JsonProperty("next_retry_at") Long nextRetryAt;

    public JobRetryMetadata() {}

    public int attempt() { return attempt; }
    public int maxAttempts() { return maxAttempts; }
    public long backoffBaseMs() { return backoffBaseMs; }
    public long nextBackoffMs() { return nextBackoffMs; }
    public Long nextRetryAt() { return nextRetryAt; }

    public long deterministicBackoffMs() {
        if (attempt <= 1) return backoffBaseMs;
        int exponent = Math.min(attempt - 1, 20);
        return backoffBaseMs * (1L << exponent);
    }

    JobRetryMetadata withAttempt(int a) { attempt = a; return this; }
    JobRetryMetadata withNextBackoffMs(long ms) { nextBackoffMs = ms; return this; }
    JobRetryMetadata withNextRetryAt(Long at) { nextRetryAt = at; return this; }
}
