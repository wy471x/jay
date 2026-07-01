package com.jay.tui.core.compaction;

/**
 * Configuration for automatic context compaction.
 * Mirrors Rust {@code CompactionConfig}.
 *
 * <p>Defaults follow CodeWhale v0.8.64: enabled=true, 800K token threshold
 * (80% of V4's 1M window), cache_summary=true.
 */
public class CompactionConfig {

    public static final CompactionConfig DISABLED = new CompactionConfig(false, 0, null, false);

    /** v0.8.11: 800K default threshold — 80% of V4's 1M context window. */
    public static final int DEFAULT_TOKEN_THRESHOLD = 800_000;
    public static final String DEFAULT_SUMMARY_MODEL = "deepseek-v4-flash";

    private final boolean enabled;
    private final int tokenThreshold;
    private final String model;
    private final boolean cacheSummary;

    public CompactionConfig(boolean enabled, int tokenThreshold, String model, boolean cacheSummary) {
        this.enabled = enabled;
        this.tokenThreshold = tokenThreshold;
        this.model = model;
        this.cacheSummary = cacheSummary;
    }

    public static CompactionConfig disabled() { return DISABLED; }

    public static CompactionConfig of(boolean enabled, int tokenThreshold, String model) {
        return new CompactionConfig(enabled, tokenThreshold, model, true);
    }

    public static CompactionConfig of(boolean enabled, int tokenThreshold, String model,
                                      boolean cacheSummary) {
        return new CompactionConfig(enabled, tokenThreshold, model, cacheSummary);
    }

    /** Default compaction config — enabled, 800K threshold, Flash model for summaries. */
    public static CompactionConfig defaultConfig() {
        return new CompactionConfig(true, DEFAULT_TOKEN_THRESHOLD,
                DEFAULT_SUMMARY_MODEL, true);
    }

    public boolean enabled() { return enabled; }
    public int tokenThreshold() { return tokenThreshold; }
    public String model() { return model; }
    public boolean cacheSummary() { return cacheSummary; }
}
