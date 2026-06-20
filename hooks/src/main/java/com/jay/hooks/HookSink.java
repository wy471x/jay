package com.jay.hooks;

/**
 * A destination that can receive HookEvents.
 * Equivalent to Rust's HookSink trait. Implementations handle
 * transport-specific delivery (stdout, file, HTTP, Unix socket).
 */
@FunctionalInterface
public interface HookSink {

    /**
     * Deliver a hook event. Best-effort: implementations should avoid
     * throwing exceptions; the dispatcher silently discards errors.
     */
    void emit(HookEvent event);
}
