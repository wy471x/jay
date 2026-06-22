package com.jay.tui.state;

import com.jay.agent.ProviderKind;

import java.time.Instant;

/**
 * Manages footer/status bar state: current model/provider, spinner,
 * and transient status messages with auto-dismiss.
 *
 * <p>Equivalent to Rust's status bar rendering state in App.
 */
public class StatusBarState {

    public enum Severity { INFO, WARN, ERROR }

    private String modelName = "deepseek-v4-flash";
    private ProviderKind provider = ProviderKind.DEEPSEEK;
    private boolean spinner;
    private String statusMessage = "";
    private Severity severity = Severity.INFO;
    private Instant messageExpiry;
    private int messageCount;

    public void setModelInfo(String model, ProviderKind provider) {
        this.modelName = model;
        this.provider = provider;
    }

    /** Set a transient status message. Auto-dismissed after ~5 seconds. */
    public void setStatus(String message, Severity severity) {
        this.statusMessage = message;
        this.severity = severity;
        this.messageExpiry = Instant.now().plusSeconds(5);
    }

    public boolean hasExpired() {
        return messageExpiry != null && Instant.now().isAfter(messageExpiry);
    }

    public void clearStatus() {
        statusMessage = "";
        messageExpiry = null;
    }

    public String modelName()      { return modelName; }
    public ProviderKind provider() { return provider; }
    public boolean spinner()       { return spinner; }
    public void setSpinner(boolean v) { spinner = v; }
    public String statusMessage()  { return statusMessage; }
    public Severity severity()     { return severity; }
    public int messageCount()      { return messageCount; }
    public void setMessageCount(int v) { messageCount = v; }
}
