package com.jay.tui.state;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Manages the text input buffer, cursor position, and history for the
 * composer area at the bottom of the TUI.
 *
 * <p>Equivalent to Rust's ComposerState embedded in App.
 */
public class ComposerState {

    private static final int MAX_HISTORY = 100;

    private final StringBuilder buffer = new StringBuilder();
    private int cursor;
    private final Deque<String> history = new ArrayDeque<>();
    private int historyIndex = -1;
    private boolean slashActive;
    private String slashFilter = "";
    private boolean composing;

    /** Insert a character at cursor position. */
    public void insertChar(char c) {
        buffer.insert(cursor, c);
        cursor++;
        if (cursor == 1 && c == '/') {
            slashActive = true;
        }
    }

    /** Delete character before cursor (Backspace). */
    public void deleteBefore() {
        if (cursor > 0) {
            buffer.deleteCharAt(cursor - 1);
            cursor--;
            updateSlashState();
        }
    }

    /** Delete character after cursor (Delete). */
    public void deleteAfter() {
        if (cursor < buffer.length()) {
            buffer.deleteCharAt(cursor);
            updateSlashState();
        }
    }

    public void moveCursorLeft()  { if (cursor > 0) cursor--; }
    public void moveCursorRight() { if (cursor < buffer.length()) cursor++; }
    public void moveCursorHome()  { cursor = 0; }
    public void moveCursorEnd()   { cursor = buffer.length(); }

    /** Commit the current buffer to history and clear. */
    public String commit() {
        var text = buffer.toString();
        if (!text.isBlank()) {
            if (history.size() >= MAX_HISTORY) {
                history.removeLast();
            }
            history.addFirst(text);
        }
        clear();
        return text;
    }

    /** Step backward through history (Up arrow). */
    public String historyPrev() {
        if (history.isEmpty()) return "";
        historyIndex = Math.min(historyIndex + 1, history.size() - 1);
        return restoreHistoryItem();
    }

    /** Step forward through history (Down arrow). */
    public String historyNext() {
        if (historyIndex <= 0) {
            historyIndex = -1;
            clear();
            return "";
        }
        historyIndex--;
        return restoreHistoryItem();
    }

    private String restoreHistoryItem() {
        var iter = history.iterator();
        for (int i = 0; i < historyIndex && iter.hasNext(); i++) iter.next();
        var text = iter.hasNext() ? iter.next() : "";
        buffer.replace(0, buffer.length(), text);
        cursor = text.length();
        return text;
    }

    /** Clear buffer and reset all transient state. */
    public void clear() {
        buffer.setLength(0);
        cursor = 0;
        historyIndex = -1;
        slashActive = false;
        slashFilter = "";
    }

    private void updateSlashState() {
        slashActive = buffer.length() > 0 && buffer.charAt(0) == '/';
        if (slashActive) {
            slashFilter = buffer.substring(1);
        } else {
            slashFilter = "";
        }
    }

    // ── Getters ───────────────────────────────────────────────────

    public String text()          { return buffer.toString(); }
    public int cursor()           { return cursor; }
    public boolean slashActive()  { return slashActive; }
    public String slashFilter()   { return slashFilter; }
    public boolean composing()    { return composing; }
    public void composing(boolean v) { composing = v; }
    public int length()           { return buffer.length(); }

    /** Replace entire buffer content (for history restore). */
    public void setText(String text) {
        buffer.replace(0, buffer.length(), text);
        cursor = text.length();
        updateSlashState();
    }

    // ── History search (Ctrl+R) ────────────────────────────────────

    private String historySearchQuery = "";
    private int historySearchIndex = -1;
    private boolean historySearchActive;

    /** Begin history search mode. */
    public void startHistorySearch() {
        historySearchActive = true;
        historySearchQuery = "";
        historySearchIndex = -1;
    }

    /** Add a character to the history search query. */
    public void historySearchAppend(char c) {
        historySearchQuery += c;
        historySearchIndex = -1;
        findNextMatch();
    }

    /** Backspace in history search query. */
    public void historySearchBackspace() {
        if (!historySearchQuery.isEmpty()) {
            historySearchQuery = historySearchQuery.substring(0, historySearchQuery.length() - 1);
            historySearchIndex = -1;
            findNextMatch();
        }
    }

    /** Find the next match for the current search query. */
    public String findNextMatch() {
        if (!historySearchActive || historySearchQuery.isEmpty()) return "";
        var filtered = history.stream()
                .filter(h -> h.toLowerCase().contains(historySearchQuery.toLowerCase()))
                .toList();
        if (filtered.isEmpty()) return "";
        historySearchIndex = (historySearchIndex + 1) % filtered.size();
        return filtered.get(historySearchIndex);
    }

    /** Accept the current search result and exit search mode. */
    public String acceptHistorySearch() {
        if (!historySearchActive) return "";
        historySearchActive = false;
        var result = findNextMatch();
        if (!result.isEmpty()) {
            setText(result);
        }
        historySearchQuery = "";
        historySearchIndex = -1;
        return result;
    }

    public void cancelHistorySearch() {
        historySearchActive = false;
        historySearchQuery = "";
        historySearchIndex = -1;
    }

    public boolean historySearchActive() { return historySearchActive; }

    // ── Escape action chain priority ────────────────────────────────

    /**
     * Cascading escape: close slash menu > discard draft > pause > cancel.
     * Returns the type of action that should be taken by the host.
     */
    public EscapeAction handleEscape() {
        if (slashActive) {
            slashActive = false;
            slashFilter = "";
            return EscapeAction.CLOSE_SLASH_MENU;
        }
        if (!buffer.isEmpty()) {
            clear();
            return EscapeAction.DISCARD_DRAFT;
        }
        return EscapeAction.NOOP;
    }

    public enum EscapeAction { CLOSE_SLASH_MENU, DISCARD_DRAFT, NOOP }

    // ── Multiline support ───────────────────────────────────────────

    /** Insert a literal newline at cursor (Alt+Enter / Ctrl+J). */
    public void insertNewline() {
        buffer.insert(cursor, '\n');
        cursor++;
    }

    /** Check if buffer contains newlines. */
    public boolean isMultiline() {
        return buffer.indexOf("\n") >= 0;
    }
}
