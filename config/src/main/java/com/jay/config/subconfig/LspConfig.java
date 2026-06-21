package com.jay.config.subconfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class LspConfig {
    private boolean enabled;
    @JsonProperty("poll_after_edit_ms") private int pollAfterEditMs;
    @JsonProperty("max_diagnostics_per_file") private int maxDiagnosticsPerFile;
    @JsonProperty("include_warnings") private boolean includeWarnings;
    private List<String> servers = List.of();
    public boolean enabled() { return enabled; }
    public void enabled(boolean v) { enabled = v; }
    public int pollAfterEditMs() { return pollAfterEditMs; }
    public void pollAfterEditMs(int v) { pollAfterEditMs = v; }
    public int maxDiagnosticsPerFile() { return maxDiagnosticsPerFile; }
    public void maxDiagnosticsPerFile(int v) { maxDiagnosticsPerFile = v; }
    public boolean includeWarnings() { return includeWarnings; }
    public void includeWarnings(boolean v) { includeWarnings = v; }
    public List<String> servers() { return servers; }
    public void servers(List<String> v) { servers = v; }
}
