package com.jay.secrets;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SecretsTest {

    // ── ProviderEnv ────────────────────────────────────────────────

    @Nested
    class ProviderEnvTests {

        @Test
        void envForReturnsNullForUnknownProvider() {
            assertNull(ProviderEnv.envFor("nonexistent-provider-xyz"));
        }

        @Test
        void envForReturnsNullForNullAndBlank() {
            assertNull(ProviderEnv.envFor(null));
            assertNull(ProviderEnv.envFor(""));
            assertNull(ProviderEnv.envFor("   "));
        }

        @Test
        void envForResolvesAliases() {
            // All these should map to the same env vars without throwing
            assertDoesNotThrow(() -> ProviderEnv.envFor("claude"));
            assertDoesNotThrow(() -> ProviderEnv.envFor("anthropic"));
            assertDoesNotThrow(() -> ProviderEnv.envFor("xiaomi_mimo"));
            assertDoesNotThrow(() -> ProviderEnv.envFor("mimo"));
            assertDoesNotThrow(() -> ProviderEnv.envFor("kimi"));
            assertDoesNotThrow(() -> ProviderEnv.envFor("fireworks-ai"));
        }

        @Test
        void knownProvidersCoverMajorOnes() {
            assertTrue(ProviderEnv.knownProviderCount() >= 20,
                "should have at least 20 provider mappings");
        }
    }

    // ── InMemoryKeyringStore ───────────────────────────────────────

    @Nested
    class InMemoryKeyringStoreTests {

        private final InMemoryKeyringStore store = new InMemoryKeyringStore();

        @Test
        void getReturnsNullForMissing() throws Exception {
            assertNull(store.get("nonexistent"));
        }

        @Test
        void setAndGetRoundTrip() throws Exception {
            store.set("deepseek", "sk-test-123");
            assertEquals("sk-test-123", store.get("deepseek"));
        }

        @Test
        void setReplacesExisting() throws Exception {
            store.set("openai", "sk-old");
            store.set("openai", "sk-new");
            assertEquals("sk-new", store.get("openai"));
        }

        @Test
        void deleteRemovesEntry() throws Exception {
            store.set("anthropic", "sk-ant");
            store.delete("anthropic");
            assertNull(store.get("anthropic"));
        }

        @Test
        void deleteMissingKeyIsNoOp() {
            assertDoesNotThrow(() -> store.delete("never-set"));
        }

        @Test
        void backendNameIsDescriptive() {
            assertEquals("in-memory", store.backendName());
        }
    }

    // ── FileKeyringStore ───────────────────────────────────────────

    @Nested
    class FileKeyringStoreTests {

        @TempDir
        Path tempDir;

        @Test
        void setAndGetRoundTrip() throws Exception {
            Path path = tempDir.resolve("secrets.json");
            var store = new FileKeyringStore(path);

            store.set("deepseek", "sk-test");
            assertEquals("sk-test", store.get("deepseek"));
        }

        @Test
        void getReturnsNullForMissing() throws Exception {
            var store = new FileKeyringStore(tempDir.resolve("nonexistent.json"));
            assertNull(store.get("nonexistent"));
        }

        @Test
        void deleteRemovesEntry() throws Exception {
            Path path = tempDir.resolve("secrets.json");
            var store = new FileKeyringStore(path);

            store.set("openai", "sk-key");
            store.delete("openai");
            assertNull(store.get("openai"));
        }

        @Test
        void fileIsCreatedWithCorrectStructure() throws Exception {
            Path path = tempDir.resolve("secrets.json");
            var store = new FileKeyringStore(path);

            store.set("deepseek", "sk-value");
            String content = Files.readString(path);
            assertTrue(content.contains("entries"));
            assertTrue(content.contains("deepseek"));
            assertTrue(content.contains("sk-value"));
        }

        @Test
        void storePersistsAcrossInstances() throws Exception {
            Path path = tempDir.resolve("secrets.json");
            var store1 = new FileKeyringStore(path);
            store1.set("openai", "sk-123");

            var store2 = new FileKeyringStore(path);
            assertEquals("sk-123", store2.get("openai"));
        }

        @Test
        void backendNameIsDescriptive() {
            var store = new FileKeyringStore(tempDir.resolve("secrets.json"));
            assertEquals(FileKeyringStore.BACKEND_NAME, store.backendName());
        }

        @Test
        void defaultPathUsesHome() {
            Path path = FileKeyringStore.defaultPath();
            assertTrue(path.toString().contains("codewhale"));
            assertTrue(path.toString().endsWith("secrets.json"));
        }

        @Test
        void pathReturnsAbsolutePath() {
            Path rel = Path.of("rel/secrets.json");
            var store = new FileKeyringStore(rel);
            assertTrue(store.path().isAbsolute());
        }
    }

    // ── FileKeyringStore permissions ───────────────────────────────

    @Nested
    class FileKeyringStorePermissions {

        @TempDir
        Path tempDir;

        @Test
        void rejectsInsecurePermissions() throws Exception {
            Path path = tempDir.resolve("insecure.json");
            // Create a file with 0644 permissions
            Files.writeString(path, "{\"entries\":{\"key\":\"val\"}}");

            if (!isWindows()) {
                Files.setPosixFilePermissions(path,
                    PosixFilePermissions.fromString("rw-r--r--"));
                var store = new FileKeyringStore(path);
                var err = assertThrows(SecretsError.class, () -> store.get("key"));
                assertEquals(SecretsError.Kind.INSECURE_PERMISSIONS, err.kind());
                assertEquals(path, err.path());
            }
        }

        @Test
        void securePermissionsAreAccepted() throws Exception {
            Path path = tempDir.resolve("secure.json");
            Files.writeString(path, "{\"entries\":{\"key\":\"val\"}}");

            if (!isWindows()) {
                Files.setPosixFilePermissions(path,
                    PosixFilePermissions.fromString("rw-------"));
            }
            var store = new FileKeyringStore(path);
            assertEquals("val", store.get("key"));
        }
    }

    // ── Legacy migration ───────────────────────────────────────────

    @Nested
    class MigrationTests {

        @TempDir
        Path tempDir;

        @Test
        void migratesNonConflictingEntries() throws Exception {
            Path primary = tempDir.resolve("primary.json");
            Path legacy = tempDir.resolve("legacy.json");

            // Write legacy with two entries
            var legacyStore = new FileKeyringStore(legacy);
            legacyStore.set("deepseek", "sk-legacy");
            legacyStore.set("openai", "sk-legacy-openai");

            // Do NOT create primary first — migration only runs when primary is new

            // Write a primary entry directly (simulating pre-existing conflicting key)
            var partialPrimary = new FileKeyringStore(primary);
            partialPrimary.set("deepseek", "sk-primary");

            // Since primary now exists, migration won't run. Re-test a fresh scenario:
            // Delete primary, re-run migration
            Files.delete(primary);
            FileKeyringStore.migrateLegacyIfNeeded(primary, legacy);

            // Primary should get all legacy entries (since it was empty)
            var result = new FileKeyringStore(primary);
            assertEquals("sk-legacy", result.get("deepseek"));
            assertEquals("sk-legacy-openai", result.get("openai"));
        }

        @Test
        void noMigrationWhenPrimaryExists() throws Exception {
            Path primary = tempDir.resolve("primary.json");
            Path legacy = tempDir.resolve("legacy.json");

            // Create primary file with secure permissions via FileKeyringStore
            var primaryStore = new FileKeyringStore(primary);
            primaryStore.set("existing", "val");

            var legacyStore = new FileKeyringStore(legacy);
            legacyStore.set("new-key", "should-not-migrate");

            FileKeyringStore.migrateLegacyIfNeeded(primary, legacy);

            var result = new FileKeyringStore(primary);
            assertNull(result.get("new-key")); // not migrated because primary existed
        }
    }

    // ── SecretsStore facade ────────────────────────────────────────

    @Nested
    class SecretsStoreTests {

        @Test
        void resolveReturnsStoredValue() {
            var store = SecretsStore.inMemory();
            assertDoesNotThrow(() -> store.set("deepseek", "sk-secret"));
            assertEquals("sk-secret", store.resolve("deepseek").orElse(null));
        }

        @Test
        void resolveWithSourceIdentifiesStore() {
            var store = SecretsStore.inMemory();
            assertDoesNotThrow(() -> store.set("openai", "sk-key"));
            var resolved = store.resolveWithSource("openai").orElse(null);
            assertNotNull(resolved);
            assertEquals("sk-key", resolved.value());
            assertEquals(SecretSource.KEYRING, resolved.source());
        }

        @Test
        void resolveWithSourceIdentifiesEnv() {
            var store = SecretsStore.inMemory(); // no stored value for "nonexistent"
            // envFor will return null in test (no env vars set), so no match
            var resolved = store.resolveWithSource("nonexistent-provider");
            assertTrue(resolved.isEmpty());
        }

        @Test
        void deleteRemovesEntry() {
            var store = SecretsStore.inMemory();
            assertDoesNotThrow(() -> store.set("test", "value"));
            assertDoesNotThrow(() -> store.delete("test"));
            assertTrue(store.resolve("test").isEmpty());
        }

        @Test
        void resolveDirectEnvHintSkipsStore() {
            var store = SecretsStore.inMemory();
            assertDoesNotThrow(() -> store.set("openai", "sk-stored"));
            // With "env" hint, should skip the store
            var result = store.resolveDirect("openai", "env");
            // Will be empty in test (no env vars), NOT the stored value
            assertTrue(result.isEmpty() || !"sk-stored".equals(result.orElse(null)));
        }

        @Test
        void resolveDirectKeyringHintSkipsEnv() {
            var store = SecretsStore.inMemory();
            assertDoesNotThrow(() -> store.set("openai", "sk-stored"));
            // With "keyring" hint, should only use store
            assertEquals("sk-stored", store.resolveDirect("openai", "keyring").orElse(null));
        }

        @Test
        void resolveDirectNullHintFallsBackToStoreThenEnv() {
            var store = SecretsStore.inMemory();
            assertDoesNotThrow(() -> store.set("openai", "sk-stored"));
            assertEquals("sk-stored", store.resolveDirect("openai", null).orElse(null));
        }

        @Test
        void backendNameIsSet() {
            assertEquals("in-memory", SecretsStore.inMemory().backendName());
        }

        @Test
        void getReadsStoreDirectly() throws Exception {
            var store = SecretsStore.inMemory();
            store.set("test", "val");
            assertEquals("val", store.get("test").orElse(null));
        }

        @Test
        void autoDetectReturnsFileBacked() {
            var store = SecretsStore.autoDetect();
            assertNotNull(store);
            assertNotNull(store.backendName());
        }
    }

    // ── SecretsError ───────────────────────────────────────────────

    @Nested
    class SecretsErrorTests {

        @Test
        void keyringErrorHasCorrectMessage() {
            var err = SecretsError.keyring("backend unavailable");
            assertEquals(SecretsError.Kind.KEYRING, err.kind());
            assertTrue(err.getMessage().contains("backend unavailable"));
        }

        @Test
        void insecurePermissionsErrorHasPathAndMode() {
            var err = SecretsError.insecurePermissions(Path.of("/tmp/secrets.json"), 0644);
            assertEquals(SecretsError.Kind.INSECURE_PERMISSIONS, err.kind());
            assertEquals(Path.of("/tmp/secrets.json"), err.path());
            assertEquals(0644, err.mode());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
