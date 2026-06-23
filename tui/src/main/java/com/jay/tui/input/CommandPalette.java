package com.jay.tui.input;

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
import java.util.Comparator;
import java.util.List;

/**
 * Unified command palette with scoped queries and scored substring matching.
 * Activated by Ctrl+P or a dedicated hotkey.
 *
 * <p>Scoped queries:
 * <ul>
 *   <li>{@code c:text} — search commands only</li>
 *   <li>{@code s:text} — search skills (future)</li>
 *   <li>{@code t:text} — search tools (future)</li>
 *   <li>No prefix — search all categories</li>
 * </ul>
 *
 * <p>Equivalent to Rust's command_palette.rs.
 */
public class CommandPalette {

    private static final Style HIGHLIGHT_STYLE = Style.create().fg(Color.YELLOW).bold();
    private static final Style SCOPE_STYLE = Style.create().fg(Color.CYAN);

    public enum Scope { ALL, COMMANDS, SKILLS, TOOLS }

    private final CommandRegistry registry;
    private final List<Entry> allEntries = new ArrayList<>();
    private final List<Entry> filtered = new ArrayList<>();
    private String query = "";
    private int selectedIndex;
    private boolean visible;

    public record Entry(String label, String description, String scope, int score) {}

    public CommandPalette(CommandRegistry registry) {
        this.registry = registry;
        rebuildIndex();
    }

    private void rebuildIndex() {
        allEntries.clear();
        for (var cmd : registry.list()) {
            allEntries.add(new Entry(cmd.name(), cmd.description(), "cmd", 0));
        }
    }

    public void show() {
        visible = true;
        query = "";
        selectedIndex = 0;
        applyFilter();
    }

    public void hide() {
        visible = false;
    }

    public boolean isVisible() { return visible; }

    public void setQuery(String q) {
        query = q.toLowerCase();
        selectedIndex = 0;
        applyFilter();
    }

    public void appendQuery(char c) {
        query += c;
        selectedIndex = 0;
        applyFilter();
    }

    public void backspaceQuery() {
        if (!query.isEmpty()) {
            query = query.substring(0, query.length() - 1);
            selectedIndex = 0;
            applyFilter();
        }
    }

    public void selectNext() {
        if (!filtered.isEmpty()) {
            selectedIndex = (selectedIndex + 1) % filtered.size();
        }
    }

    public void selectPrev() {
        if (!filtered.isEmpty()) {
            selectedIndex = (selectedIndex - 1 + filtered.size()) % filtered.size();
        }
    }

    public Entry selectedEntry() {
        return filtered.isEmpty() ? null : filtered.get(selectedIndex);
    }

    private void applyFilter() {
        filtered.clear();
        var scope = Scope.ALL;
        String search = query;

        if (query.startsWith("c:")) { scope = Scope.COMMANDS; search = query.substring(2); }
        else if (query.startsWith("s:")) { scope = Scope.SKILLS; search = query.substring(2); }
        else if (query.startsWith("t:")) { scope = Scope.TOOLS; search = query.substring(2); }

        for (var entry : allEntries) {
            boolean matchScope = switch (scope) {
                case ALL -> true;
                case COMMANDS -> "cmd".equals(entry.scope());
                default -> false;
            };
            if (!matchScope) continue;
            if (search.isEmpty() || entry.label().toLowerCase().contains(search)
                    || entry.description().toLowerCase().contains(search)) {
                int score = score(entry, search);
                filtered.add(new Entry(entry.label(), entry.description(),
                        entry.scope(), score));
            }
        }
        filtered.sort(Comparator.comparingInt(Entry::score).reversed());
        selectedIndex = Math.min(selectedIndex, Math.max(0, filtered.size() - 1));
    }

    private int score(Entry entry, String search) {
        if (search.isEmpty()) return 1;
        String label = entry.label().toLowerCase();
        if (label.equals(search)) return 100;
        if (label.startsWith(search)) return 80;
        if (label.contains(search)) return 50;
        if (entry.description().toLowerCase().contains(search)) return 20;
        return 0;
    }

    /** Render the command palette overlay. */
    public void render(Frame frame, Rect area) {
        if (!visible) return;
        int w = Math.min(50, area.width() - 4);
        int h = Math.min(12, filtered.size() + 3);
        int x = area.x() + (area.width() - w) / 2;
        int y = area.y() + 2;
        var modalArea = new Rect(x, y, w, h);

        String title = query.isEmpty() ? "Command Palette" : "Search: " + query;
        var block = Block.builder()
                .title(Title.from(title))
                .borderType(BorderType.ROUNDED)
                .build();
        frame.renderWidget(block, modalArea);

        var buf = frame.buffer();
        int row = y + 1;
        for (int i = 0; i < filtered.size() && row < y + h - 1; i++) {
            var entry = filtered.get(i);
            String scope = "[" + entry.scope() + "] ";
            String text = scope + entry.label();
            if (text.length() > w - 3) text = text.substring(0, w - 4) + "\u2026";
            var style = i == selectedIndex ? HIGHLIGHT_STYLE : Style.EMPTY;
            buf.setString(x + 1, row, scope, SCOPE_STYLE);
            buf.setString(x + 1 + scope.length(), row, entry.label(), style);
            row++;
        }
    }

    /** Handle keyboard input while the palette is visible. */
    public boolean handleKey(char character, KeyCode keyCode, boolean ctrl, boolean alt) {
        if (keyCode == KeyCode.ESCAPE) { hide(); return true; }
        if (keyCode == KeyCode.ENTER) { hide(); return true; }
        if (keyCode == KeyCode.UP) { selectPrev(); return true; }
        if (keyCode == KeyCode.DOWN) { selectNext(); return true; }
        if (keyCode == KeyCode.BACKSPACE) { backspaceQuery(); return true; }
        if (character >= 32 && character < 127 && !ctrl) {
            appendQuery(character);
            return true;
        }
        return false;
    }
}
