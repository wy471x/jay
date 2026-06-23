package com.jay.tui.widgets;

import com.jay.tui.markdown.MarkdownParser;
import com.jay.tui.markdown.MarkdownRenderer;
import com.jay.tui.markdown.MarkdownParser.Token;
import com.jay.tui.markdown.MarkdownRenderer.RenderedLine;
import com.jay.tui.state.AppState;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the conversation transcript area without borders.
 * Handles multi-line messages, basic markdown, turn separators, and scrolling.
 */
public class TranscriptWidget {

    private static final Style USER_TEXT = Style.create().fg(Color.CYAN);
    private static final Style TOOL_STYLE = Style.create().fg(Color.YELLOW);
    private static final Style TURN_SEPARATOR = Style.create().fg(Color.DARK_GRAY);
    private static final Style DIM_STYLE = Style.create().fg(Color.DARK_GRAY);
    private static final Style HEADER_STYLE = Style.create().fg(Color.GREEN).bold();

    private final AppState state;
    private final MarkdownParser parser = new MarkdownParser();
    private final MarkdownRenderer renderer = new MarkdownRenderer();

    public TranscriptWidget(AppState state) {
        this.state = state;
    }

    /**
     * Render the transcript into the given area. Returns number of content lines
     * rendered (for scroll tracking).
     */
    public int render(Frame frame, Rect area) {
        var buf = frame.buffer();
        var viewport = state.viewport();

        // Build all rendered lines from current messages
        List<RenderEntry> entries = buildEntries();
        int contentHeight = entries.size();

        // Update viewport content height for scroll calculations
        viewport.setContentHeight(contentHeight);
        int viewH = area.height();
        int offset = viewport.scrollOffset();

        // Clamp offset
        if (offset > contentHeight - viewH && contentHeight > viewH) {
            offset = Math.max(0, contentHeight - viewH);
            viewport.scrollTo(offset);
        }

        int row = area.y();
        int endRow = area.y() + viewH;
        int lineIdx = offset;

        while (row < endRow && lineIdx < contentHeight) {
            RenderEntry entry = entries.get(lineIdx);
            int y = row;
            if (y >= area.y() && y < endRow) {
                String text = entry.text();
                int x = area.x() + entry.indent();
                int maxLen = area.width() - entry.indent();
                if (text.length() > maxLen) text = text.substring(0, maxLen);
                buf.setString(x, y, text, entry.style());
            }
            lineIdx++;
            row++;
        }

        // If empty, show welcome
        if (contentHeight == 0) {
            buf.setString(area.x() + 1, area.y(),
                    "Welcome to Jay TUI — type a message to start", HEADER_STYLE);
            buf.setString(area.x() + 1, area.y() + 1,
                    "Ctrl+P palette  |  Ctrl+R history  |  F1 help  |  / slash commands", DIM_STYLE);
            return 2;
        }

        return contentHeight;
    }

    /** Build all visible lines from messages + separators. */
    private List<RenderEntry> buildEntries() {
        List<RenderEntry> entries = new ArrayList<>();
        var messages = state.currentMessages();
        if (messages.isEmpty()) return entries;

        for (var msg : messages) {
            String role = msg.role();
            String content = msg.content() != null ? msg.content() : "";

            if ("user".equals(role)) {
                // User message: > prefix in cyan
                for (String line : content.split("\n", -1)) {
                    for (String wrapped : wrapText(line, 80)) {
                        entries.add(new RenderEntry("> " + wrapped, USER_TEXT, 0));
                    }
                }
                entries.add(new RenderEntry("", DIM_STYLE, 0));
            } else if ("tool".equals(role)) {
                entries.add(new RenderEntry("  [tool] " + content, TOOL_STYLE, 0));
            } else {
                // Assistant message: render markdown
                List<Token> tokens = parser.parse(content);
                List<RenderedLine> rendered = renderer.render(tokens, 80);
                for (var rl : rendered) {
                    entries.add(new RenderEntry(rl.text(), rl.style(), rl.indent()));
                }
                entries.add(new RenderEntry("", DIM_STYLE, 0));
            }
        }

        // Turn separator at end
        if (!entries.isEmpty()) {
            int turnCount = messages.size() / 2; // rough estimate
            entries.add(new RenderEntry(
                    "\u2500".repeat(50) + " Turn " + turnCount + " complete",
                    TURN_SEPARATOR, 0));
        }

        return entries;
    }

    /** Simple word-wrap helper. */
    private List<String> wrapText(String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        if (text.isEmpty()) {
            result.add("");
            return result;
        }
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (line.isEmpty()) {
                line.append(word);
            } else if (line.length() + 1 + word.length() <= maxWidth) {
                line.append(' ').append(word);
            } else {
                result.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (!line.isEmpty()) result.add(line.toString());
        return result;
    }

    /** A single renderable line with style and indent. */
    record RenderEntry(String text, Style style, int indent) {}
}
