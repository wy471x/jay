package com.jay.secrets;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * In-memory secret store for testing.
 * Equivalent to Rust's InMemoryKeyringStore.
 */
public class InMemoryKeyringStore implements KeyringStore {

    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, String> entries = new HashMap<>();

    @Override
    public String get(String key) throws SecretsError {
        lock.lock();
        try {
            return entries.get(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void set(String key, String value) throws SecretsError {
        lock.lock();
        try {
            entries.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void delete(String key) throws SecretsError {
        lock.lock();
        try {
            entries.remove(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String backendName() {
        return "in-memory";
    }
}
