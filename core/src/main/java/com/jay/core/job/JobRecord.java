package com.jay.core.job;

import java.util.ArrayList;

import java.util.List;

public record JobRecord(

    String id, String name, JobStatus status, Integer progress, String detail,

    JobRetryMetadata retry, List<JobHistoryEntry> history,

    long createdAt, long updatedAt

) {

    public JobRecord {

        if (progress == null) progress = 0;

        if (retry == null) retry = new JobRetryMetadata();

        if (history == null) history = new ArrayList<>();

    }

    public JobRecord withStatus(JobStatus s) { return new JobRecord(id, name, s, progress, detail, retry, history, createdAt, updatedAt); }

    public JobRecord withUpdatedAt(long at) { return new JobRecord(id, name, status, progress, detail, retry, history, createdAt, at); }

    public JobRecord withRetry(JobRetryMetadata r) { return new JobRecord(id, name, status, progress, detail, r, history, createdAt, updatedAt); }

    public JobRecord withHistory(List<JobHistoryEntry> h) { return new JobRecord(id, name, status, progress, detail, retry, h, createdAt, updatedAt); }

    public JobRecord withProgress(int p) { return new JobRecord(id, name, status, p, detail, retry, history, createdAt, updatedAt); }

    public JobRecord withDetail(String d) { return new JobRecord(id, name, status, progress, d, retry, history, createdAt, updatedAt); }

}
