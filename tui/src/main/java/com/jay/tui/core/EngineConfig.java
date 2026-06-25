package com.jay.tui.core;

import com.jay.tui.core.compaction.CompactionConfig;

import java.nio.file.Path;

/**
 * Immutable configuration for {@link Engine}.
 * Mirrors Rust {@code EngineConfig} — lightweight subset of the 47-field original.
 *
 * <p>Use the builder for construction:
 * <pre>{@code
 * var config = EngineConfig.builder()
 *     .model("deepseek-v4-pro")
 *     .workspace(Path.of("."))
 *     .build();
 * }</pre>
 */
public class EngineConfig {

    private final String model;
    private final Path workspace;
    private final boolean allowShell;
    private final boolean trustMode;
    private final boolean autoApprove;
    private final int maxSteps;
    private final CompactionConfig compaction;
    private final int streamChunkTimeoutSecs;
    private final boolean showThinking;
    private final boolean memoryEnabled;

    private EngineConfig(Builder builder) {
        this.model = builder.model;
        this.workspace = builder.workspace;
        this.allowShell = builder.allowShell;
        this.trustMode = builder.trustMode;
        this.autoApprove = builder.autoApprove;
        this.maxSteps = builder.maxSteps;
        this.compaction = builder.compaction;
        this.streamChunkTimeoutSecs = builder.streamChunkTimeoutSecs;
        this.showThinking = builder.showThinking;
        this.memoryEnabled = builder.memoryEnabled;
    }

    // ── Getters ───────────────────────────────────────────────────────

    public String model() { return model; }
    public Path workspace() { return workspace; }
    public boolean allowShell() { return allowShell; }
    public boolean trustMode() { return trustMode; }
    public boolean autoApprove() { return autoApprove; }
    public int maxSteps() { return maxSteps; }
    public CompactionConfig compaction() { return compaction; }
    public int streamChunkTimeoutSecs() { return streamChunkTimeoutSecs; }
    public boolean showThinking() { return showThinking; }
    public boolean memoryEnabled() { return memoryEnabled; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String model = "deepseek-v4-pro";
        private Path workspace = Path.of(".");
        private boolean allowShell = true;
        private boolean trustMode;
        private boolean autoApprove;
        private int maxSteps = 100;
        private CompactionConfig compaction = CompactionConfig.disabled();
        private int streamChunkTimeoutSecs = 45;
        private boolean showThinking = true;
        private boolean memoryEnabled = true;

        public Builder model(String model) { this.model = model; return this; }
        public Builder workspace(Path ws) { this.workspace = ws; return this; }
        public Builder allowShell(boolean v) { this.allowShell = v; return this; }
        public Builder trustMode(boolean v) { this.trustMode = v; return this; }
        public Builder autoApprove(boolean v) { this.autoApprove = v; return this; }
        public Builder maxSteps(int n) { this.maxSteps = n; return this; }
        public Builder compaction(CompactionConfig c) { this.compaction = c; return this; }
        public Builder streamChunkTimeoutSecs(int s) { this.streamChunkTimeoutSecs = s; return this; }
        public Builder showThinking(boolean v) { this.showThinking = v; return this; }
        public Builder memoryEnabled(boolean v) { this.memoryEnabled = v; return this; }

        public EngineConfig build() { return new EngineConfig(this); }
    }
}
