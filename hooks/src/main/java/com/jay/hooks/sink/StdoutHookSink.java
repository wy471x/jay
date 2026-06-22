package com.jay.hooks.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jay.hooks.HookEvent;
import com.jay.hooks.HookSink;
import java.util.logging.Logger;

/**
 * Prints each hook event as a JSON line to stdout.
 * Equivalent to Rust's StdoutHookSink.
 */
public class StdoutHookSink implements HookSink {
    private static final Logger LOGGER = Logger.getLogger(StdoutHookSink.class.getName());

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void emit(HookEvent event) {
        try {
            LOGGER.info(MAPPER.writeValueAsString(event));
        } catch (Exception ignored) {
            // best-effort: silently drop serialization errors
        }
    }
}
