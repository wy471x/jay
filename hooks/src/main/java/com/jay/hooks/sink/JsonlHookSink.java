package com.jay.hooks.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jay.hooks.HookEvent;
import com.jay.hooks.HookSink;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * Appends hook events as timestamped JSON lines to a file.
 * Equivalent to Rust's JsonlHookSink.
 */
public class JsonlHookSink implements HookSink {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final Path path;

    public JsonlHookSink(Path path) {
        this.path = path;
    }

    @Override
    public void emit(HookEvent event) {
        try {
            Files.createDirectories(path.getParent());

            ObjectNode envelope = mapper.createObjectNode();
            envelope.put("at", Instant.now().toString());
            envelope.set("event", mapper.valueToTree(event));

            String line = mapper.writeValueAsString(envelope) + "\n";
            Files.writeString(path, line,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // best-effort: silently drop IO errors
        }
    }
}
