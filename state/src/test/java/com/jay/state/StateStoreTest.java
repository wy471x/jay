package com.jay.state;

import com.jay.state.support.TestRepositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StateStore CRUD operations — ported from Rust state tests.
 */
class StateStoreTest {

    private static final long NOW = 1700000000L;

    private TestThreadRepository threads;
    private TestMessageRepository messages;
    private TestCheckpointRepository checkpoints;
    private TestJobRepository jobs;
    private TestGoalRepository goals;
    private TestDynamicToolRepository dynamicTools;
    private StateStore store;

    @BeforeEach
    void setUp() {
        threads = new TestThreadRepository();
        messages = new TestMessageRepository();
        checkpoints = new TestCheckpointRepository();
        jobs = new TestJobRepository();
        goals = new TestGoalRepository();
        dynamicTools = new TestDynamicToolRepository();
        store = new StateStore(threads, messages, checkpoints, jobs, goals, dynamicTools);
    }

    // ── Thread operations ────────────────────────────────────

    @Test
    void upsertAndReadThread() {
        var thread = newThread("thr-1", "preview text", "deepseek", "running");
        store.upsertThread(thread);

        var found = store.readThread("thr-1");
        assertTrue(found.isPresent());
        assertEquals("thr-1", found.orElseThrow().id());
        assertEquals("preview text", found.orElseThrow().preview());
        assertEquals("deepseek", found.orElseThrow().modelProvider());
    }

    @Test
    void upsertThreadUpdatesExisting() {
        var thread = newThread("thr-1", "first", "deepseek", "running");
        store.upsertThread(thread);

        var updated = new ThreadEntity("thr-1", null, "second", false, "anthropic",
                NOW, NOW + 100, "completed", null, "/tmp", "0.2.0", "interactive",
                null, null, null, false, null, null, null, null, null, null);
        store.upsertThread(updated);

        var found = store.readThread("thr-1").orElseThrow();
        assertEquals("second", found.preview());
        assertEquals("anthropic", found.modelProvider());
        assertEquals("completed", found.status());
    }

    @Test
    void listThreadsExcludesArchivedByDefault() {
        store.upsertThread(newThread("thr-1", "active", "deepseek", "running"));
        store.upsertThread(newThread("thr-2", "also active", "openai", "idle"));
        var archived = new ThreadEntity("thr-3", null, "archived", false, "deepseek",
                NOW, NOW, "completed", null, "/tmp", "0.1.0", "interactive",
                null, null, null, true, NOW, null, null, null, null, null);
        store.upsertThread(archived);

        var active = store.listThreads(false, 50);
        assertEquals(2, active.size());
        assertTrue(active.stream().noneMatch(t -> t.id().equals("thr-3")));
    }

    @Test
    void listThreadsIncludesArchivedWhenRequested() {
        store.upsertThread(newThread("thr-1", "active", "deepseek", "running"));
        var archived = new ThreadEntity("thr-2", null, "archived", false, "deepseek",
                NOW, NOW, "completed", null, "/tmp", "0.1.0", "interactive",
                null, null, null, true, NOW, null, null, null, null, null);
        store.upsertThread(archived);

        var all = store.listThreads(true, 50);
        assertEquals(2, all.size());
    }

    @Test
    void archiveAndUnarchiveThread() {
        store.upsertThread(newThread("thr-1", "test", "deepseek", "running"));
        store.archiveThread("thr-1", NOW);

        var found = store.readThread("thr-1").orElseThrow();
        assertTrue(found.archived());
        assertEquals(NOW, found.archivedAt());

        store.unarchiveThread("thr-1");
        found = store.readThread("thr-1").orElseThrow();
        assertFalse(found.archived());
    }

    @Test
    void setAndClearThreadName() {
        store.upsertThread(newThread("thr-1", "test", "deepseek", "running"));
        store.setThreadName("thr-1", "My Thread");

        var found = store.readThread("thr-1").orElseThrow();
        assertEquals("My Thread", found.title());

        store.clearThreadName("thr-1");
        found = store.readThread("thr-1").orElseThrow();
        assertNull(found.title());
    }

    @Test
    void threadStoresFullMetadata() {
        var thread = new ThreadEntity("thr-full", "/tmp/rollout.jsonl", "full preview",
                false, "anthropic", NOW, NOW, "running", "/home/user", "/workspace",
                "0.1.0", "api", "Custom Title", "workspace-write", "on-request",
                false, null, "abc1234", "main", "git@github.com:foo/bar.git",
                "local", null);
        store.upsertThread(thread);

        var found = store.readThread("thr-full").orElseThrow();
        assertEquals("Custom Title", found.title());
        assertEquals("api", found.source());
        assertEquals("abc1234", found.gitSha());
        assertEquals("main", found.gitBranch());
    }

    // ── Message operations ───────────────────────────────────

    @Test
    void appendAndListMessages() {
        store.upsertThread(newThread("thr-1", "test", "deepseek", "running"));

        var msg1 = new MessageEntity(null, "thr-1", "user", "hello", null, NOW, null);
        var msg2 = new MessageEntity(null, "thr-1", "assistant", "hi there", null, NOW + 1, null);

        var saved1 = store.appendMessage(msg1);
        var saved2 = store.appendMessage(msg2);

        var all = store.listMessages("thr-1");
        assertEquals(2, all.size());
        assertEquals("user", all.get(0).role());
        assertEquals("assistant", all.get(1).role());
        assertEquals("hello", all.get(0).content());
    }

    @Test
    void messageTreeStructurePreservesParent() {
        store.upsertThread(newThread("thr-1", "test", "deepseek", "running"));

        var root = store.appendMessage(
                new MessageEntity(null, "thr-1", "user", "root", null, NOW, null));

        var child = store.appendMessage(
                new MessageEntity(null, "thr-1", "assistant", "child", null, NOW + 1, root.id()));

        var children = store.listMessageChildren("thr-1", root.id());
        assertEquals(1, children.size());
        assertEquals("child", children.get(0).content());
        assertEquals(root.id(), children.get(0).parentEntryId());
    }

    @Test
    void listMessagesAfterExcludesEarlier() {
        store.upsertThread(newThread("thr-1", "test", "deepseek", "running"));

        var m1 = store.appendMessage(
                new MessageEntity(null, "thr-1", "user", "first", null, NOW, null));
        store.appendMessage(
                new MessageEntity(null, "thr-1", "assistant", "second", null, NOW + 1, null));

        var after = store.listMessagesAfter("thr-1", m1.id());
        assertEquals(1, after.size());
        assertEquals("second", after.get(0).content());
    }

    // ── Checkpoint operations ────────────────────────────────

    @Test
    void saveAndLoadCheckpoint() {
        store.saveCheckpoint("thr-1", "cp-1", "{\"state\":\"snapshot\"}", NOW);

        var loaded = store.loadCheckpoint("thr-1", "cp-1");
        assertTrue(loaded.isPresent());
        assertEquals("thr-1", loaded.get().threadId());
        assertEquals("cp-1", loaded.get().checkpointId());
        assertEquals("{\"state\":\"snapshot\"}", loaded.get().stateJson());
    }

    @Test
    void listAndDeleteCheckpoints() {
        store.saveCheckpoint("thr-1", "cp-1", "{}", NOW);
        store.saveCheckpoint("thr-1", "cp-2", "{}", NOW + 100);

        var list = store.listCheckpoints("thr-1");
        assertEquals(2, list.size());

        store.deleteCheckpoint("thr-1", "cp-1");
        assertEquals(1, store.listCheckpoints("thr-1").size());
    }

    // ── Job operations ───────────────────────────────────────

    @Test
    void upsertAndReadJob() {
        var job = new JobEntity("job-1", "lint-check", "running", 50, "processing...", NOW, NOW);
        store.upsertJob(job);

        var found = store.readJob("job-1");
        assertTrue(found.isPresent());
        assertEquals("lint-check", found.orElseThrow().name());
        assertEquals(50, found.orElseThrow().progress());
    }

    @Test
    void listRecentJobs() {
        store.upsertJob(new JobEntity("j1", "task-a", "completed", 100, null, NOW - 100, NOW));
        store.upsertJob(new JobEntity("j2", "task-b", "running", 30, null, NOW, NOW));

        var recent = store.listRecentJobs(10);
        assertTrue(recent.size() >= 2);
    }

    // ── Goal operations ──────────────────────────────────────

    @Test
    void upsertAndReadGoal() {
        var goal = new GoalEntity("thr-1", "goal-1", "Release v1.0", "active",
                10000L, 5000L, 3600L, 3L, NOW, NOW);
        store.upsertGoal(goal);

        var found = store.readGoal("thr-1");
        assertTrue(found.isPresent());
        assertEquals("Release v1.0", found.orElseThrow().objective());
        assertEquals(5000L, found.orElseThrow().tokensUsed());
    }

    @Test
    void recordGoalProgress() {
        store.upsertGoal(new GoalEntity("thr-1", "goal-1", "obj", "active",
                10000L, 1000L, 600L, 1L, NOW, NOW));

        store.recordGoalProgress("thr-1", 500, 300, true, NOW + 100);

        var found = store.readGoal("thr-1").orElseThrow();
        assertEquals(1500L, found.tokensUsed());
        assertEquals(900L, found.timeUsedSeconds());
        assertEquals(2L, found.continuationCount());
    }

    @Test
    void clearGoal() {
        store.upsertGoal(new GoalEntity("thr-1", "goal-1", "obj", "active",
                null, 0L, 0L, 0L, NOW, NOW));
        assertTrue(store.readGoal("thr-1").isPresent());

        store.clearGoal("thr-1");
        assertFalse(store.readGoal("thr-1").isPresent());
    }

    // ── Dynamic tool operations ──────────────────────────────

    @Test
    void saveAndListDynamicTools() {
        var tools = List.of(
                new DynamicToolEntity("thr-1", 0, "tool-a", "desc a", "{\"type\":\"object\"}"),
                new DynamicToolEntity("thr-1", 1, "tool-b", "desc b", "{\"type\":\"object\"}"));

        store.saveDynamicTools("thr-1", tools);

        var loaded = store.listDynamicTools("thr-1");
        assertEquals(2, loaded.size());
        assertEquals("tool-a", loaded.get(0).name());
        assertEquals("tool-b", loaded.get(1).name());
    }

    @Test
    void saveDynamicToolsReplacesExisting() {
        store.saveDynamicTools("thr-1",
                List.of(new DynamicToolEntity("thr-1", 0, "tool-a", "desc", "{}")));
        assertEquals(1, store.listDynamicTools("thr-1").size());

        store.saveDynamicTools("thr-1",
                List.of(new DynamicToolEntity("thr-1", 0, "tool-c", "new desc", "{}")));
        assertEquals(1, store.listDynamicTools("thr-1").size());
        assertEquals("tool-c", store.listDynamicTools("thr-1").get(0).name());
    }

    // ── helpers ──────────────────────────────────────────────

    private ThreadEntity newThread(String id, String preview, String provider, String status) {
        return new ThreadEntity(id, null, preview, false, provider, NOW, NOW, status,
                null, "/tmp", "0.1.0", "interactive", null, null, null,
                false, null, null, null, null, null, null);
    }
}
