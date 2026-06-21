package com.jay.config.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Config file path resolution and migration helpers.
 * Equivalent to Rust's config path resolution chain.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>Explicit path from caller</li>
 *   <li>{@code CODEWHALE_CONFIG_PATH} env var</li>
 *   <li>{@code DEEPSEEK_CONFIG_PATH} env var (legacy)</li>
 *   <li>Default: {@code ~/.codewhale/config.toml}</li>
 * </ol>
 */
public final class ConfigPathResolver {

    public static final String CONFIG_FILE_NAME = "config.toml";
    public static final String PERMISSIONS_FILE_NAME = "permissions.toml";
    public static final String CODEWHALE_APP_DIR = ".codewhale";
    public static final String LEGACY_APP_DIR = ".deepseek";

    private static final String CODEWHALE_CONFIG_PATH_ENV = "CODEWHALE_CONFIG_PATH";
    private static final String LEGACY_CONFIG_PATH_ENV = "DEEPSEEK_CONFIG_PATH";

    private ConfigPathResolver() {}

    /** Resolve the primary config directory. */
    public static Path appDir() {
        String customHome = System.getenv("CODEWHALE_HOME");
        if (customHome != null && !customHome.isBlank()) {
            return Path.of(customHome);
        }
        Path home = homeDir();
        return home.resolve(CODEWHALE_APP_DIR);
    }

    /** Legacy config directory for migration. */
    public static Path legacyAppDir() {
        Path home = homeDir();
        return home.resolve(LEGACY_APP_DIR);
    }

    /** Resolve the config file path, respecting env var overrides. */
    public static Path resolveConfigPath(Path explicit) {
        if (explicit != null) return explicit;

        for (String key : new String[]{CODEWHALE_CONFIG_PATH_ENV, LEGACY_CONFIG_PATH_ENV}) {
            String val = System.getenv(key);
            if (val != null && !val.isBlank()) {
                return Path.of(val);
            }
        }
        Path primary = appDir().resolve(CONFIG_FILE_NAME);
        Path legacy = legacyAppDir().resolve(CONFIG_FILE_NAME);

        // Fall back to legacy if primary doesn't exist and legacy does
        if (!Files.exists(primary) && Files.exists(legacy)) {
            return legacy;
        }
        return primary;
    }

    /** Resolve the permissions file path alongside the config. */
    public static Path resolvePermissionsPath(Path configPath) {
        Path sibling = configPath.resolveSibling(PERMISSIONS_FILE_NAME);
        Path legacy = legacyAppDir().resolve(PERMISSIONS_FILE_NAME);

        if (!Files.exists(sibling) && Files.exists(legacy)) {
            return legacy;
        }
        return sibling;
    }

    /**
     * Migrate config from legacy (~/.deepseek/) to primary (~/.codewhale/)
     * if the primary doesn't exist and the legacy does. One-time copy.
     */
    public static boolean migrateIfNeeded() {
        Path primary = appDir().resolve(CONFIG_FILE_NAME);
        Path legacy = legacyAppDir().resolve(CONFIG_FILE_NAME);

        if (Files.exists(primary) || !Files.exists(legacy)) return false;

        try {
            Files.createDirectories(primary.getParent());
            Files.copy(legacy, primary);
            // Also migrate permissions if present
            Path legacyPerms = legacyAppDir().resolve(PERMISSIONS_FILE_NAME);
            if (Files.exists(legacyPerms)) {
                Path primaryPerms = primary.resolveSibling(PERMISSIONS_FILE_NAME);
                Files.copy(legacyPerms, primaryPerms);
            }
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────

    public static Path homeDir() {
        String home = System.getenv("HOME");
        if (home == null || home.isBlank()) {
            home = System.getProperty("user.home");
        }
        return Path.of(home);
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
