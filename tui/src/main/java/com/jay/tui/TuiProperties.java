package com.jay.tui;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Externalized configuration for the TUI module.
 * Bound from application.properties / config under the "tui" prefix.
 */
@ConfigurationProperties(prefix = "tui")
public class TuiProperties {

    // ── Display ───────────────────────────────────────────────────────

    private boolean altScreen = true;
    private boolean mouseCapture = true;
    private String theme = "default";
    private int sidebarWidth = 30;
    private int frameDelayMs = 50;
    private Map<String, String> keybindings = new LinkedHashMap<>();

    // ── Engine ────────────────────────────────────────────────────────

    private String model = "deepseek-v4-pro";
    private boolean allowShell = true;
    private boolean trustMode = false;
    private boolean autoApprove = false;
    private boolean showThinking = true;
    private int maxSteps = 100;

    // ── Compaction ────────────────────────────────────────────────────

    private boolean compactionEnabled = true;
    private int compactionTokenThreshold = 80_000;
    private String compactionModel = "deepseek-v4-flash";

    // ── Streaming ─────────────────────────────────────────────────────

    private int streamChunkTimeoutSecs = 45;
    private int streamMaxRetries = 3;

    // ── Workspace ─────────────────────────────────────────────────────

    private String workspace = ".";

    // ── Setters / Getters ─────────────────────────────────────────────

    public boolean altScreen() { return altScreen; }
    public void setAltScreen(boolean v) { altScreen = v; }
    public boolean mouseCapture() { return mouseCapture; }
    public void setMouseCapture(boolean v) { mouseCapture = v; }
    public String theme() { return theme; }
    public void setTheme(String v) { theme = v; }
    public int sidebarWidth() { return sidebarWidth; }
    public void setSidebarWidth(int v) { sidebarWidth = v; }
    public int frameDelayMs() { return frameDelayMs; }
    public void setFrameDelayMs(int v) { frameDelayMs = v; }
    public Map<String, String> keybindings() { return keybindings; }
    public void setKeybindings(Map<String, String> v) { keybindings = v; }

    public String model() { return model; }
    public void setModel(String v) { model = v; }
    public boolean allowShell() { return allowShell; }
    public void setAllowShell(boolean v) { allowShell = v; }
    public boolean trustMode() { return trustMode; }
    public void setTrustMode(boolean v) { trustMode = v; }
    public boolean autoApprove() { return autoApprove; }
    public void setAutoApprove(boolean v) { autoApprove = v; }
    public boolean showThinking() { return showThinking; }
    public void setShowThinking(boolean v) { showThinking = v; }
    public int maxSteps() { return maxSteps; }
    public void setMaxSteps(int v) { maxSteps = v; }

    public boolean compactionEnabled() { return compactionEnabled; }
    public void setCompactionEnabled(boolean v) { compactionEnabled = v; }
    public int compactionTokenThreshold() { return compactionTokenThreshold; }
    public void setCompactionTokenThreshold(int v) { compactionTokenThreshold = v; }
    public String compactionModel() { return compactionModel; }
    public void setCompactionModel(String v) { compactionModel = v; }

    public int streamChunkTimeoutSecs() { return streamChunkTimeoutSecs; }
    public void setStreamChunkTimeoutSecs(int v) { streamChunkTimeoutSecs = v; }
    public int streamMaxRetries() { return streamMaxRetries; }
    public void setStreamMaxRetries(int v) { streamMaxRetries = v; }

    public String workspace() { return workspace; }
    public void setWorkspace(String v) { workspace = v; }
}
