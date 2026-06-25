package com.jay.tui.widgets;

import com.jay.tui.commands.CommandRegistry;
import com.jay.tui.commands.CommandRegistry.CommandInfo;
import com.jay.tui.state.ComposerState;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;

import java.util.List;

/**
 * Renders the input area: prompt line with cursor + slash command suggestions.
 */
public class ComposerWidget {

    private static final Style PROMPT_STYLE = Style.create().fg(Color.CYAN).bold();
    private static final Style INPUT_STYLE = Style.create().fg(Color.YELLOW);
    private static final Style CURSOR_STYLE = Style.create().bg(Color.WHITE).fg(Color.BLACK);
    private static final Style SLASH_LABEL = Style.create().fg(Color.YELLOW).bold();
    private static final Style DIM_STYLE = Style.create().fg(Color.DARK_GRAY);

    private final ComposerState composer;
    private final CommandRegistry registry;

    public ComposerWidget(ComposerState composer, CommandRegistry registry) {
        this.composer = composer;
        this.registry = registry;
    }

    public void tick() { /* reserved for animation */ }

    /** Returns height needed for composer + slash menu. Always at least 3. */
    public int desiredHeight() {
        int base = 3; // prompt + padding
        if (composer.slashActive() || composer.historySearchActive()) {
            List<CommandInfo> matches = registry.list().stream()
                    .filter(c -> !composer.slashFilter().isEmpty()
                            && c.name().toLowerCase().contains(composer.slashFilter().toLowerCase()))
                    .limit(6)
                    .toList();
            base += Math.min(matches.size(), 4);
        }
        return base;
    }

    public void render(Frame frame, Rect area) {
        var buf = frame.buffer();
        int x = area.x();
        int w = area.width();
        int row = area.y();

        // Separator line above composer — full width
        String sep = "\u2500".repeat(w);
        buf.setString(x, row, sep, DIM_STYLE);
        row++;

        // Input line: "> text█"
        String text = composer.text();
        int cursorPos = composer.cursor();

        if (text.isEmpty()) {
            buf.setString(x, row, "> ", PROMPT_STYLE);
            buf.setString(x + 2, row, " ", CURSOR_STYLE); // blink cursor
            buf.setString(x + 4, row, "Type a message or /command...", DIM_STYLE);
        } else {
            // Show text with cursor highlight — split into before/at/after
            int cursor = Math.min(cursorPos, text.length());
            String before = text.substring(0, cursor);
            String at = cursor < text.length() ? String.valueOf(text.charAt(cursor)) : " ";
            String after = cursor + 1 < text.length() ? text.substring(cursor + 1) : "";

            int beforeWidth = displayWidth(before);
            int atWidth = at.equals(" ") ? 1 : displayWidth(at);

            buf.setString(x, row, "> ", PROMPT_STYLE);
            buf.setString(x + 2, row, before, INPUT_STYLE);
            buf.setString(x + 2 + beforeWidth, row, at, CURSOR_STYLE);
            if (!after.isEmpty()) {
                buf.setString(x + 2 + beforeWidth + atWidth, row, after, INPUT_STYLE);
            }
        }

        row++;

        // History search indicator
        if (composer.historySearchActive()) {
            String hint = "(history search: " + composer.slashFilter() + ")";
            buf.setString(x + 2, row, hint, DIM_STYLE);
            row++;
        }

        // Slash command suggestions
        if (composer.slashActive()) {
            String filter = composer.slashFilter();
            List<CommandInfo> matches = registry.list().stream()
                    .filter(c -> c.name().toLowerCase().contains(filter.toLowerCase()))
                    .limit(5)
                    .toList();

            for (var cmd : matches) {
                if (row >= area.y() + area.height()) break;
                String line = "  /" + cmd.name();
                String desc = cmd.description();
                if (desc != null && !desc.isEmpty()) {
                    line = padRight(line, 24) + desc;
                }
                if (line.length() > w - 2) line = line.substring(0, w - 3);
                buf.setString(x + 1, row, line, SLASH_LABEL);
                row++;
            }
        }
    }

    /** Terminal display width: CJK characters take 2 columns, others 1. */
    private static int displayWidth(String s) {
        if (s == null || s.isEmpty()) return 0;
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            w += displayWidth(s.charAt(i));
        }
        return w;
    }

    private static int displayWidth(char c) {
        // CJK Unified Ideographs and common wide ranges
        if (c >= 0x1100 && c <= 0x115F) return 2;   // Hangul
        if (c >= 0x2329 && c <= 0x232A) return 2;   // angle brackets
        if (c >= 0x2E80 && c <= 0xA4CF) return 2;   // CJK Radicals .. Yi
        if (c >= 0xAC00 && c <= 0xD7A3) return 2;   // Hangul Syllables
        if (c >= 0xF900 && c <= 0xFAFF) return 2;   // CJK Compat
        if (c >= 0xFE10 && c <= 0xFE19) return 2;   // vertical forms
        if (c >= 0xFE30 && c <= 0xFE6F) return 2;   // CJK Compat Forms
        if (c >= 0xFF00 && c <= 0xFF60) return 2;   // Fullwidth Forms
        if (c >= 0xFFE0 && c <= 0xFFE6) return 2;   // Fullwidth Signs
        if (c >= 0x1F300 && c <= 0x1F64F) return 2; // Emoji
        if (c >= 0x1F680 && c <= 0x1F9FF) return 2; // Emoji transport/symbols
        return 1;
    }

    private String padRight(String s, int n) {
        if (s.length() >= n) return s;
        return s + " ".repeat(n - s.length());
    }
}
