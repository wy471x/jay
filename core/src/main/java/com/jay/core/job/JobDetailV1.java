package com.jay.core.job;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record JobDetailV1(
    @JsonProperty("schema_version") int schemaVersion,
    @JsonProperty("status") String status,
    @JsonProperty("detail") String detail,
    @JsonProperty("retry") JobRetryMetadata retry,
    @JsonProperty("history") List<JobHistoryEntry> history
) {}
