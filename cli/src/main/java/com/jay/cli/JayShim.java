package com.jay.cli;

/**
 * Convenience {@code j} alias shim.
 * Forwards argv to the {@code jay} dispatcher.
 *
 * <p>Equivalent to Rust's codew_legacy_shim.rs — a shorthand entry point
 * for those who prefer typing {@code j} instead of {@code jay}.
 */
public final class JayShim {

    private JayShim() { }

    public static void main(String[] args) {
        JayCli.main(args);
    }
}
