package com.jay.cli.delegate;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Locates the sibling TUI binary (jay-tui or codewhale-tui).
 * In the Java port, this is a JVM process launched with the tui module on the classpath.
 */
public final class TuiBinaryLocator {

    private static final String TUI_BIN_ENV = "JAY_TUI_BIN";
    private static final String LEGACY_TUI_BIN_ENV = "CODEWHALE_TUI_BIN";

    private TuiBinaryLocator() { }

    /** Locate the TUI binary, checking env vars then siblings. */
    public static Path locate() {
        // 1. Explicit env var overrides
        for (String key : new String[]{TUI_BIN_ENV, LEGACY_TUI_BIN_ENV}) {
            String val = System.getenv(key);
            if (val != null && !val.isBlank()) {
                Path p = Path.of(val);
                if (Files.exists(p)) return p;
            }
        }

        // 2. Check for jay-tui binary next to the current java process
        Path jayTui = siblingTuiPath("jay-tui");
        if (Files.exists(jayTui)) return jayTui;

        // 3. Check for codewhale-tui legacy
        Path legacy = siblingTuiPath("codewhale-tui");
        if (Files.exists(legacy)) return legacy;

        // 4. Fallback — return the expected path even if it doesn't exist
        return jayTui;
    }

    private static Path siblingTuiPath(String name) {
        String javaHome = System.getProperty("java.home");
        Path javaBin = Path.of(javaHome);
        return javaBin.resolveSibling(name);
    }
}
