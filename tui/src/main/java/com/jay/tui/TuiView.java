package com.jay.tui;

import com.jay.tui.commands.CommandRegistry;
import com.jay.tui.input.SlashMenu;
import com.jay.tui.state.AppState;
import com.jay.tui.state.ComposerState;
import com.jay.tui.widgets.ComposerWidget;
import com.jay.tui.widgets.FooterWidget;
import com.jay.tui.widgets.HeaderWidget;
import com.jay.tui.widgets.TranscriptWidget;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;

/**
 * Renders the full TUI view: Header | Transcript | Composer | Footer.
 * Sidebar is hidden by default and can be toggled with Ctrl+B.
 */
public class TuiView {

    private static final int HEADER_HEIGHT = 1;
    private static final int FOOTER_HEIGHT = 1;
    private static final int MIN_COMPOSER_HEIGHT = 3;
    private static final int SIDEBAR_WIDTH = 30;

    private final AppState state;
    private final List<String> transcriptLines = new ArrayList<>();

    // Sub-widgets
    private final HeaderWidget header;
    private final TranscriptWidget transcript;
    private final ComposerWidget composer;
    private final FooterWidget footer;

    public TuiView(AppState state, ComposerState composerState,
                   SlashMenu slashMenu, CommandRegistry commandRegistry) {
        this.state = state;
        this.header = new HeaderWidget(state);
        this.transcript = new TranscriptWidget(state);
        this.composer = new ComposerWidget(composerState, commandRegistry);
        this.footer = new FooterWidget(state);
    }

    /** Append a line to the transcript buffer (used by send-message handler). */
    public void appendTranscript(String line) {
        transcriptLines.add(line);
    }

    /** Get a copy of the transcript buffer lines (for testing). */
    public List<String> getTranscriptLines() {
        return List.copyOf(transcriptLines);
    }

    public void tick() {
        header.tick();
        composer.tick();
    }

    public void render(Frame frame) {
        Rect area = frame.area();
        int w = area.width();
        int h = area.height();
        if (w < 10 || h < 10) return;

        // Update viewport height
        state.viewport().setViewportHeight(h - HEADER_HEIGHT - FOOTER_HEIGHT - MIN_COMPOSER_HEIGHT);

        boolean sidebarVisible = state.sidebar().visible();
        int sidebarW = sidebarVisible ? Math.min(SIDEBAR_WIDTH, w / 4) : 0;
        int mainX = sidebarW;
        int mainW = w - sidebarW;

        // Compute layout heights
        int composerH = Math.max(MIN_COMPOSER_HEIGHT, composer.desiredHeight());
        int footerY = h - FOOTER_HEIGHT;
        int composerY = footerY - composerH;
        int transcriptY = HEADER_HEIGHT;
        int transcriptH = Math.max(1, composerY - transcriptY);

        // Render
        header.render(frame, new Rect(mainX, 0, mainW, HEADER_HEIGHT));

        if (sidebarVisible) {
            renderSidebar(frame, new Rect(0, 0, sidebarW, h));
        }

        transcript.render(frame, new Rect(mainX, transcriptY, mainW, transcriptH));
        composer.render(frame, new Rect(mainX, composerY, mainW, composerH));
        footer.render(frame, new Rect(0, footerY, w, FOOTER_HEIGHT));
    }

    private void renderSidebar(Frame frame, Rect area) {
        var buf = frame.buffer();
        var threads = state.sidebar().threads();
        String activeId = state.activeThreadId();
        int row = area.y();

        // Simple header
        buf.setString(area.x() + 1, row, " Threads", dev.tamboui.style.Style.create()
                .fg(dev.tamboui.style.Color.CYAN).bold());
        row++;

        for (var t : threads) {
            if (row >= area.y() + area.height()) break;
            String preview = t.preview() != null ? t.preview() : t.id();
            if (preview.length() > area.width() - 4) {
                preview = preview.substring(0, area.width() - 5) + "\u2026";
            }
            boolean active = t.id().equals(activeId);
            String prefix = active ? "\u25B6 " : "  ";
            var style = active
                    ? dev.tamboui.style.Style.create().fg(dev.tamboui.style.Color.CYAN)
                    : dev.tamboui.style.Style.create().fg(dev.tamboui.style.Color.DARK_GRAY);
            buf.setString(area.x() + 1, row, prefix + preview, style);
            row++;
        }
    }
}
