package com.jay.tui.state;

import com.jay.protocol.core.Thread;

import java.util.ArrayList;
import java.util.List;

/**
 * Controls the visibility and selection state of the thread list sidebar.
 * Equivalent to Rust's sidebar focus / thread list panel state.
 */
public class SidebarState {

    private boolean visible;
    private int width = 30;
    private int selectedIndex;
    private List<Thread> threads = List.of();
    private int scrollOffset;

    public void toggle()       { visible = !visible; }
    public void show()         { visible = true; }
    public void hide()         { visible = false; }
    public boolean visible()   { return visible; }
    public int width()         { return width; }
    public void width(int w)   { width = Math.max(20, Math.min(50, w)); }

    public int selectedIndex()    { return selectedIndex; }
    public String selectedThreadId() {
        if (selectedIndex >= 0 && selectedIndex < threads.size()) {
            return threads.get(selectedIndex).id();
        }
        return null;
    }

    public void selectNext() {
        selectedIndex = Math.min(selectedIndex + 1, Math.max(0, threads.size() - 1));
    }

    public void selectPrev() {
        selectedIndex = Math.max(0, selectedIndex - 1);
    }

    public void setThreads(List<Thread> t) {
        this.threads = new ArrayList<>(t);
        this.selectedIndex = Math.min(selectedIndex, Math.max(0, t.size() - 1));
    }

    public List<Thread> threads() { return threads; }
    public int scrollOffset()     { return scrollOffset; }
    public void scrollOffset(int v) { scrollOffset = Math.max(0, v); }
}
