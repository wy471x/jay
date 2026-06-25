package com.jay.tui.core.compaction;

/**
 * Configuration for automatic context compaction.
 * Mirrors Rust {@code CompactionConfig}.
 */
public class CompactionConfig {

    public static final CompactionConfig DISABLED = new CompactionConfig(false, 0, null);

    private final boolean enabled;
    private final int tokenThreshold;
    private final String model;

    public CompactionConfig(boolean enabled, int tokenThreshold, String model) {
        this.enabled = enabled;
        this.tokenThreshold = tokenThreshold;
        this.model = model;
    }

    public static CompactionConfig disabled() { return DISABLED; }

    public static CompactionConfig of(boolean enabled, int tokenThreshold, String model) {
        return new CompactionConfig(enabled, tokenThreshold, model);
    }

    public boolean enabled() { return enabled; }
    public int tokenThreshold() { return tokenThreshold; }
    public String model() { return model; }
}
