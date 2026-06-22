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

    private boolean altScreen = true;
    private boolean mouseCapture = true;
    private String theme = "default";
    private int sidebarWidth = 30;
    private int frameDelayMs = 50;
    private Map<String, String> keybindings = new LinkedHashMap<>();

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
}
