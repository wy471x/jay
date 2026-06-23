package com.jay.tui.widgets;

import com.jay.tui.state.AppState;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;

/**
 * Renders the 1-line footer/status bar at the bottom of the TUI.
 */
public class FooterWidget {

    private static final Style FOOTER_STYLE = Style.create().bg(Color.BLUE).fg(Color.WHITE);
    private static final Style FOOTER_DIM = Style.create().bg(Color.BLUE).fg(Color.WHITE);
    private static final Style FOOTER_ACCENT = Style.create().bg(Color.BLUE).fg(Color.CYAN).bold();

    private final AppState state;

    public FooterWidget(AppState state) {
        this.state = state;
    }

    public void render(Frame frame, Rect area) {
        var buf = frame.buffer();
        int x = area.x();
        int y = area.y();
        int w = area.width();

        // Fill background
        buf.setString(x, y, " ".repeat(w), FOOTER_STYLE);

        var status = state.statusBar();
        if (status.hasExpired()) status.clearStatus();

        // Left: model@provider
        var provider = status.provider();
        String providerId = provider != null ? provider.id() : "unknown";
        String left = " " + status.modelName() + "@" + providerId;
        if (status.spinner()) {
            left = " \u25CC " + left;
        }
        buf.setString(x, y, left, FOOTER_ACCENT);
        int pos = left.length();

        // Center: status message
        String center = status.statusMessage();
        if (!center.isEmpty()) {
            while (pos < 35 && pos < w) {
                buf.setString(x + pos, y, " ", FOOTER_STYLE);
                pos++;
            }
            buf.setString(x + pos, y, " \u2502 " + center, FOOTER_DIM);
            pos += center.length() + 4;
        }

        // Right: context info
        int threadCount = state.threads().size();
        String right = threadCount + " threads";
        String model = state.currentModel();
        if (model != null && !model.isEmpty()) {
            right = model + "  " + right;
        }
        if (right.length() < w - pos) {
            buf.setString(x + w - right.length() - 1, y, right, FOOTER_DIM);
        }
    }
}
