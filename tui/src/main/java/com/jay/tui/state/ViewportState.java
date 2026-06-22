package com.jay.tui.state;

/**
 * Tracks scroll position for the transcript viewport.
 *
 * <p>Equivalent to Rust's ViewportState / scroll tracking in App.
 */
public class ViewportState {

    private int scrollOffset;
    private int contentHeight;
    private int viewportHeight;
    private boolean followBottom = true;

    public void scrollUp(int lines) {
        scrollOffset = Math.max(0, scrollOffset - lines);
        followBottom = false;
    }

    public void scrollDown(int lines) {
        int maxOffset = Math.max(0, contentHeight - viewportHeight);
        scrollOffset = Math.min(maxOffset, scrollOffset + lines);
        if (scrollOffset >= maxOffset - 1) {
            followBottom = true;
        }
    }

    public void scrollToTop() {
        scrollOffset = 0;
        followBottom = false;
    }

    public void scrollToBottom() {
        scrollOffset = Math.max(0, contentHeight - viewportHeight);
        followBottom = true;
    }

    /** Call after content changes. Auto-scrolls if followBottom is true. */
    public void onContentChanged(int newContentHeight, int newViewportHeight) {
        contentHeight = newContentHeight;
        viewportHeight = newViewportHeight;
        if (followBottom) {
            scrollOffset = Math.max(0, contentHeight - viewportHeight);
        }
    }

    public int scrollOffset()   { return scrollOffset; }
    public int contentHeight()  { return contentHeight; }
    public int viewportHeight() { return viewportHeight; }
    public boolean followBottom() { return followBottom; }
}
