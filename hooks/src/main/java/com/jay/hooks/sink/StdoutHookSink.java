package com.jay.hooks.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jay.hooks.HookEvent;
import com.jay.hooks.HookSink;

/**
 * Prints each hook event as a JSON line to stdout.
 * Equivalent to Rust's StdoutHookSink.
 */
public class StdoutHookSink implements HookSink {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void emit(HookEvent event) {
        try {
            System.out.println(mapper.writeValueAsString(event));
        } catch (Exception ignored) {
            // best-effort: silently drop serialization errors
        }
    }
}
