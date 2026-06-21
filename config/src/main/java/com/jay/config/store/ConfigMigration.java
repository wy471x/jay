package com.jay.config.store;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * One-time config migration v0.8.44: ~/.deepseek → ~/.codewhale.
 * Equivalent to Rust's migrate_config_if_needed().
 */
public final class ConfigMigration {

    private static final String PRIMARY_DIR = ".codewhale";
    private static final String LEGACY_DIR = ".deepseek";

    private ConfigMigration() {}

    public record MigrationInfo(Path legacyPath, Path primaryPath) {
        public String userNotice() {
            return "Migrated config from " + legacyPath + " to " + primaryPath;
        }
    }

    /** Attempt one-time migration. Returns info if migration occurred. */
    public static Optional<MigrationInfo> migrateIfNeeded() {
        Path home = Path.of(System.getProperty("user.home"));
        String appHome = System.getenv("CODEWHALE_HOME");
        Path primary = appHome != null && !appHome.isBlank()
            ? Path.of(appHome, "config.toml")
            : home.resolve(PRIMARY_DIR).resolve("config.toml");
        Path legacy = home.resolve(LEGACY_DIR).resolve("config.toml");

        if (Files.exists(primary) || !Files.exists(legacy)) {
            return Optional.empty();
        }

        try {
            Files.createDirectories(primary.getParent());
            Files.copy(legacy, primary);
            // Also migrate permissions if present
            Path legacyPerms = legacy.resolveSibling("permissions.toml");
            if (Files.exists(legacyPerms)) {
                Files.copy(legacyPerms, primary.resolveSibling("permissions.toml"));
            }
            return Optional.of(new MigrationInfo(legacy, primary));
        } catch (IOException e) {
            System.err.println("Config migration failed: " + e.getMessage());
            return Optional.empty();
        }
    }

    /** Resolve config path: primary first, legacy fallback, primary default. */
    public static Path resolveConfigPath() {
        Path primary = Path.of(
            System.getenv().getOrDefault("CODEWHALE_HOME",
                Path.of(System.getProperty("user.home"), PRIMARY_DIR).toString()),
            "config.toml");
        Path legacy = Path.of(System.getProperty("user.home"), LEGACY_DIR, "config.toml");

        if (Files.exists(primary)) return primary;
        if (Files.exists(legacy)) return legacy;
        return primary;
    }
}
