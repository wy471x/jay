package com.jay.secrets;

import java.io.IOException;
import java.nio.file.Path;

public class SecretsError extends Exception {

    public enum Kind {
        KEYRING,
        IO,
        JSON,
        INSECURE_PERMISSIONS
    }

    private final Kind kind;
    private final Path path;
    private final int mode;

    private SecretsError(Kind kind, String message, Throwable cause, Path path, int mode) {
        super(message, cause);
        this.kind = kind;
        this.path = path;
        this.mode = mode;
    }

    public static SecretsError keyring(String message) {
        return new SecretsError(Kind.KEYRING, "keyring backend error: " + message, null, null, 0);
    }

    public static SecretsError io(IOException cause) {
        return new SecretsError(Kind.IO, "file-backed secret store I/O error: " + cause.getMessage(),
            cause, null, 0);
    }

    public static SecretsError json(Exception cause) {
        return new SecretsError(Kind.JSON, "file-backed secret store JSON error: " + cause.getMessage(),
            cause, null, 0);
    }

    public static SecretsError insecurePermissions(Path path, int mode) {
        return new SecretsError(Kind.INSECURE_PERMISSIONS,
            String.format("file-backed secret store at %s has insecure permissions %o (expected 0600)", path, mode),
            null, path, mode);
    }

    public Kind kind() { return kind; }

    public Path path() { return path; }

    public int mode() { return mode; }
}
