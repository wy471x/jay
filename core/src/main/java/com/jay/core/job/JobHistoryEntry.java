package com.jay.core.job;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JobHistoryEntry(
    @JsonProperty("at") long at,
    @JsonProperty("phase") String phase,
    @JsonProperty("status") JobStatus status,
    @JsonProperty("progress") Integer progress,
    @JsonProperty("detail") String detail,
    @JsonProperty("retry") JobRetryMetadata retry
) { }
