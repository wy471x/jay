package com.jay.agent;

import java.util.List;
import java.util.Optional;

/**
 * The result of resolving a user-requested model name to a concrete model entry.
 * Equivalent to Rust's ModelResolution struct.
 */
public record ModelResolution(
        Optional<String> requested,
        ModelInfo resolved,
        boolean usedFallback,
        List<String> fallbackChain
) {}
