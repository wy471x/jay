package com.jay.hooks;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dispatches hook events to all registered sinks. Best-effort: errors
 * from individual sinks are silently discarded, matching the Rust
 * implementation's resilience guarantee.
 *
 * <p>Thread-safe: sinks are stored in a CopyOnWriteArrayList.
 */
public class HookDispatcher {

    private final List<HookSink> sinks = new CopyOnWriteArrayList<>();

    /** Register a sink. All subsequently emitted events will reach it. */
    public void addSink(HookSink sink) {
        sinks.add(sink);
    }

    /** Remove a previously registered sink. */
    public void removeSink(HookSink sink) {
        sinks.remove(sink);
    }

    /** Number of registered sinks. */
    public int sinkCount() {
        return sinks.size();
    }

    /**
     * Fan out an event to every registered sink.
     * Errors from individual sinks are silently discarded.
     */
    public void emit(HookEvent event) {
        for (HookSink sink : sinks) {
            try {
                sink.emit(event);
            } catch (Exception ignored) {
                // best-effort: one failing sink does not block others
            }
        }
    }
}
