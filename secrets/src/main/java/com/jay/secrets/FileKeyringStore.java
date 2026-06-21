package com.jay.secrets;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;

/**
 * JSON-on-disk secret store. Equivalent to Rust's FileKeyringStore.
 *
 * <p>Stores secrets as a plain JSON file at {@code ~/.codewhale/secrets/secrets.json}.
 * Protection relies on Unix file permissions (0600 file, 0700 directory).
 * On first access, non-conflicting entries from the legacy
 * {@code ~/.deepseek/secrets/secrets.json} are migrated.
 */
public class FileKeyringStore implements KeyringStore {

    private static final ObjectMapper mapper = new ObjectMapper();
    static final String BACKEND_NAME = "file-based (~/.codewhale/secrets/)";

    private final Path path;

    public FileKeyringStore(Path path) {
        this.path = path.toAbsolutePath();
    }

    /** Resolve the default path: {@code $CODEWHALE_HOME/secrets/secrets.json}
     *  or {@code $HOME/.codewhale/secrets/secrets.json}. */
    public static Path defaultPath() {
        String codewhaleHome = System.getenv("CODEWHALE_HOME");
        if (codewhaleHome != null && !codewhaleHome.isBlank()) {
            return Path.of(codewhaleHome, "secrets", "secrets.json");
        }
        String home = System.getenv("HOME");
        if (home == null || home.isBlank()) {
            home = System.getProperty("user.home");
        }
        return Path.of(home, ".codewhale", "secrets", "secrets.json");
    }

    /** Legacy path for migration. */
    public static Path legacyPath() {
        String home = System.getenv("HOME");
        if (home == null || home.isBlank()) {
            home = System.getProperty("user.home");
        }
        return Path.of(home, ".deepseek", "secrets", "secrets.json");
    }

    public Path path() { return path; }

    /** Migrate legacy entries if the primary file doesn't exist and the legacy does. */
    static void migrateLegacyIfNeeded(Path primary, Path legacy) throws SecretsError {
        if (Files.exists(primary) || !Files.exists(legacy)) return;

        try {
            FileSecretsBlob legacyBlob = loadFile(legacy);
            Map<String, String> existing = loadFile(primary).entries;

            boolean migrated = false;
            for (var entry : legacyBlob.entries.entrySet()) {
                if (!existing.containsKey(entry.getKey())) {
                    existing.put(entry.getKey(), entry.getValue());
                    migrated = true;
                }
            }
            if (migrated) {
                storeFile(primary, new FileSecretsBlob(existing));
            }
        } catch (SecretsError e) {
            // Migration failure is non-fatal
        }
    }

    @Override
    public String get(String key) throws SecretsError {
        FileSecretsBlob blob = loadUnlocked();
        return blob.entries.get(key);
    }

    @Override
    public void set(String key, String value) throws SecretsError {
        FileSecretsBlob blob = loadUnlocked();
        blob.entries.put(key, value);
        storeUnlocked(blob);
    }

    @Override
    public void delete(String key) throws SecretsError {
        FileSecretsBlob blob = loadUnlocked();
        blob.entries.remove(key);
        storeUnlocked(blob);
    }

    @Override
    public String backendName() {
        return BACKEND_NAME;
    }

    // ── Internal ────────────────────────────────────────────────────

    private FileSecretsBlob loadUnlocked() throws SecretsError {
        try {
            if (!Files.exists(path)) {
                return new FileSecretsBlob(new LinkedHashMap<>());
            }
            // On Unix, reject files with group/world permissions
            if (!isWindows()) {
                int mode = getUnixMode(path);
                if ((mode & 077) != 0) {
                    throw SecretsError.insecurePermissions(path, mode);
                }
            }
            return loadFile(path);
        } catch (IOException e) {
            throw SecretsError.io(e);
        }
    }

    private void storeUnlocked(FileSecretsBlob blob) throws SecretsError {
        try {
            Files.createDirectories(path.getParent());
            if (!isWindows()) {
                Files.setPosixFilePermissions(path.getParent(),
                    PosixFilePermissions.fromString("rwx------"));
            }
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            storeFile(tmp, blob);
            if (!isWindows()) {
                try {
                    Files.setPosixFilePermissions(tmp,
                        PosixFilePermissions.fromString("rw-------"));
                } catch (IOException ignored) {
                    // best-effort: ignore permission failure
                }
            }
            Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw SecretsError.io(e);
        }
    }

    // ── Static helpers ──────────────────────────────────────────────

    static FileSecretsBlob loadFile(Path path) throws SecretsError {
        try {
            if (!Files.exists(path) || Files.size(path) == 0) {
                return new FileSecretsBlob(new LinkedHashMap<>());
            }
            String content = Files.readString(path);
            return mapper.readValue(content, FileSecretsBlob.class);
        } catch (IOException e) {
            throw SecretsError.io(e);
        } catch (Exception e) {
            throw SecretsError.json(e);
        }
    }

    static void storeFile(Path path, FileSecretsBlob blob) throws SecretsError {
        try {
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(blob);
            Files.writeString(path, json);
            if (!isWindows()) {
                try {
                    Files.setPosixFilePermissions(path,
                        PosixFilePermissions.fromString("rw-------"));
                } catch (IOException ignored) {
                    // best-effort
                }
            }
        } catch (IOException e) {
            throw SecretsError.io(e);
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    static int getUnixMode(Path path) throws IOException {
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path);
        int mode = 0;
        if (perms.contains(PosixFilePermission.OWNER_READ)) mode |= 0400;
        if (perms.contains(PosixFilePermission.OWNER_WRITE)) mode |= 0200;
        if (perms.contains(PosixFilePermission.OWNER_EXECUTE)) mode |= 0100;
        if (perms.contains(PosixFilePermission.GROUP_READ)) mode |= 040;
        if (perms.contains(PosixFilePermission.GROUP_WRITE)) mode |= 020;
        if (perms.contains(PosixFilePermission.GROUP_EXECUTE)) mode |= 010;
        if (perms.contains(PosixFilePermission.OTHERS_READ)) mode |= 04;
        if (perms.contains(PosixFilePermission.OTHERS_WRITE)) mode |= 02;
        if (perms.contains(PosixFilePermission.OTHERS_EXECUTE)) mode |= 01;
        return mode;
    }

    // ── Inner types ─────────────────────────────────────────────────

    static class FileSecretsBlob {
        @JsonProperty("entries")
        Map<String, String> entries;

        FileSecretsBlob() { this.entries = new LinkedHashMap<>(); }
        FileSecretsBlob(Map<String, String> entries) { this.entries = entries; }
    }
}
