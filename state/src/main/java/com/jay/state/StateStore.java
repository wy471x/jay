package com.jay.state;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Persistent state store backed by SQLite with Flyway migrations.
 * Equivalent to Rust's StateStore — provides CRUD for threads,
 * messages, checkpoints, jobs, goals, and dynamic tools.
 * <p>
 * Spring Data JDBC replaces ~1,200 lines of hand-written SQL
 * from the Rust state crate with declarative repository interfaces.
 */
@Service
@Transactional
public class StateStore {

    private final ThreadRepository threads;
    private final MessageRepository messages;
    private final CheckpointRepository checkpoints;
    private final JobRepository jobs;
    private final GoalRepository goals;
    private final DynamicToolRepository dynamicTools;

    public StateStore(ThreadRepository threads, MessageRepository messages,
                      CheckpointRepository checkpoints, JobRepository jobs,
                      GoalRepository goals, DynamicToolRepository dynamicTools) {
        this.threads = threads;
        this.messages = messages;
        this.checkpoints = checkpoints;
        this.jobs = jobs;
        this.goals = goals;
        this.dynamicTools = dynamicTools;
    }

    // ── Threads ───────────────────────────────────────────────

    public ThreadEntity upsertThread(ThreadEntity thread) {
        return threads.save(thread);
    }

    @Transactional(readOnly = true)
    public Optional<ThreadEntity> readThread(String id) {
        return threads.findById(id);
    }

    @Transactional(readOnly = true)
    public List<ThreadEntity> listThreads(boolean includeArchived, int limit) {
        return includeArchived ? threads.listAll(limit) : threads.listActive(limit);
    }

    public void archiveThread(String id, long archivedAt) {
        threads.archive(id, archivedAt);
    }

    public void unarchiveThread(String id) {
        threads.unarchive(id);
    }

    public void setThreadName(String id, String name) {
        threads.setName(id, name);
    }

    public void clearThreadName(String id) {
        threads.clearName(id);
    }

    public void setCurrentLeafId(String threadId, long leafId) {
        threads.setCurrentLeafId(threadId, leafId);
    }

    // ── Messages ──────────────────────────────────────────────

    public MessageEntity appendMessage(MessageEntity message) {
        var saved = messages.save(message);
        setCurrentLeafId(message.threadId(), saved.id());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<MessageEntity> listMessages(String threadId) {
        return messages.findByThreadId(threadId);
    }

    @Transactional(readOnly = true)
    public List<MessageEntity> listMessagesAfter(String threadId, long afterId) {
        return messages.findByThreadIdAfter(threadId, afterId);
    }

    @Transactional(readOnly = true)
    public List<MessageEntity> listMessageChildren(String threadId, long parentId) {
        return messages.findChildren(threadId, parentId);
    }

    // ── Checkpoints ───────────────────────────────────────────

    public void saveCheckpoint(String threadId, String checkpointId, String stateJson, long now) {
        checkpoints.save(new CheckpointEntity(threadId, checkpointId, stateJson, now));
    }

    @Transactional(readOnly = true)
    public Optional<CheckpointEntity> loadCheckpoint(String threadId, String checkpointId) {
        return Optional.ofNullable(checkpoints.findOne(threadId, checkpointId));
    }

    @Transactional(readOnly = true)
    public List<CheckpointEntity> listCheckpoints(String threadId) {
        return checkpoints.findByThreadId(threadId);
    }

    public void deleteCheckpoint(String threadId, String checkpointId) {
        checkpoints.deleteOne(threadId, checkpointId);
    }

    // ── Jobs ──────────────────────────────────────────────────

    public JobEntity upsertJob(JobEntity job) {
        return jobs.save(job);
    }

    @Transactional(readOnly = true)
    public Optional<JobEntity> readJob(String id) {
        return jobs.findById(id);
    }

    @Transactional(readOnly = true)
    public List<JobEntity> listRecentJobs(int limit) {
        return jobs.listRecent(limit);
    }

    // ── Thread Goals ──────────────────────────────────────────

    public GoalEntity upsertGoal(GoalEntity goal) {
        return goals.save(goal);
    }

    @Transactional(readOnly = true)
    public Optional<GoalEntity> readGoal(String threadId) {
        return goals.findById(threadId);
    }

    public void clearGoal(String threadId) {
        goals.deleteById(threadId);
    }

    public void recordGoalProgress(String threadId, long tokenDelta, long timeDeltaSeconds,
                                    boolean isContinuation, long now) {
        goals.recordProgress(threadId, tokenDelta, timeDeltaSeconds, isContinuation, now);
    }

    // ── Dynamic Tools ─────────────────────────────────────────

    public void saveDynamicTools(String threadId, List<DynamicToolEntity> tools) {
        dynamicTools.deleteByThreadId(threadId);
        for (var tool : tools) {
            dynamicTools.save(tool);
        }
    }

    @Transactional(readOnly = true)
    public List<DynamicToolEntity> listDynamicTools(String threadId) {
        return dynamicTools.findByThreadId(threadId);
    }
}
