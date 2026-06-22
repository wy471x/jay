package com.jay.tui.core;

import com.jay.config.model.CliRuntimeOverrides;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable session metadata for a TUI instance.
 * Equivalent to Rust's session context initialisation record.
 */
public record TuiSession(
        String sessionId,
        Instant startTime,
        String initialThreadId,
        CliRuntimeOverrides overrides
) {
    public TuiSession {
        if (sessionId == null) {
            sessionId = "tui-" + UUID.randomUUID();
        }
        if (startTime == null) {
            startTime = Instant.now();
        }
    }

    public TuiSession() {
        this(null, null, null, new CliRuntimeOverrides());
    }
}
