package com.jay.protocol.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record McpStartupCompleteEvent(List<String> ready, List<McpStartupFailure> failed, List<String> cancelled) {
    public McpStartupCompleteEvent {
        if (ready == null) ready = List.of();
        if (failed == null) failed = List.of();
        if (cancelled == null) cancelled = List.of();
    }
}
