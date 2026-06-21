package com.jay.secrets;

/**
 * Abstract secret store interface. Equivalent to Rust's KeyringStore trait.
 *
 * <p>Implementations: {@link FileKeyringStore} (JSON-on-disk),
 * {@link InMemoryKeyringStore} (test double).
 */
public interface KeyringStore {

    /** Read a secret by key. Returns null if no entry exists. */
    String get(String key) throws SecretsError;

    /** Write a secret, replacing any existing value for the same key. */
    void set(String key, String value) throws SecretsError;

    /** Remove a secret by key. No-op if already absent. */
    void delete(String key) throws SecretsError;

    /** Short human-readable label for this backend. */
    String backendName();
}
