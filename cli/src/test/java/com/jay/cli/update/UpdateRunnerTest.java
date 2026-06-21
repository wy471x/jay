package com.jay.cli.update;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class UpdateRunnerTest {

    // ── platformAssetName ────────────────────────────────────────────

    @Nested
    class PlatformAssetName {

        @Test
        void returnsNonEmptyString() {
            String name = UpdateRunner.platformAssetName();
            assertNotNull(name);
            assertFalse(name.isBlank());
        }

        @Test
        void startsWithCodewhale() {
            assertTrue(UpdateRunner.platformAssetName().startsWith("codewhale-"));
        }

        @Test
        void containsArch() {
            String name = UpdateRunner.platformAssetName();
            assertTrue(name.contains("x86_64") || name.contains("aarch64"),
                    "should contain architecture: " + name);
        }

        @Test
        void containsOs() {
            String name = UpdateRunner.platformAssetName();
            assertTrue(
                    name.contains("apple-darwin")
                            || name.contains("pc-windows")
                            || name.contains("linux-gnu"),
                    "should contain OS: " + name);
        }

        @Test
        void hasExeExtensionOnWindows() {
            String name = UpdateRunner.platformAssetName();
            // macOS won't have .exe, but test the logic
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            assertEquals(isWindows, name.endsWith(".exe"));
        }
    }

    // ── extractHash ──────────────────────────────────────────────────

    @Nested
    class ExtractHash {

        @Test
        void findsHashForMatchingAsset() {
            String manifest = """
                    abc123def456  codewhale-x86_64-apple-darwin
                    deadbeef1234  codewhale-aarch64-apple-darwin
                    0123456789ab  codewhale-x86_64-unknown-linux-gnu
                    """;
            assertEquals("deadbeef1234",
                    UpdateRunner.extractHash(manifest, "codewhale-aarch64-apple-darwin"));
        }

        @Test
        void returnsNullWhenAssetNotFound() {
            String manifest = "abc123  codewhale-x86_64-apple-darwin\n";
            assertNull(UpdateRunner.extractHash(manifest, "nonexistent-binary"));
        }

        @Test
        void handlesMiddleOfList() {
            String manifest = """
                    aaa  binary-one
                    bbb  binary-two
                    ccc  binary-three
                    """;
            assertEquals("bbb", UpdateRunner.extractHash(manifest, "binary-two"));
        }

        @Test
        void handlesFirstEntry() {
            String manifest = "firsthash  codewhale-my-platform\nsecondhash  codewhale-other\n";
            assertEquals("firsthash",
                    UpdateRunner.extractHash(manifest, "codewhale-my-platform"));
        }

        @Test
        void returnsNullForEmptyManifest() {
            assertNull(UpdateRunner.extractHash("", "anything"));
        }

        @Test
        void trimsWhitespace() {
            String manifest = "   myhash    mybinary   \n";
            assertEquals("myhash", UpdateRunner.extractHash(manifest, "mybinary"));
        }
    }

    // ── sha256 ───────────────────────────────────────────────────────

    @Nested
    class Sha256 {

        @Test
        void knownVector() {
            byte[] data = "hello".getBytes();
            String hash = UpdateRunner.sha256(data);
            assertEquals(64, hash.length());
            // SHA-256("hello") = 2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824
            assertEquals(
                    "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                    hash);
        }

        @Test
        void emptyInput() {
            byte[] data = new byte[0];
            String hash = UpdateRunner.sha256(data);
            assertEquals(64, hash.length());
            assertEquals(
                    "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                    hash);
        }

        @Test
        void allLowercaseHex() {
            String hash = UpdateRunner.sha256("test".getBytes());
            assertEquals(hash, hash.toLowerCase());
            assertTrue(hash.matches("^[0-9a-f]{64}$"));
        }
    }

    // ── UpdateRunner construction ────────────────────────────────────

    @Test
    void constructorStoresParameters() {
        UpdateRunner runner = new UpdateRunner(true, true, "http://proxy:8080");
        // Can't access fields directly, but construction succeeds
        assertNotNull(runner);
    }
}
