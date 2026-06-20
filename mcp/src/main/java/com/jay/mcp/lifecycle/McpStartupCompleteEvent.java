package com.jay.mcp.lifecycle;

import java.util.List;

public record McpStartupCompleteEvent(
    List<String> ready,
    List<McpStartupFailure> failed,
    List<String> cancelled
) {}
