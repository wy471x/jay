package com.jay.config;

import com.jay.config.model.CliRuntimeOverrides;
import com.jay.config.model.RuntimeApiKeySource;
import com.jay.config.provider.ProviderDefaults;
import com.jay.config.provider.ProviderSource;
import com.jay.config.store.ConfigStore;
import com.jay.config.util.ConfigPathResolver;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    // ── ConfigPathResolver ─────────────────────────────────────────

    @Nested
    class ConfigPathResolverTests {

        @Test
        void constantsAreCorrect() {
            assertEquals("config.toml", ConfigPathResolver.CONFIG_FILE_NAME);
            assertEquals("permissions.toml", ConfigPathResolver.PERMISSIONS_FILE_NAME);
            assertEquals(".codewhale", ConfigPathResolver.CODEWHALE_APP_DIR);
            assertEquals(".deepseek", ConfigPathResolver.LEGACY_APP_DIR);
        }

        @Test
        void appDirContainsCorrectPath() {
            Path dir = ConfigPathResolver.appDir();
            assertTrue(dir.toString().contains(".codewhale"));
        }

        @Test
        void resolveConfigPathReturnsDefaultWhenNoEnv() {
            Path resolved = ConfigPathResolver.resolveConfigPath(null);
            assertNotNull(resolved);
            assertTrue(resolved.getFileName().toString().contains("config.toml"));
        }

        @Test
        void resolvePermissionsPathIsAdjacent() {
            Path configPath = Path.of("/tmp/test/config.toml");
            Path perms = ConfigPathResolver.resolvePermissionsPath(configPath);
            assertTrue(perms.toString().endsWith("permissions.toml"));
        }

        @Test
        void migrateIfNeededReturnsFalseWhenNoLegacy() {
            boolean migrated = ConfigPathResolver.migrateIfNeeded();
            assertFalse(migrated);
        }
    }

    // ── ConfigStore key-value ─────────────────────────────────────

    @Nested
    class ConfigStoreKeyValueTests {

        @TempDir
        Path tempDir;

        private ConfigStore store;

        @BeforeEach
        void setUp() throws Exception {
            Path path = tempDir.resolve("config.toml");
            store = new ConfigStore(path);
        }

        @Test
        void getValueReturnsNullForMissing() {
            assertNull(store.getValue("nonexistent"));
            assertNull(store.getValue("providers.deepseek.api_key"));
        }

        @Test
        void setValueAndGetValueRoundTrip() {
            store.setValue("provider", "deepseek");
            assertEquals("deepseek", store.getValue("provider"));
        }

        @Test
        void setNestedValueAndGet() {
            store.setValue("providers.deepseek.api_key", "sk-test-123");
            assertEquals("sk-test-123", store.getValue("providers.deepseek.api_key"));
        }

        @Test
        void setMultipleNestedValues() {
            store.setValue("providers.deepseek.api_key", "sk-deepseek");
            store.setValue("providers.openai.api_key", "sk-openai");
            store.setValue("providers.deepseek.base_url", "https://api.deepseek.com");

            assertEquals("sk-deepseek", store.getValue("providers.deepseek.api_key"));
            assertEquals("sk-openai", store.getValue("providers.openai.api_key"));
            assertEquals("https://api.deepseek.com", store.getValue("providers.deepseek.base_url"));
        }

        @Test
        void unsetValueRemovesEntry() {
            store.setValue("providers.deepseek.api_key", "sk-test");
            store.unsetValue("providers.deepseek.api_key");
            assertNull(store.getValue("providers.deepseek.api_key"));
        }

        @Test
        void unsetValueNonExistentIsNoOp() {
            assertDoesNotThrow(() -> store.unsetValue("nonexistent.key"));
        }

        @Test
        void overwriteExistingValue() {
            store.setValue("model", "old-model");
            store.setValue("model", "new-model");
            assertEquals("new-model", store.getValue("model"));
        }
    }

    // ── ConfigStore display / redaction ────────────────────────────

    @Nested
    class ConfigStoreDisplayTests {

        @TempDir
        Path tempDir;

        private ConfigStore store;

        @BeforeEach
        void setUp() {
            store = new ConfigStore(tempDir.resolve("config.toml"));
        }

        @Test
        void getDisplayValueRedactsApiKey() {
            store.setValue("providers.deepseek.api_key", "sk-this-is-a-long-api-key-12345678");
            String display = store.getDisplayValue("providers.deepseek.api_key");
            assertNotNull(display);
            assertTrue(display.startsWith("sk-t"));
            assertTrue(display.endsWith("5678"));
            assertTrue(display.contains("********"));
            assertFalse(display.contains("this-is-a-long"));
        }

        @Test
        void getDisplayValueRedactsShortApiKey() {
            store.setValue("api_key", "short-key");
            assertEquals("********", store.getDisplayValue("api_key"));
        }

        @Test
        void getDisplayValueDoesNotRedactNonSecretKeys() {
            store.setValue("model", "claude-sonnet-4-6");
            store.setValue("provider", "deepseek");
            assertEquals("claude-sonnet-4-6", store.getDisplayValue("model"));
            assertEquals("deepseek", store.getDisplayValue("provider"));
        }

        @Test
        void listValuesRedactsSecrets() {
            store.setValue("model", "deepseek-v4");
            store.setValue("providers.deepseek.api_key", "sk-secret-key-1234567890abcdef");
            store.setValue("output_mode", "text");

            Map<String, String> values = store.listValues();

            assertEquals("deepseek-v4", values.get("model"));
            assertEquals("text", values.get("output_mode"));
            assertTrue(values.get("providers.deepseek.api_key").contains("********"));
        }

        @Test
        void listValuesIsSorted() {
            store.setValue("b", "2");
            store.setValue("a", "1");
            store.setValue("c", "3");
            Map<String, String> values = store.listValues();
            var keys = values.keySet().iterator();
            assertEquals("a", keys.next());
            assertEquals("b", keys.next());
            assertEquals("c", keys.next());
        }
    }

    // ── ConfigStore file I/O ──────────────────────────────────────

    @Nested
    class ConfigStoreFileIOTests {

        @TempDir
        Path tempDir;

        @Test
        void saveAndLoadRoundTrip() throws Exception {
            Path path = tempDir.resolve("config.toml");
            var store = new ConfigStore(path);
            store.setValue("provider", "deepseek");
            store.setValue("model", "deepseek-v4-pro");
            store.setValue("providers.deepseek.api_key", "sk-secret");

            store.save();
            assertTrue(Files.exists(path));

            var loaded = ConfigStore.load(path);
            assertEquals("deepseek", loaded.getValue("provider"));
            assertEquals("deepseek-v4-pro", loaded.getValue("model"));
            assertEquals("sk-secret", loaded.getValue("providers.deepseek.api_key"));
        }

        @Test
        void saveCreatesBackupOnFirstChange() throws Exception {
            Path path = tempDir.resolve("config.toml");

            var store1 = new ConfigStore(path);
            store1.setValue("provider", "deepseek");
            store1.save();

            var store2 = ConfigStore.load(path);
            store2.setValue("model", "claude-sonnet");
            store2.save();

            Path bak = path.resolveSibling("config.toml.bak");
            assertTrue(Files.exists(bak));

            var bakStore = ConfigStore.load(bak);
            assertEquals("deepseek", bakStore.getValue("provider"));
            assertNull(bakStore.getValue("model"));
        }

        @Test
        void saveIsNoOpWhenUnchanged() throws Exception {
            Path path = tempDir.resolve("config.toml");

            var store1 = new ConfigStore(path);
            store1.setValue("provider", "deepseek");
            store1.save();

            var store2 = ConfigStore.load(path);
            // Don't modify anything
            store2.save();  // Should be no-op
        }

        @Test
        void loadHandlesMissingFile() throws Exception {
            Path path = tempDir.resolve("nonexistent.toml");
            var store = ConfigStore.load(path);
            assertNotNull(store);
            assertNull(store.getValue("anything"));
        }

        @Test
        void loadHandlesEmptyFile() throws Exception {
            Path path = tempDir.resolve("empty.toml");
            Files.writeString(path, "");
            var store = ConfigStore.load(path);
            assertNotNull(store);
            assertNull(store.getValue("anything"));
        }
    }

    // ── Key resolution helpers ─────────────────────────────────────

    @Nested
    class KeyResolutionTests {

        @Test
        void resolveSimpleKey() {
            var map = new java.util.LinkedHashMap<String, Object>();
            map.put("key", "value");
            assertEquals("value", ConfigStore.resolveKey(map, "key"));
        }

        @Test
        void resolveNestedKey() {
            var inner = new java.util.LinkedHashMap<String, Object>();
            inner.put("api_key", "sk-secret");
            var outer = new java.util.LinkedHashMap<String, Object>();
            outer.put("deepseek", inner);
            assertEquals("sk-secret", ConfigStore.resolveKey(outer, "deepseek.api_key"));
        }

        @Test
        void resolveMissingIntermediate() {
            var map = new java.util.LinkedHashMap<String, Object>();
            assertNull(ConfigStore.resolveKey(map, "a.b.c"));
        }

        @Test
        void setNestedKeyCreatesIntermediateMaps() {
            var map = new java.util.LinkedHashMap<String, Object>();
            ConfigStore.setNestedKey(map, "a.b.c", "value");
            assertEquals("value", ConfigStore.resolveKey(map, "a.b.c"));
        }

        @Test
        void unsetRemovesDeeply() {
            var map = new java.util.LinkedHashMap<String, Object>();
            ConfigStore.setNestedKey(map, "a.b.c", "value");
            ConfigStore.unsetNestedKey(map, "a.b.c");
            assertNull(ConfigStore.resolveKey(map, "a.b.c"));
        }
    }

    // ── ProviderDefaults ──────────────────────────────────────────

    @Nested
    class ProviderDefaultsTests {

        @Test
        void all25ProvidersHaveDefaults() {
            for (var kind : com.jay.agent.ProviderKind.values()) {
                assertNotNull(ProviderDefaults.defaultModel(kind),
                    "missing default model for " + kind);
                assertNotNull(ProviderDefaults.defaultBaseUrl(kind),
                    "missing default base URL for " + kind);
            }
        }

        @Test
        void localProvidersSkipSecretStore() {
            assertTrue(ProviderDefaults.shouldSkipSecretStore(
                com.jay.agent.ProviderKind.SGLANG));
            assertTrue(ProviderDefaults.shouldSkipSecretStore(
                com.jay.agent.ProviderKind.VLLM));
            assertTrue(ProviderDefaults.shouldSkipSecretStore(
                com.jay.agent.ProviderKind.OLLAMA));
            assertFalse(ProviderDefaults.shouldSkipSecretStore(
                com.jay.agent.ProviderKind.DEEPSEEK));
        }
    }

    // ── RuntimeApiKeySource ────────────────────────────────────────

    @Nested
    class RuntimeApiKeySourceTests {

        @Test
        void asEnvValueReturnsCorrectStrings() {
            assertEquals("cli", RuntimeApiKeySource.CLI.asEnvValue());
            assertEquals("config", RuntimeApiKeySource.CONFIG_FILE.asEnvValue());
            assertEquals("keyring", RuntimeApiKeySource.KEYRING.asEnvValue());
            assertEquals("env", RuntimeApiKeySource.ENV.asEnvValue());
        }
    }

    // ── CliRuntimeOverrides ─────────────────────────────────────────

    @Nested
    class CliRuntimeOverridesTests {

        @Test
        void builderPatternWorks() {
            var overrides = new CliRuntimeOverrides()
                .provider(com.jay.agent.ProviderKind.DEEPSEEK)
                .model("deepseek-v4-pro");
            assertEquals(com.jay.agent.ProviderKind.DEEPSEEK, overrides.provider());
            assertEquals("deepseek-v4-pro", overrides.model());
        }

        @Test
        void defaultsAreNull() {
            var overrides = new CliRuntimeOverrides();
            assertNull(overrides.provider());
            assertNull(overrides.model());
            assertNull(overrides.apiKey());
            assertNull(overrides.telemetry());
            assertNull(overrides.yolo());
        }
    }

    // ── ConfigStore.resolveRuntimeOptions ──────────────────────────

    @Nested
    class ResolveRuntimeOptionsTests {

        @TempDir
        Path tempDir;

        @Test
        void resolvesWithCliOverrides() {
            var store = new ConfigStore(tempDir.resolve("config.toml"));
            var overrides = new CliRuntimeOverrides()
                .provider(com.jay.agent.ProviderKind.ANTHROPIC)
                .model("claude-sonnet-4-6")
                .apiKey("sk-cli-key");

            var opts = store.resolveRuntimeOptions(overrides, null);
            assertEquals(com.jay.agent.ProviderKind.ANTHROPIC, opts.provider());
            assertEquals("claude-sonnet-4-6", opts.model());
            assertEquals("sk-cli-key", opts.apiKey());
            assertEquals(RuntimeApiKeySource.CLI, opts.apiKeySource());
        }

        @Test
        void resolvesWithConfigFallback() {
            var store = new ConfigStore(tempDir.resolve("config.toml"));
            store.setValue("provider", "deepseek");
            store.setValue("model", "deepseek-v4-pro");

            var opts = store.resolveRuntimeOptions(null, null);
            assertEquals(com.jay.agent.ProviderKind.DEEPSEEK, opts.provider());
            assertEquals("deepseek-v4-pro", opts.model());
        }

        @Test
        void cliModelOverridesEnvVars() {
            var store = new ConfigStore(tempDir.resolve("config.toml"));
            var overrides = new CliRuntimeOverrides()
                .provider(com.jay.agent.ProviderKind.ANTHROPIC)
                .model("claude-opus-4-8");

            var opts = store.resolveRuntimeOptions(overrides, null);
            assertEquals("claude-opus-4-8", opts.model());
            assertNotNull(opts.baseUrl());
        }

        @Test
        void configFileModelOverridesDefault() {
            var store = new ConfigStore(tempDir.resolve("config.toml"));
            store.setValue("model", "custom-model-from-config");
            var overrides = new CliRuntimeOverrides()
                .provider(com.jay.agent.ProviderKind.DEEPSEEK);

            var opts = store.resolveRuntimeOptions(overrides, null);
            assertNotNull(opts.model());
        }

        @Test
        void providerSourceTracksCli() {
            var store = new ConfigStore(tempDir.resolve("config.toml"));
            var overrides = new CliRuntimeOverrides()
                .provider(com.jay.agent.ProviderKind.OPENAI);

            var opts = store.resolveRuntimeOptions(overrides, null);
            assertTrue(opts.providerSource() instanceof ProviderSource.Cli);
        }

        @Test
        void usesSecretResolverForApiKey() {
            var store = new ConfigStore(tempDir.resolve("config.toml"));
            var overrides = new CliRuntimeOverrides()
                .provider(com.jay.agent.ProviderKind.DEEPSEEK);

            var opts = store.resolveRuntimeOptions(overrides,
                k -> java.util.Optional.of("sk-keyring-key"));
            assertEquals("sk-keyring-key", opts.apiKey());
            assertEquals(RuntimeApiKeySource.KEYRING, opts.apiKeySource());
        }

        @Test
        void parseProviderKindReturnsNullForUnknown() {
            assertNull(com.jay.agent.ProviderKind.parse("unknown-provider"));
            assertNotNull(com.jay.agent.ProviderKind.parse("deepseek"));
        }
    }

    // ── Redaction ──────────────────────────────────────────────────

    @Nested
    class RedactionTests {

        @Test
        void redactLongKey() {
            String result = ConfigStore.redact("sk-abcdefghijklmnopqrstuvwxyz1234");
            assertTrue(result.startsWith("sk-a"));
            assertTrue(result.endsWith("1234"));
            assertTrue(result.contains("********"));
            assertEquals(16, result.length());
        }

        @Test
        void redactShortKey() {
            assertEquals("********", ConfigStore.redact("short"));
            assertEquals("********", ConfigStore.redact("1234567890123456"));
        }

        @Test
        void redactNull() {
            assertEquals("********", ConfigStore.redact(null));
        }
    }
}
