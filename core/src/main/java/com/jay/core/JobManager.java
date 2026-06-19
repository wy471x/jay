package com.jay.core;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle of agent jobs (conversation sessions).
 * Each job tracks its state, associated thread, and metadata.
 */
public class JobManager {

    private final Map<String, Job> jobs = new ConcurrentHashMap<>();
    private final ThreadManager threadManager;

    public JobManager(ThreadManager threadManager) {
        this.threadManager = threadManager;
    }

    public Job create(String sessionId) {
        var job = new Job(UUID.randomUUID().toString(), sessionId, Job.State.CREATED);
        jobs.put(job.id(), job);
        return job;
    }

    public Optional<Job> get(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    public void updateState(String jobId, Job.State state) {
        jobs.computeIfPresent(jobId, (id, job) -> job.withState(state));
    }

    public record Job(
            String id,
            String sessionId,
            State state
    ) {
        public enum State { CREATED, RUNNING, WAITING_FOR_TOOL, WAITING_FOR_USER, COMPLETED, ERROR }
        public Job withState(State newState) { return new Job(id, sessionId, newState); }
    }
}
