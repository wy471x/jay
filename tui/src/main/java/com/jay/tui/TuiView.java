package com.jay.tui;

import com.jay.tui.commands.CommandRegistry;
import com.jay.tui.input.SlashMenu;
import com.jay.tui.state.AppState;
import com.jay.tui.state.ComposerState;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Title;

/**
 * Renders the TUI view using TamboUI Frame API.
 * Layout: sidebar | transcript | composer | footer
 */
public class TuiView {

    private static final int FOOTER_HEIGHT = 1;
    private static final int COMPOSER_HEIGHT = 3;
    private static final int SIDEBAR_WIDTH = 30;

    private static final Style HEADER_STYLE = Style.create().fg(Color.GREEN).bold();
    private static final Style INPUT_STYLE = Style.create().fg(Color.YELLOW);
    private static final Style USER_STYLE = Style.create().fg(Color.CYAN).bold();
    private static final Style TOOL_STYLE = Style.create().fg(Color.YELLOW);
    private static final Style FOOTER_STYLE = Style.create().fg(Color.BLACK).bg(Color.WHITE).bold();

    private final AppState state;
    private final ComposerState composer;
    private final SlashMenu slashMenu;
    private final CommandRegistry commandRegistry;
    private final List<String> transcriptLines = new ArrayList<>();

    public TuiView(AppState state, ComposerState composer,
                   SlashMenu slashMenu, CommandRegistry commandRegistry) {
        this.state = state;
        this.composer = composer;
        this.slashMenu = slashMenu;
        this.commandRegistry = commandRegistry;
    }

    /** Append a line to the transcript buffer. */
    public void appendTranscript(String line) {
        transcriptLines.add(line);
    }

    public void render(Frame frame) {
        Rect area = frame.area();
        int w = area.width();
        int h = area.height();
        if (w < 10 || h < 10) return;

        var buf = frame.buffer();

        boolean sidebarVisible = state.sidebar().visible();
        int sidebarW = sidebarVisible ? Math.min(SIDEBAR_WIDTH, w / 4) : 0;
        int mainX = sidebarW;
        int mainW = w - sidebarW;
        int footerY = h - FOOTER_HEIGHT;
        int composerY = footerY - COMPOSER_HEIGHT;
        int transcriptH = Math.max(1, composerY);

        if (sidebarVisible) {
            renderSidebar(frame, new Rect(0, 0, sidebarW, h));
        }
        renderTranscript(frame, new Rect(mainX, 0, mainW, transcriptH));
        renderComposer(frame, new Rect(mainX, composerY, mainW, COMPOSER_HEIGHT));
        renderFooter(frame, new Rect(0, footerY, w, FOOTER_HEIGHT));
    }

    private void renderSidebar(Frame frame, Rect area) {
        var block = Block.builder()
                .title(Title.from("Threads"))
                .borderType(BorderType.ROUNDED)
                .build();
        frame.renderWidget(block, area);

        var buf = frame.buffer();
        var threads = state.sidebar().threads();
        String activeId = state.activeThreadId();
        int row = area.y() + 1;
        for (var t : threads) {
            if (row >= area.y() + area.height() - 1) break;
            String preview = t.preview() != null ? t.preview() : t.id();
            if (preview.length() > area.width() - 4) {
                preview = preview.substring(0, area.width() - 5) + "\u2026";
            }
            String line = (t.id().equals(activeId) ? "* " : "  ") + preview;
            buf.setString(area.x() + 1, row, line, Style.EMPTY);
            row++;
        }
    }

    private void renderTranscript(Frame frame, Rect area) {
        var block = Block.builder()
                .title(Title.from("Transcript"))
                .borderType(BorderType.ROUNDED)
                .build();
        frame.renderWidget(block, area);

        var buf = frame.buffer();
        var messages = state.currentMessages();
        int row = area.y() + 1;
        if (messages.isEmpty()) {
            buf.setString(area.x() + 1, row,
                    "Welcome to Jay TUI — type /help for commands", HEADER_STYLE);
            return;
        }
        for (var msg : messages) {
            if (row >= area.y() + area.height() - 1) break;
            String role = msg.role();
            String content = msg.content() != null ? msg.content() : "";
            String line = switch (role) {
                case "user" -> "> " + content;
                case "tool" -> "  [tool] " + content;
                default -> content;
            };
            if (line.length() > area.width() - 2) {
                line = line.substring(0, area.width() - 3) + "\u2026";
            }
            Style style = "user".equals(role) ? USER_STYLE : Style.EMPTY;
            buf.setString(area.x() + 1, row, line, style);
            row++;
        }
    }

    private void renderComposer(Frame frame, Rect area) {
        var status = state.statusBar();
        String title = status.spinner() ? "\u25CC Composer" : "Composer";
        var block = Block.builder()
                .title(Title.from(title))
                .borderType(BorderType.ROUNDED)
                .build();
        frame.renderWidget(block, area);

        var buf = frame.buffer();
        String text = composer.text();
        String display = "> " + text;
        if (display.length() > area.width() - 2) {
            display = display.substring(display.length() - area.width() + 2);
        }
        buf.setString(area.x() + 1, area.y() + 1, display, INPUT_STYLE);
    }

    private void renderFooter(Frame frame, Rect area) {
        var status = state.statusBar();
        if (status.hasExpired()) status.clearStatus();

        String left = (status.spinner() ? "\u25CC " : "")
                + status.modelName() + "@" + status.provider().id();
        String center = status.statusMessage();
        int threadCount = state.threads().size();

        var sb = new StringBuilder();
        sb.append(left);
        if (!center.isEmpty()) {
            while (sb.length() < 40) sb.append(' ');
            sb.append("\u2502 ").append(center);
        }
        while (sb.length() < Math.max(70, area.width() - 15)) sb.append(' ');
        sb.append(threadCount).append(" threads");
        String line = sb.toString();
        if (line.length() > area.width()) line = line.substring(0, area.width());

        var buf = frame.buffer();
        buf.setString(area.x(), area.y(), line, FOOTER_STYLE);
    }
}
