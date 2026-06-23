package com.jay.tui.views;

import com.jay.tui.commands.CommandRegistry;
import dev.tamboui.layout.Rect;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Title;

import java.util.ArrayList;
import java.util.List;

/**
 * Searchable help overlay showing slash commands and keybindings.
 * Activated by /help or pressing F1.
 *
 * <p>Equivalent to Rust's views/help.rs.
 */
public class HelpOverlay implements ModalView {

    private static final Style HEADER_STYLE = Style.create().fg(Color.CYAN).bold();
    private static final Style HIGHLIGHT_STYLE = Style.create().fg(Color.YELLOW).bold();

    private final CommandRegistry registry;
    private final List<String> entries = new ArrayList<>();
    private String filter = "";
    private int scrollOffset;
    private int selectedIndex;

    public HelpOverlay(CommandRegistry registry) {
        this.registry = registry;
        rebuildEntries();
    }

    private void rebuildEntries() {
        entries.clear();
        entries.add("=== Commands ===");
        for (var cmd : registry.list()) {
            entries.add(String.format("  /%-15s — %s", cmd.name(), cmd.description()));
        }
        entries.add("");
        entries.add("=== Keybindings ===");
        entries.add("  Enter       — Send message");
        entries.add("  Ctrl+C      — Quit");
        entries.add("  Ctrl+L      — Scroll to bottom");
        entries.add("  Esc         — Cancel / Close menu");
        entries.add("  /           — Slash command menu");
        entries.add("  Ctrl+R      — Search history");
        entries.add("  Alt+Enter   — Newline");
        entries.add("  Tab         — Toggle sidebar");
        entries.add("  F1          — This help");
        entries.add("");
        entries.add("=== Navigation ===");
        entries.add("  /help       — Search commands, type to filter");
        entries.add("  Esc or Enter — Close help");
    }

    public void setFilter(String text) {
        this.filter = text.toLowerCase();
        this.scrollOffset = 0;
        this.selectedIndex = 0;
    }

    @Override
    public String kind() { return "help"; }

    @Override
    public ViewAction handleKey(char character, KeyCode keyCode, boolean ctrl, boolean alt) {
        if (keyCode == KeyCode.ESCAPE || keyCode == KeyCode.ENTER) {
            return new ViewAction.Close();
        }
        if (character == 'j' || keyCode == KeyCode.DOWN) {
            selectedIndex = Math.min(selectedIndex + 1, Math.max(0, visibleEntries().size() - 1));
        } else if (character == 'k' || keyCode == KeyCode.UP) {
            selectedIndex = Math.max(0, selectedIndex - 1);
        } else if (character >= 32 && character < 127) {
            filter += character;
            selectedIndex = 0;
        } else if (keyCode == KeyCode.BACKSPACE) {
            if (!filter.isEmpty()) filter = filter.substring(0, filter.length() - 1);
        }
        return new ViewAction.None();
    }

    @Override
    public void render(Frame frame, Rect area) {
        int w = Math.min(60, area.width() - 4);
        int h = Math.min(20, area.height() - 4);
        int x = area.x() + (area.width() - w) / 2;
        int y = area.y() + (area.height() - h) / 2;
        var modalArea = new Rect(x, y, w, h);

        var block = Block.builder()
                .title(Title.from("Help — " + (filter.isEmpty() ? "type to search" : "filter: '" + filter + "'")))
                .borderType(BorderType.ROUNDED)
                .build();
        frame.renderWidget(block, modalArea);

        var buf = frame.buffer();
        var visible = visibleEntries();
        int row = y + 1;
        for (int i = scrollOffset; i < visible.size() && row < y + h - 1; i++) {
            String line = visible.get(i);
            if (line.length() > w - 2) line = line.substring(0, w - 3) + "\u2026";
            var style = i == selectedIndex ? HIGHLIGHT_STYLE : Style.EMPTY;
            buf.setString(x + 1, row, line, style);
            row++;
        }
    }

    private List<String> visibleEntries() {
        if (filter.isEmpty()) return entries;
        return entries.stream()
                .filter(e -> e.toLowerCase().contains(filter))
                .toList();
    }
}
