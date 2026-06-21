package com.jay.state.index;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Append-only JSONL session index for fast name-based thread lookups
 * without opening the SQLite database.
 * Equivalent to Rust's session_index.jsonl management.
 */
public class SessionIndexManager {

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private final Path indexPath;

    public SessionIndexManager(Path indexPath) {
        this.indexPath = indexPath;
    }

    public Path indexPath() {
        return indexPath;
    }

    // ── Write ──────────────────────────────────────────────────

    /**
     * Append a session index entry for a thread.
     */
    public void appendThreadName(String threadId, String threadName,
                                  long updatedAt, String rolloutPath) {
        try {
            Files.createDirectories(indexPath.getParent());
            var entry = new SessionIndexEntry(threadId, threadName, updatedAt, rolloutPath);
            String line = jsonMapper.writeValueAsString(entry);
            Files.writeString(indexPath, line + "\n",
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ── Read ───────────────────────────────────────────────────

    /** Find a thread name by ID, using the session index. */
    public Optional<String> findThreadNameById(String threadId) {
        return loadIndex().values().stream()
            .filter(e -> e.threadId().equals(threadId))
            .findFirst()
            .map(SessionIndexEntry::threadName);
    }

    /** Look up thread names for multiple IDs at once. */
    public Map<String, String> findThreadNamesByIds(List<String> ids) {
        var index = loadIndex();
        Map<String, String> out = new HashMap<>();
        for (String id : ids) {
            for (var entry : index.values()) {
                if (entry.threadId().equals(id)) {
                    out.put(id, entry.threadName());
                    break;
                }
            }
        }
        return out;
    }

    /**
     * Find the rollout path for a thread by name (case-insensitive).
     * If multiple threads share the same name, the most recently updated is returned.
     */
    public Optional<String> findThreadPathByName(String name) {
        return loadIndex().values().stream()
            .filter(e -> name.equalsIgnoreCase(e.threadName()))
            .max(Comparator.comparingLong(SessionIndexEntry::updatedAt))
            .map(SessionIndexEntry::rolloutPath)
            .filter(Objects::nonNull);
    }

    // ── Internal ───────────────────────────────────────────────

    private Map<String, SessionIndexEntry> loadIndex() {
        Map<String, SessionIndexEntry> map = new LinkedHashMap<>();
        if (!Files.exists(indexPath)) return map;
        try {
            for (String line : Files.readAllLines(indexPath)) {
                if (line.isBlank()) continue;
                try {
                    var entry = jsonMapper.readValue(line, SessionIndexEntry.class);
                    map.put(entry.threadId(), entry);  // last write wins
                } catch (JsonProcessingException ignored) {
                    // skip corrupt lines
                }
            }
        } catch (IOException ignored) {
            // return empty map
        }
        return map;
    }

    // ── Inner type ─────────────────────────────────────────────

    record SessionIndexEntry(String threadId, String threadName, long updatedAt, String rolloutPath) {}
}
