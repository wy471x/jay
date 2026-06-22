package com.jay.secrets;

import java.nio.file.Path;
import java.util.Optional;

/**
 * High-level secret store facade. Wraps a {@link KeyringStore} backend and adds
 * layered resolution: store → environment variable → none.
 *
 * <p>Equivalent to Rust's {@code Secrets} struct.
 *
 * <h3>Backend selection (via {@link #autoDetect})</h3>
 * <ul>
 *   <li>{@code CODEWHALE_SECRET_BACKEND=file|local|json} or unset → file-backed</li>
 *   <li>{@code CODEWHALE_SECRET_BACKEND=system|keyring|os|os-keyring} → file-backed
 *       (OS keyring not available on JVM without native libs)</li>
 *   <li>{@code DEEPSEEK_SECRET_BACKEND} — legacy alias for above</li>
 * </ul>
 */
public class SecretsStore {

    static final String DEFAULT_SERVICE = "deepseek";
    private static final String SECRET_BACKEND_ENV = "CODEWHALE_SECRET_BACKEND";
    private static final String LEGACY_SECRET_BACKEND_ENV = "DEEPSEEK_SECRET_BACKEND";

    private final KeyringStore store;
    private final String service;

    public SecretsStore(KeyringStore store) {
        this(store, DEFAULT_SERVICE);
    }

    public SecretsStore(KeyringStore store, String service) {
        this.store = store;
        this.service = service;
    }

    /** Auto-detect the best backend based on environment variables. */
    public static SecretsStore autoDetect() {
        String selection = configuredSecretBackend();
        if ("system".equals(selection) || "keyring".equals(selection)
            || "os".equals(selection) || "os-keyring".equals(selection)) {
            // OS keyring not available on JVM — fall back to file
            System.err.println("[secrets] OS keyring requested but not supported on JVM; falling back to file");
        }
        return fileBacked();
    }

    /** Create a file-backed store at the default path. */
    public static SecretsStore fileBacked() {
        Path defaultPath = FileKeyringStore.defaultPath();
        try {
            FileKeyringStore.migrateLegacyIfNeeded(defaultPath, FileKeyringStore.legacyPath());
        } catch (SecretsError ignored) {
            // migration failure is non-fatal
        }
        return new SecretsStore(new FileKeyringStore(defaultPath));
    }

    /** Create an in-memory store (for testing). */
    public static SecretsStore inMemory() {
        return new SecretsStore(new InMemoryKeyringStore());
    }

    public String backendName() {
        return store.backendName();
    }

    // ── Core operations ─────────────────────────────────────────────

    /** Write a secret directly to the store. */
    public void set(String name, String value) throws SecretsError {
        store.set(name, value);
    }

    /** Delete a secret from the store. */
    public void delete(String name) throws SecretsError {
        store.delete(name);
    }

    /** Read from the store directly (no env fallback). */
    public Optional<String> get(String name) throws SecretsError {
        return Optional.ofNullable(store.get(name));
    }

    // ── Layered resolution ──────────────────────────────────────────

    /**
     * Resolve a secret with precedence: store → env → none.
     *
     * @param name canonical provider name (e.g. "deepseek", "openai")
     */
    public Optional<String> resolve(String name) {
        try {
            String stored = store.get(name);
            if (stored != null && !stored.isBlank()) return Optional.of(stored);
        } catch (SecretsError ignored) {
            // store errors fall through to env
        }
        return Optional.ofNullable(ProviderEnv.envFor(name));
    }

    /**
     * Resolve with source tracking.
     *
     * @return {@code (value, SecretSource)} or empty if not found
     */
    public Optional<ResolvedSecret> resolveWithSource(String name) {
        try {
            String stored = store.get(name);
            if (stored != null && !stored.isBlank()) {
                return Optional.of(new ResolvedSecret(stored, SecretSource.KEYRING));
            }
        } catch (SecretsError ignored) { }
        String env = ProviderEnv.envFor(name);
        if (env != null && !env.isBlank()) {
            return Optional.of(new ResolvedSecret(env, SecretSource.ENV));
        }
        return Optional.empty();
    }

    /**
     * Fleet-worker resolution path. Respects a source hint to constrain
     * the lookup to a specific layer.
     *
     * @param key        provider name
     * @param sourceHint "env" → only environment; "keyring"/"file" → only store;
     *                   null → store first, then env
     */
    public Optional<String> resolveDirect(String key, String sourceHint) {
        if ("env".equals(sourceHint)) {
            return Optional.ofNullable(ProviderEnv.envFor(key));
        }
        if ("keyring".equals(sourceHint) || "file".equals(sourceHint)) {
            try {
                return Optional.ofNullable(store.get(key));
            } catch (SecretsError ignored) { }
            return Optional.empty();
        }
        return resolve(key);
    }

    // ── Internal ────────────────────────────────────────────────────

    private static String configuredSecretBackend() {
        for (String key : new String[]{SECRET_BACKEND_ENV, LEGACY_SECRET_BACKEND_ENV}) {
            String val = System.getenv(key);
            if (val != null && !val.isBlank()) return val.trim().toLowerCase();
        }
        return "file";
    }

    /** A resolved secret value paired with its source. */
    public record ResolvedSecret(String value, SecretSource source) { }
}
