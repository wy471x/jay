package com.jay.tui.state;

/**
 * Flat line-offset scroll model with TAIL_SENTINEL for "stick to bottom."
 * Mirrors Rust {@code TranscriptScroll}.
 *
 * <p>TAIL_SENTINEL semantics: when offset is {@code TAIL_SENTINEL}, the
 * viewport always shows the last N lines (auto-scroll). When the user
 * scrolls up, offset becomes a concrete line number. Scroll-to-bottom
 * re-enables TAIL_SENTINEL.
 */
public class ScrollState {

    /** Magic value meaning "stuck to live tail" (auto-scroll). */
    public static final int TAIL_SENTINEL = Integer.MAX_VALUE;

    private int offset;

    private ScrollState(int offset) {
        this.offset = offset;
    }

    /** Create pinned to bottom (auto-scroll). */
    public static ScrollState toBottom() {
        return new ScrollState(TAIL_SENTINEL);
    }

    /** Create at a specific offset. */
    public static ScrollState at(int offset) {
        return new ScrollState(Math.max(0, offset));
    }

    public boolean isAtTail() {
        return offset == TAIL_SENTINEL;
    }

    public int offset() {
        return offset;
    }

    /**
     * Apply a scroll delta. Positive = scroll down, negative = up.
     * Returns a new ScrollState (immutable).
     */
    public ScrollState scrolledBy(int delta, int totalLines, int visibleLines) {
        if (delta == 0 || totalLines <= visibleLines) {
            return toBottom();
        }

        int maxStart = Math.max(0, totalLines - visibleLines);
        int currentTop = isAtTail() ? maxStart
                : Math.min(offset, maxStart);

        int newTop = delta < 0
                ? Math.max(0, currentTop + delta)       // scroll up
                : Math.min(maxStart, currentTop + delta); // scroll down

        return newTop >= maxStart ? toBottom()
                : new ScrollState(newTop);
    }

    /**
     * Resolve the effective offset given the current content size.
     * Returns (newScrollState, effectiveOffset).
     */
    public ResolvedScroll resolve(int totalLines, int visibleLines) {
        int maxStart = Math.max(0, totalLines - visibleLines);
        int effectiveOffset;
        boolean wasTail = isAtTail();

        if (isAtTail()) {
            effectiveOffset = maxStart;
        } else {
            effectiveOffset = Math.min(offset, maxStart);
        }

        // Collapse to tail if at bottom
        if (effectiveOffset >= maxStart) {
            return new ResolvedScroll(toBottom(), effectiveOffset);
        }

        return new ResolvedScroll(this, effectiveOffset);
    }

    /** Pin to a specific line number. */
    public ScrollState anchorAt(int lineIndex) {
        return new ScrollState(Math.max(0, lineIndex));
    }

    /** Scroll to bottom (re-enable auto-scroll). */
    public ScrollState scrollToBottom() {
        return toBottom();
    }

    /** Scroll to top. */
    public ScrollState scrollToTop() {
        return new ScrollState(0);
    }

    /** Page up by visibleLines. */
    public ScrollState pageUp(int visibleLines) {
        int newOffset = isAtTail() ? Math.max(0, offset - visibleLines)
                : Math.max(0, offset - visibleLines);
        return new ScrollState(newOffset);
    }

    /** Page down by visibleLines. */
    public ScrollState pageDown(int visibleLines) {
        int newOffset = isAtTail() ? offset
                : offset + visibleLines;
        return new ScrollState(newOffset);
    }

    /** Resolved scroll: concrete state + effective offset for rendering. */
    public record ResolvedScroll(ScrollState state, int effectiveOffset) {}
}
