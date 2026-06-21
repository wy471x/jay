package com.jay.state.support;

import com.jay.state.model.*;
import com.jay.state.store.*;

import org.springframework.data.repository.CrudRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory test implementations of state repositories.
 */
public class TestRepositories {

    private abstract static class AbstractRepo<T, ID> implements CrudRepository<T, ID> {
        @Override public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
            entities.forEach(e -> save(e)); return entities;
        }
    }

    public static class TestThreadRepository extends AbstractRepo<ThreadEntity, String>
            implements ThreadRepository {
        final Map<String, ThreadEntity> data = new ConcurrentHashMap<>();

        @Override public <S extends ThreadEntity> S save(S e) { data.put(e.id(), e); return e; }
        @Override public Optional<ThreadEntity> findById(String id) { return Optional.ofNullable(data.get(id)); }
        @Override public boolean existsById(String id) { return data.containsKey(id); }
        @Override public List<ThreadEntity> findAll() { return List.copyOf(data.values()); }
        @Override public List<ThreadEntity> findAllById(Iterable<String> ids) {
            var r = new ArrayList<ThreadEntity>(); ids.forEach(id -> findById(id).ifPresent(r::add)); return r;
        }
        @Override public long count() { return data.size(); }
        @Override public void deleteById(String id) { data.remove(id); }
        @Override public void delete(ThreadEntity e) { data.remove(e.id()); }
        @Override public void deleteAllById(Iterable<? extends String> ids) { ids.forEach(data::remove); }
        @Override public void deleteAll(Iterable<? extends ThreadEntity> es) { es.forEach(e -> data.remove(e.id())); }
        @Override public void deleteAll() { data.clear(); }

        @Override public List<ThreadEntity> listActive(int limit) {
            return data.values().stream().filter(t -> !t.archived())
                    .sorted(Comparator.comparingLong(ThreadEntity::updatedAt).reversed()).limit(limit).toList();
        }
        @Override public List<ThreadEntity> listAll(int limit) {
            return data.values().stream()
                    .sorted(Comparator.comparingLong(ThreadEntity::updatedAt).reversed()).limit(limit).toList();
        }
        @Override public void archive(String id, long at) {
            data.computeIfPresent(id, (k, t) -> with(t, true, at));
        }
        @Override public void unarchive(String id) {
            data.computeIfPresent(id, (k, t) -> with(t, false, null));
        }
        @Override public void setName(String id, String title) {
            data.computeIfPresent(id, (k, t) -> withTitle(t, title));
        }
        @Override public void clearName(String id) { setName(id, null); }
        @Override public void setCurrentLeafId(String id, Long leafId) {
            data.computeIfPresent(id, (k, t) -> withLeaf(t, leafId));
        }
        @Override public void setMemoryMode(String id, String mode) {
            data.computeIfPresent(id, (k, t) -> new ThreadEntity(t.id(), t.rolloutPath(), t.preview(),
                    t.ephemeral(), t.modelProvider(), t.createdAt(), t.updatedAt(), t.status(),
                    t.path(), t.cwd(), t.cliVersion(), t.source(), t.title(),
                    t.sandboxPolicy(), t.approvalMode(), t.archived(), t.archivedAt(),
                    t.gitSha(), t.gitBranch(), t.gitOriginUrl(), mode, t.currentLeafId()));
        }
        @Override public String getMemoryMode(String id) {
            var t = data.get(id);
            return t != null ? t.memoryMode() : null;
        }
        private ThreadEntity with(ThreadEntity t, boolean archived, Long at) {
            return new ThreadEntity(t.id(), t.rolloutPath(), t.preview(), t.ephemeral(), t.modelProvider(),
                    t.createdAt(), t.updatedAt(), t.status(), t.path(), t.cwd(), t.cliVersion(), t.source(),
                    t.title(), t.sandboxPolicy(), t.approvalMode(), archived, at,
                    t.gitSha(), t.gitBranch(), t.gitOriginUrl(), t.memoryMode(), t.currentLeafId());
        }
        private ThreadEntity withTitle(ThreadEntity t, String title) {
            return new ThreadEntity(t.id(), t.rolloutPath(), t.preview(), t.ephemeral(), t.modelProvider(),
                    t.createdAt(), t.updatedAt(), t.status(), t.path(), t.cwd(), t.cliVersion(), t.source(),
                    title, t.sandboxPolicy(), t.approvalMode(), t.archived(), t.archivedAt(),
                    t.gitSha(), t.gitBranch(), t.gitOriginUrl(), t.memoryMode(), t.currentLeafId());
        }
        private ThreadEntity withLeaf(ThreadEntity t, Long leaf) {
            return new ThreadEntity(t.id(), t.rolloutPath(), t.preview(), t.ephemeral(), t.modelProvider(),
                    t.createdAt(), t.updatedAt(), t.status(), t.path(), t.cwd(), t.cliVersion(), t.source(),
                    t.title(), t.sandboxPolicy(), t.approvalMode(), t.archived(), t.archivedAt(),
                    t.gitSha(), t.gitBranch(), t.gitOriginUrl(), t.memoryMode(), leaf);
        }
    }

    public static class TestMessageRepository extends AbstractRepo<MessageEntity, Long>
            implements MessageRepository {
        final Map<Long, MessageEntity> data = new ConcurrentHashMap<>();
        final AtomicLong seq = new AtomicLong(1);

        @Override public <S extends MessageEntity> S save(S e) {
            long id = e.id() != null ? e.id() : seq.getAndIncrement();
            @SuppressWarnings("unchecked") var s = (S) new MessageEntity(id, e.threadId(), e.role(),
                    e.content(), e.itemJson(), e.createdAt(), e.parentEntryId());
            data.put(id, s); return s;
        }
        @Override public Optional<MessageEntity> findById(Long id) { return Optional.ofNullable(data.get(id)); }
        @Override public boolean existsById(Long id) { return data.containsKey(id); }
        @Override public List<MessageEntity> findAll() { return List.copyOf(data.values()); }
        @Override public List<MessageEntity> findAllById(Iterable<Long> ids) {
            var r = new ArrayList<MessageEntity>(); ids.forEach(id -> findById(id).ifPresent(r::add)); return r;
        }
        @Override public long count() { return data.size(); }
        @Override public void deleteById(Long id) { data.remove(id); }
        @Override public void delete(MessageEntity e) { data.remove(e.id()); }
        @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(data::remove); }
        @Override public void deleteAll(Iterable<? extends MessageEntity> es) { es.forEach(e -> data.remove(e.id())); }
        @Override public void deleteAll() { data.clear(); }

        @Override public List<MessageEntity> findByThreadId(String tid) {
            return data.values().stream().filter(m -> m.threadId().equals(tid))
                    .sorted(Comparator.comparingLong(MessageEntity::createdAt)).toList();
        }
        @Override public List<MessageEntity> findByThreadIdAfter(String tid, long after) {
            return data.values().stream().filter(m -> m.threadId().equals(tid) && m.id() > after)
                    .sorted(Comparator.comparingLong(MessageEntity::createdAt)).toList();
        }
        @Override public List<MessageEntity> findChildren(String tid, long pid) {
            return data.values().stream()
                    .filter(m -> m.threadId().equals(tid) && m.parentEntryId() != null && m.parentEntryId() == pid)
                    .toList();
        }
        @Override public List<MessageEntity> findLeafMessages(String tid) {
            var parentIds = new HashSet<Long>();
            data.values().stream()
                    .filter(m -> m.threadId().equals(tid) && m.parentEntryId() != null)
                    .forEach(m -> parentIds.add(m.parentEntryId()));
            return data.values().stream()
                    .filter(m -> m.threadId().equals(tid) && !parentIds.contains(m.id()))
                    .sorted(Comparator.comparingLong(MessageEntity::createdAt).reversed())
                    .toList();
        }
    }

    public static class TestCheckpointRepository extends AbstractRepo<CheckpointEntity, Void>
            implements CheckpointRepository {
        final Map<String, CheckpointEntity> data = new ConcurrentHashMap<>();
        private String key(String tid, String cid) { return tid + ":" + cid; }

        @Override public <S extends CheckpointEntity> S save(S e) { data.put(key(e.threadId(), e.checkpointId()), e); return e; }
        @Override public Optional<CheckpointEntity> findById(Void id) { return Optional.empty(); }
        @Override public boolean existsById(Void id) { return false; }
        @Override public List<CheckpointEntity> findAll() { return List.copyOf(data.values()); }
        @Override public List<CheckpointEntity> findAllById(Iterable<Void> ids) { return List.of(); }
        @Override public long count() { return data.size(); }
        @Override public void deleteById(Void id) {}
        @Override public void delete(CheckpointEntity e) { data.remove(key(e.threadId(), e.checkpointId())); }
        @Override public void deleteAllById(Iterable<? extends Void> ids) {}
        @Override public void deleteAll(Iterable<? extends CheckpointEntity> es) { es.forEach(this::delete); }
        @Override public void deleteAll() { data.clear(); }
        @Override public List<CheckpointEntity> findByThreadId(String tid) {
            return data.values().stream().filter(c -> c.threadId().equals(tid))
                    .sorted(Comparator.comparingLong(CheckpointEntity::createdAt).reversed()).toList();
        }
        @Override public CheckpointEntity findOne(String tid, String cid) { return data.get(key(tid, cid)); }
        @Override public void deleteOne(String tid, String cid) { data.remove(key(tid, cid)); }
        @Override public void deleteByThreadId(String tid) { data.entrySet().removeIf(e -> e.getValue().threadId().equals(tid)); }
    }

    public static class TestJobRepository extends AbstractRepo<JobEntity, String>
            implements JobRepository {
        final Map<String, JobEntity> data = new ConcurrentHashMap<>();
        @Override public <S extends JobEntity> S save(S e) { data.put(e.id(), e); return e; }
        @Override public Optional<JobEntity> findById(String id) { return Optional.ofNullable(data.get(id)); }
        @Override public boolean existsById(String id) { return data.containsKey(id); }
        @Override public List<JobEntity> findAll() { return List.copyOf(data.values()); }
        @Override public List<JobEntity> findAllById(Iterable<String> ids) {
            var r = new ArrayList<JobEntity>(); ids.forEach(id -> findById(id).ifPresent(r::add)); return r;
        }
        @Override public long count() { return data.size(); }
        @Override public void deleteById(String id) { data.remove(id); }
        @Override public void delete(JobEntity e) { data.remove(e.id()); }
        @Override public void deleteAllById(Iterable<? extends String> ids) { ids.forEach(data::remove); }
        @Override public void deleteAll(Iterable<? extends JobEntity> es) { es.forEach(e -> data.remove(e.id())); }
        @Override public void deleteAll() { data.clear(); }
        @Override public List<JobEntity> listRecent(int limit) {
            return data.values().stream()
                    .sorted(Comparator.comparingLong(JobEntity::updatedAt).reversed()).limit(limit).toList();
        }
    }

    public static class TestGoalRepository extends AbstractRepo<GoalEntity, String>
            implements GoalRepository {
        final Map<String, GoalEntity> data = new ConcurrentHashMap<>();
        @Override public <S extends GoalEntity> S save(S e) { data.put(e.threadId(), e); return e; }
        @Override public Optional<GoalEntity> findById(String tid) { return Optional.ofNullable(data.get(tid)); }
        @Override public boolean existsById(String tid) { return data.containsKey(tid); }
        @Override public List<GoalEntity> findAll() { return List.copyOf(data.values()); }
        @Override public List<GoalEntity> findAllById(Iterable<String> ids) {
            var r = new ArrayList<GoalEntity>(); ids.forEach(id -> findById(id).ifPresent(r::add)); return r;
        }
        @Override public long count() { return data.size(); }
        @Override public void deleteById(String tid) { data.remove(tid); }
        @Override public void delete(GoalEntity e) { data.remove(e.threadId()); }
        @Override public void deleteAllById(Iterable<? extends String> ids) { ids.forEach(data::remove); }
        @Override public void deleteAll(Iterable<? extends GoalEntity> es) { es.forEach(e -> data.remove(e.threadId())); }
        @Override public void deleteAll() { data.clear(); }
        @Override public void recordProgress(String tid, long tokens, long seconds, boolean cont, long now) {
            data.computeIfPresent(tid, (id, g) -> new GoalEntity(g.threadId(), g.goalId(), g.objective(),
                    g.status(), g.tokenBudget(), g.tokensUsed() + tokens, g.timeUsedSeconds() + seconds,
                    g.continuationCount() + (cont ? 1 : 0), g.createdAt(), now));
        }
    }

    public static class TestDynamicToolRepository extends AbstractRepo<DynamicToolEntity, Void>
            implements DynamicToolRepository {
        final Map<String, DynamicToolEntity> data = new ConcurrentHashMap<>();
        private String key(String tid, int pos) { return tid + ":" + pos; }
        @Override public <S extends DynamicToolEntity> S save(S e) { data.put(key(e.threadId(), e.position()), e); return e; }
        @Override public Optional<DynamicToolEntity> findById(Void id) { return Optional.empty(); }
        @Override public boolean existsById(Void id) { return false; }
        @Override public List<DynamicToolEntity> findAll() { return List.copyOf(data.values()); }
        @Override public List<DynamicToolEntity> findAllById(Iterable<Void> ids) { return List.of(); }
        @Override public long count() { return data.size(); }
        @Override public void deleteById(Void id) {}
        @Override public void delete(DynamicToolEntity e) { data.remove(key(e.threadId(), e.position())); }
        @Override public void deleteAllById(Iterable<? extends Void> ids) {}
        @Override public void deleteAll(Iterable<? extends DynamicToolEntity> es) { es.forEach(this::delete); }
        @Override public void deleteAll() { data.clear(); }
        @Override public List<DynamicToolEntity> findByThreadId(String tid) {
            return data.values().stream().filter(t -> t.threadId().equals(tid))
                    .sorted(Comparator.comparingInt(DynamicToolEntity::position)).toList();
        }
        @Override public void deleteByThreadId(String tid) {
            data.entrySet().removeIf(e -> e.getValue().threadId().equals(tid));
        }
    }
}
