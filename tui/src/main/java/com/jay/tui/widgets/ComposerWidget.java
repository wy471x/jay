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

        // Separator line above composer
        String sep = "\u2500".repeat(Math.min(w, 60));
        buf.setString(x, row, sep, DIM_STYLE);
        row++;

        // Input line: "> text█"
        String text = composer.text();
        int cursorPos = composer.cursor();
        StringBuilder inputLine = new StringBuilder();
        inputLine.append("> ");
        if (text.isEmpty()) {
            inputLine.append(" "); // place for cursor
        } else {
            // Show text with cursor highlight
            String before = text.substring(0, Math.min(cursorPos, text.length()));
            String at = (cursorPos < text.length()) ? String.valueOf(text.charAt(cursorPos)) : " ";
            String after = (cursorPos + 1 < text.length()) ? text.substring(cursorPos + 1) : "";

            inputLine.append(before);
            buf.setString(x + 2, row, before, INPUT_STYLE);
            buf.setString(x + 2 + before.length(), row, at, CURSOR_STYLE);
            if (!after.isEmpty()) {
                buf.setString(x + 2 + before.length() + 1, row, after, INPUT_STYLE);
            }
            String display = "> " + inputLine.toString().replaceAll("\\n", " ");
            if (display.length() > w) display = display.substring(display.length() - w);
            buf.setString(x, row, display, INPUT_STYLE);
            // Re-draw cursor
            int cursorX = x + 2 + before.length();
            if (cursorX < x + w) {
                buf.setString(cursorX, row, at, CURSOR_STYLE);
            }
        }

        if (text.isEmpty()) {
            buf.setString(x, row, "> ", PROMPT_STYLE);
            buf.setString(x + 2, row, " ", CURSOR_STYLE); // blink cursor
            // Hint text
            buf.setString(x + 4, row, "Type a message or /command...", DIM_STYLE);
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

    private String padRight(String s, int n) {
        if (s.length() >= n) return s;
        return s + " ".repeat(n - s.length());
    }
}
