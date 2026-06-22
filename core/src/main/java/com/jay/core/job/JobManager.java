package com.jay.core.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jay.state.model.JobEntity;
import com.jay.state.store.StateStore;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages background jobs with exponential backoff retry and versioned JSON persistence.
 * Equivalent to Rust's JobManager.
 */
public class JobManager {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_HISTORY = 64;
    static final int JOB_DETAIL_SCHEMA_VERSION = 1;

    private final ConcurrentHashMap<String, JobRecord> jobs = new ConcurrentHashMap<>();
    private final StateStore store;

    public JobManager(StateStore store) {
        this.store = store;
    }

    // ── Create ──────────────────────────────────────────────────

    public JobRecord enqueue(String name) {
        long now = nowTs();
        String id = "job-" + UUID.randomUUID();
        var job = recordWithHistory(new JobRecord(id, name, JobStatus.QUEUED, 0, null,
            new JobRetryMetadata(), List.of(), now, now), "created");
        jobs.put(id, job);
        persistJob(job);
        return job;
    }

    // ── State transitions ──────────────────────────────────────

    public void setRunning(String id) {
        updateJob(id, job -> {
            var retry = job.retry();
            retry.nextBackoffMs = 0;
            retry.nextRetryAt = null;
            return recordWithHistory(
                job.withStatus(JobStatus.RUNNING).withRetry(retry).withUpdatedAt(nowTs()),
                "running");
        });
    }

    public void updateProgress(String id, int progress, String detail) {
        updateJob(id, job -> recordWithHistory(
            job.withProgress(Math.min(progress, 100))
               .withDetail(detail)
               .withUpdatedAt(nowTs()),
            "progress_updated"));
    }

    public void complete(String id) {
        updateJob(id, job -> {
            var retry = job.retry();
            retry.nextBackoffMs = 0;
            retry.nextRetryAt = null;
            return recordWithHistory(
                job.withStatus(JobStatus.COMPLETED).withProgress(100).withRetry(retry).withUpdatedAt(nowTs()),
                "completed");
        });
    }

    public void fail(String id, String detail) {
        updateJob(id, job -> {
            var retry = job.retry();
            long now = nowTs();
            if (retry.attempt() < retry.maxAttempts()) {
                retry.withAttempt(retry.attempt() + 1);
                long backoff = retry.deterministicBackoffMs();
                if (backoff <= 0) backoff = retry.backoffBaseMs();
                retry.withNextBackoffMs(backoff);
                retry.withNextRetryAt(now + backoff);
            }
            return recordWithHistory(
                job.withStatus(JobStatus.FAILED).withDetail(detail).withRetry(retry).withUpdatedAt(now),
                "failed");
        });
    }

    public void pause(String id) {
        updateJob(id, job -> recordWithHistory(
            job.withStatus(JobStatus.PAUSED).withUpdatedAt(nowTs()), "paused"));
    }

    public void resume(String id, String detail) {
        updateJob(id, job -> {
            var updated = job.withStatus(JobStatus.RUNNING).withUpdatedAt(nowTs());
            if (detail != null) updated = updated.withDetail(detail);
            return recordWithHistory(updated, "resumed");
        });
    }

    public void cancel(String id) {
        updateJob(id, job -> recordWithHistory(
            job.withStatus(JobStatus.CANCELLED).withUpdatedAt(nowTs()), "cancelled"));
    }

    // ── Resume pending ─────────────────────────────────────────

    /** Reload all unfinished jobs from the store on startup. */
    public List<JobRecord> resumePending() {
        List<JobRecord> resumed = new ArrayList<>();
        for (var stored : store.listRecentJobs(Integer.MAX_VALUE)) {
            var job = loadFromStore(stored);
            if (job == null) continue;
            if (job.status() == JobStatus.QUEUED || job.status() == JobStatus.RUNNING) {
                job = job.withStatus(JobStatus.QUEUED);
            }
            jobs.put(job.id(), job);
            resumed.add(job);
        }
        return resumed;
    }

    // ── Access ─────────────────────────────────────────────────

    public Optional<JobRecord> get(String id) {
        return Optional.ofNullable(jobs.get(id));
    }

    public List<JobRecord> listAll() {
        return List.copyOf(jobs.values());
    }

    // ── Internal ───────────────────────────────────────────────

    @FunctionalInterface
    private interface JobUpdater { JobRecord update(JobRecord job); }

    private void updateJob(String id, JobUpdater updater) {
        var job = jobs.get(id);
        if (job == null) return;
        var updated = updater.update(job);
        jobs.put(id, updated);
        persistJob(updated);
    }

    private JobRecord recordWithHistory(JobRecord job, String phase) {
        List<JobHistoryEntry> h = new ArrayList<>(job.history());
        h.add(new JobHistoryEntry(job.updatedAt(), phase, job.status(),
            job.progress(), job.detail(), job.retry()));
        if (h.size() > MAX_HISTORY) h = new ArrayList<>(h.subList(h.size() - MAX_HISTORY, h.size()));
        return job.withHistory(h);
    }

    private void persistJob(JobRecord job) {
        try {
            String detailJson = MAPPER.writeValueAsString(new JobDetailV1(
                JOB_DETAIL_SCHEMA_VERSION, job.status().toStoreValue(),
                job.detail(), job.retry(), job.history()));
            store.upsertJob(new JobEntity(job.id(), job.name(),
                job.status().toStoreValue(), job.progress(),
                detailJson, job.createdAt(), job.updatedAt()));
        } catch (JsonProcessingException ignored) { }
    }

    private JobRecord loadFromStore(JobEntity rec) {
        try {
            if (rec.detail() != null && !rec.detail().isEmpty()) {
                var detail = MAPPER.readValue(rec.detail(), JobDetailV1.class);
                return new JobRecord(rec.id(), rec.name(),
                    JobStatus.fromStoreValue(rec.status()),
                    rec.progress() != null ? rec.progress() : 0, rec.detail(),
                    detail.retry() != null ? detail.retry() : new JobRetryMetadata(),
                    detail.history() != null ? detail.history() : List.of(),
                    rec.createdAt(), rec.updatedAt());
            }
        } catch (JsonProcessingException ignored) { }
        return new JobRecord(rec.id(), rec.name(),
            JobStatus.fromStoreValue(rec.status()),
            rec.progress() != null ? rec.progress() : 0, rec.detail(),
            new JobRetryMetadata(), List.of(), rec.createdAt(), rec.updatedAt());
    }

    private static long nowTs() {
        return System.currentTimeMillis() / 1000;
    }
}
