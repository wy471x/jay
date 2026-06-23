package com.jay.tui.widgets;

import com.jay.tui.state.AppState;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;

/**
 * Renders the top header bar showing provider, session name, loading indicator, and model.
 */
public class HeaderWidget {

    private static final Style HEADER_BG = Style.create().bg(Color.BLUE).fg(Color.WHITE);
    private static final Style HEADER_BG_DIM = Style.create().bg(Color.BLUE).fg(Color.WHITE);
    private static final Style DIM_STYLE = Style.create().fg(Color.DARK_GRAY);
    private static final Style CHIP_STYLE = Style.create().bg(Color.CYAN).fg(Color.BLACK).bold();
    private static final Style LABEL_STYLE = Style.create().fg(Color.WHITE).bold();

    private final AppState state;
    private int tickCount;
    private String sessionName;

    public HeaderWidget(AppState state) {
        this.state = state;
        this.sessionName = "Jay";
    }

    public void setSessionName(String name) {
        this.sessionName = name;
    }

    /** Called each frame tick to advance animations. */
    public void tick() {
        tickCount++;
    }

    /** Render the header bar (1 line). Returns the height used (always 1). */
    public int render(Frame frame, Rect area) {
        var buf = frame.buffer();
        int x = area.x();
        int y = area.y();
        int w = area.width();

        // Fill entire header row with blue background
        var pad = new StringBuilder();
        for (int i = 0; i < w; i++) pad.append(' ');
        buf.setString(x, y, pad.toString(), HEADER_BG);

        // --- Left cluster: provider chip + session name ---
        var status = state.statusBar();
        var provider = status.provider();
        String providerName = provider != null ? provider.id() : "unknown";
        String chip = " " + providerName + " ";
        buf.setString(x, y, chip, CHIP_STYLE);
        int pos = chip.length();

        // Loading indicator (animated dot cycle when spinner is active)
        if (status.spinner()) {
            String[] spinnerFrames = {"\u25D0", "\u25D3", "\u25D1", "\u25D2"};
            String spinner = " " + spinnerFrames[(tickCount / 8) % spinnerFrames.length] + " ";
            buf.setString(x + pos, y, spinner, DIM_STYLE);
            pos += spinner.length();
        }

        // Session name (truncated)
        String displaySession = sessionName;
        int maxCenter = Math.max(0, w - 30 - pos);
        if (displaySession.length() > maxCenter) {
            displaySession = displaySession.substring(0, Math.max(1, maxCenter)) + "\u2026";
        }
        if (!displaySession.isEmpty()) {
            buf.setString(x + pos, y, " " + displaySession + " ", HEADER_BG_DIM);
            pos += displaySession.length() + 2;
        }

        // --- Right cluster: model name + thread count ---
        String model = status.modelName();
        int threadCount = state.threads().size();
        String rightInfo = model;
        if (threadCount > 0) rightInfo += "  " + threadCount + "t";
        if (rightInfo.length() > w - pos - 2) {
            rightInfo = rightInfo.substring(0, Math.max(1, w - pos - 3));
        }
        buf.setString(x + w - rightInfo.length() - 1, y, rightInfo, LABEL_STYLE);

        return 1;
    }
}
