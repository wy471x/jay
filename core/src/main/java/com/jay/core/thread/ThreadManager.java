package com.jay.core.thread;

import com.jay.protocol.core.Thread;
import com.jay.protocol.core.ThreadStatus;
import com.jay.protocol.core.SessionSource;
import com.jay.protocol.core.ThreadGoal;
import com.jay.protocol.core.ThreadResponse;
import com.jay.protocol.params.*;
import com.jay.state.model.*;
import com.jay.state.store.StateStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages conversation thread lifecycle: create, resume, fork, message, goal tracking,
 * archival, and persistent state. Equivalent to Rust's ThreadManager.
 */
public class ThreadManager {

    private final StateStore store;
    private final ConcurrentHashMap<String, Thread> runningThreads = new ConcurrentHashMap<>();
    private final String cliVersion;

    public ThreadManager(StateStore store, String cliVersion) {
        this.store = store;
        this.cliVersion = cliVersion;
    }

    public StateStore stateStore() { return store; }

    // ── Spawn ──────────────────────────────────────────────────

    /** Create a new thread with initial history. */
    public NewThread spawnThreadWithHistory(String modelProvider, String cwd,
                                             InitialHistory history) {
        String id = "thread-" + UUID.randomUUID();
        String preview = generatePreview(history);
        long now = nowTs();

        var entity = new ThreadEntity(id, "rollout/" + id, preview, false,
            modelProvider, now, now, "running", null, cwd, cliVersion, "interactive",
            null, null, null, false, null,
            null, null, null, null, null);
        store.upsertThread(entity);

        var thread = toProtocolThread(entity);
        runningThreads.put(id, thread);
        return new NewThread(id, null, modelProvider, cwd, null, null);
    }

    // ── Resume ─────────────────────────────────────────────────

    /** Resume an existing thread, optionally with history. */
    public Thread resumeThreadWithHistory(ThreadResumeParams params, String fallbackCwd) {
        var cached = runningThreads.get(params.threadId());
        if (cached != null) return cached;

        var stored = store.readThread(params.threadId())
            .orElseThrow(() -> new NoSuchElementException("Thread not found: " + params.threadId()));

        var updated = new ThreadEntity(stored.id(), stored.rolloutPath(), stored.preview(),
            stored.ephemeral(), stored.modelProvider(), stored.createdAt(), nowTs(),
            "running", stored.path(), stored.cwd(), stored.cliVersion(), stored.source(),
            stored.title(), stored.sandboxPolicy(), stored.approvalMode(),
            stored.archived(), stored.archivedAt(),
            stored.gitSha(), stored.gitBranch(), stored.gitOriginUrl(),
            stored.memoryMode(), stored.currentLeafId());
        store.upsertThread(updated);

        var thread = toProtocolThread(updated);
        runningThreads.put(params.threadId(), thread);
        return thread;
    }

    // ── Fork ───────────────────────────────────────────────────

    /** Fork a new thread from an existing parent. */
    public NewThread forkThread(ThreadForkParams params, String fallbackCwd) {
        var parent = store.readThread(params.threadId())
            .orElseThrow(() -> new NoSuchElementException("Parent not found: " + params.threadId()));
        String cwd = params.cwd() != null ? params.cwd() : parent.cwd();
        String provider = params.modelProvider() != null ? params.modelProvider() : parent.modelProvider();
        return spawnThreadWithHistory(provider, cwd, InitialHistory.Forked.class.cast(
            InitialHistory.forked(params.threadId())));
    }

    // ── Message ────────────────────────────────────────────────

    /** Record a user message and update the thread preview. */
    public void touchMessage(String threadId, String input) {
        var stored = store.readThread(threadId).orElse(null);
        if (stored == null) return;

        long now = nowTs();
        store.appendMessage(new MessageEntity(null, threadId, "user", input, null, now, null));

        String preview = input.length() > 80 ? input.substring(0, 80) + "..." : input;
        var updated = new ThreadEntity(stored.id(), stored.rolloutPath(), preview,
            stored.ephemeral(), stored.modelProvider(), stored.createdAt(), now,
            stored.status(), stored.path(), stored.cwd(), stored.cliVersion(), stored.source(),
            stored.title(), stored.sandboxPolicy(), stored.approvalMode(),
            stored.archived(), stored.archivedAt(),
            stored.gitSha(), stored.gitBranch(), stored.gitOriginUrl(),
            stored.memoryMode(), stored.currentLeafId());
        store.upsertThread(updated);
        runningThreads.put(threadId, toProtocolThread(updated));
    }

    // ── Goal CRUD ──────────────────────────────────────────────

    public void upsertThreadGoal(String threadId, String objective, Long tokenBudget) {
        long now = nowTs();
        var goal = new GoalEntity(threadId, "goal-" + UUID.randomUUID(), objective,
            "active", tokenBudget, 0, 0, 0, now, now);
        store.upsertGoal(goal);
    }

    public Optional<GoalEntity> getThreadGoal(String threadId) {
        return store.readGoal(threadId);
    }

    public void recordGoalUsage(String threadId, long tokens, long seconds) {
        store.recordGoalProgress(threadId, tokens, seconds, false, nowTs());
    }

    public void recordGoalContinuation(String threadId) {
        store.recordGoalProgress(threadId, 0, 0, true, nowTs());
    }

    public boolean clearThreadGoal(String threadId) {
        var existing = store.readGoal(threadId);
        if (existing.isEmpty()) return false;
        store.clearGoal(threadId);
        return true;
    }

    // ── Archive / Unarchive ────────────────────────────────────

    public void archiveThread(String threadId) {
        store.archiveThread(threadId, nowTs());
        var cached = runningThreads.get(threadId);
        if (cached != null) {
            runningThreads.put(threadId, new Thread(cached.id(), cached.preview(),
                cached.ephemeral(), cached.modelProvider(), cached.createdAt(),
                nowTs(), com.jay.protocol.core.ThreadStatus.ARCHIVED, cached.path(), cached.cwd(),
                cached.cliVersion(), cached.source(), cached.name()));
        }
    }

    public void unarchiveThread(String threadId) {
        store.unarchiveThread(threadId);
    }

    // ── Query ──────────────────────────────────────────────────

    public List<Thread> listThreads(ThreadListParams params) {
        return store.listThreads(new ThreadListFilters(params.includeArchived(), params.limit()))
            .stream().map(this::toProtocolThread).toList();
    }

    public Optional<Thread> readThread(String threadId) {
        return store.readThread(threadId).map(this::toProtocolThread);
    }

    public void setThreadName(String threadId, String name) {
        store.setThreadName(threadId, name);
        var cached = runningThreads.get(threadId);
        if (cached != null) {
            runningThreads.put(threadId, new Thread(cached.id(), cached.preview(),
                cached.ephemeral(), cached.modelProvider(), cached.createdAt(),
                cached.updatedAt(), cached.status(), cached.path(), cached.cwd(),
                cached.cliVersion(), cached.source(), name));
        }
    }

    // ── Helpers ────────────────────────────────────────────────

    private Thread toProtocolThread(ThreadEntity e) {
        return new Thread(e.id(), e.preview(), e.ephemeral(), e.modelProvider(),
            e.createdAt(), e.updatedAt(),
            e.status() != null ? com.jay.protocol.core.ThreadStatus.valueOf(e.status().toUpperCase()) : com.jay.protocol.core.ThreadStatus.IDLE,
            e.path(), e.cwd(), e.cliVersion(),
            e.source() != null ? com.jay.protocol.core.SessionSource.valueOf(e.source().toUpperCase()) : com.jay.protocol.core.SessionSource.INTERACTIVE,
            e.title());
    }

    private String generatePreview(InitialHistory history) {
        return switch (history) {
            case InitialHistory.New ignored -> "New conversation";
            case InitialHistory.Forked ignored -> "Forked conversation";
            case InitialHistory.Resumed r -> "Resumed: " + r.conversationId();
        };
    }

    private static long nowTs() {
        return System.currentTimeMillis() / 1000;
    }
}
