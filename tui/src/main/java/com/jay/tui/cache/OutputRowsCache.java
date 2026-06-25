package com.jay.tui.cache;

import java.util.*;

/**
 * Content-addressed LRU cache for rendered tool output rows.
 * Mirrors Rust {@code OutputRowsCache}.
 *
 * <p>Keys are {@code (contentHash, width)} tuples, values are rendered
 * line arrays. Eviction is LRU, max 256 entries.
 */
public class OutputRowsCache {

    private static final int DEFAULT_CAPACITY = 256;

    // Singleton instance (thread-safe)
    private static final OutputRowsCache INSTANCE = new OutputRowsCache();

    private final int capacity;
    private final Map<Key, List<String>> cache;
    private final Deque<Key> order;

    public OutputRowsCache() {
        this(DEFAULT_CAPACITY);
    }

    public OutputRowsCache(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>();
        this.order = new ArrayDeque<>();
    }

    public static OutputRowsCache instance() { return INSTANCE; }

    /** Look up rendered rows by content hash and width. */
    public synchronized Optional<List<String>> get(String contentHash, int width) {
        var key = new Key(contentHash, width);
        var result = cache.get(key);
        if (result != null) {
            // Move to front (LRU)
            order.remove(key);
            order.addFirst(key);
        }
        return Optional.ofNullable(result);
    }

    /** Store rendered rows. */
    public synchronized void put(String contentHash, int width, List<String> rows) {
        var key = new Key(contentHash, width);
        // If already exists, replace in-place without evicting
        if (cache.containsKey(key)) {
            cache.put(key, rows);
            return;
        }
        // Evict oldest if at capacity
        if (cache.size() >= capacity) {
            var oldest = order.pollLast();
            if (oldest != null) cache.remove(oldest);
        }
        cache.put(key, Collections.unmodifiableList(rows));
        order.addFirst(key);
    }

    /** Clear all cached entries. */
    public synchronized void clear() {
        cache.clear();
        order.clear();
    }

    public synchronized int size() {
        return cache.size();
    }

    /** Compute a content hash for cache key. */
    public static String contentHash(String content) {
        if (content == null) return "null";
        // Simple fast hash — good enough for cache keys
        int h = 0;
        for (int i = 0; i < content.length(); i++) {
            h = 31 * h + content.charAt(i);
        }
        return Integer.toHexString(h);
    }

    private record Key(String contentHash, int width) {}
}
