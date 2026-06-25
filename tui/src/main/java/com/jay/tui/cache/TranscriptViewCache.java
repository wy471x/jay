package com.jay.tui.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Incremental per-cell line-array cache for transcript rendering.
 * Mirrors Rust {@code TranscriptViewCache}.
 *
 * <p>Performance core: only re-renders cells whose content (tracked by
 * revision) or layout (width/options) has changed. For streaming turns,
 * only the last cell changes — all others are reused from cache.
 *
 * <p>Architecture:
 * <pre>
 *   cells[0..N] (List&lt;HistoryCell&gt;) → perCell[0..N] (CachedCell)
 *     → flattenFrom(dirtyCell) → flatLines (List&lt;String&gt;)
 * </pre>
 */
public class TranscriptViewCache {

    private int width;
    private final List<CachedCell> perCell = new ArrayList<>();
    private final List<String> flatLines = new ArrayList<>();
    private final List<LineMeta> flatMeta = new ArrayList<>();
    private Set<Integer> foldedCells = Set.of();

    /** One rendered cell in the cache. */
    public record CachedCell(
            long revision,
            List<String> lines,
            boolean isEmpty,
            boolean isToolGroupable
    ) {}

    /** Per-line metadata identifying source cell and line index. */
    public sealed interface LineMeta {
        record CellLine(int cellIndex, int lineInCell) implements LineMeta {}
        record Spacer() implements LineMeta {}
    }

    /**
     * Incrementally update the cache. Only re-renders cells whose revision
     * changed or whose layout (width/folded) changed.
     */
    public void ensure(
            List<?> cells,
            List<Long> revisions,
            int newWidth,
            Set<Integer> newFolded
    ) {
        boolean layoutChanged = this.width != newWidth
                || !this.foldedCells.equals(newFolded);

        if (layoutChanged) {
            perCell.clear();
        }
        this.width = newWidth;
        this.foldedCells = newFolded;

        boolean anyDirty = layoutChanged || perCell.size() != cells.size();
        int firstDirty = cells.size();

        for (int i = 0; i < cells.size(); i++) {
            long currentRev = revisions.get(i);

            if (!layoutChanged && i < perCell.size()
                    && perCell.get(i).revision == currentRev) {
                // Cache hit — reuse
                continue;
            }

            anyDirty = true;
            if (firstDirty > i) firstDirty = i;

            // Render this cell (to List<String> lines)
            List<String> rendered = renderCell(cells.get(i), newWidth);
            boolean isToolGroupable = isToolCell(cells.get(i));

            // Extend perCell list if needed
            while (perCell.size() <= i) {
                perCell.add(new CachedCell(-1, List.of(), true, false));
            }
            perCell.set(i, new CachedCell(currentRev, rendered,
                    rendered.isEmpty(), isToolGroupable));
        }

        if (anyDirty) {
            flattenFrom(Math.max(firstDirty, 0));
        }
    }

    /**
     * Rebuild flat line buffer from a given cell index onward.
     * Only the suffix is rebuilt — all earlier lines are preserved.
     */
    private void flattenFrom(int firstCell) {
        // Find truncation point in flat arrays
        int truncateAt = findTruncationPoint(firstCell);

        // Truncate
        if (truncateAt < flatLines.size()) {
            flatLines.subList(truncateAt, flatLines.size()).clear();
        }
        if (truncateAt < flatMeta.size()) {
            flatMeta.subList(truncateAt, flatMeta.size()).clear();
        }

        // Rebuild from firstCell onward
        for (int ci = firstCell; ci < perCell.size(); ci++) {
            var entry = perCell.get(ci);
            if (entry == null || entry.isEmpty) continue;

            // Add spacer between cells (except before first visible cell)
            if (ci > firstCell && needsSpacer(ci)) {
                flatLines.add("");
                flatMeta.add(new LineMeta.Spacer());
            }

            for (int li = 0; li < entry.lines.size(); li++) {
                flatLines.add(entry.lines.get(li));
                flatMeta.add(new LineMeta.CellLine(ci, li));
            }
        }
    }

    private int findTruncationPoint(int firstCell) {
        for (int i = 0; i < flatMeta.size(); i++) {
            if (flatMeta.get(i) instanceof LineMeta.CellLine(var ci, var li)
                    && ci >= firstCell) {
                return i;
            }
        }
        return flatMeta.size(); // truncate everything
    }

    private boolean needsSpacer(int cellIndex) {
        if (cellIndex <= 0 || cellIndex >= perCell.size()) return false;
        var prev = perCell.get(cellIndex - 1);
        var curr = perCell.get(cellIndex);
        if (prev == null || curr == null) return false;
        // Adjacent groupable tool cells get 0 spacers
        if (prev.isToolGroupable && curr.isToolGroupable) return false;
        return true;
    }

    // ── Rendering hook (overridable) ──────────────────────────────────

    /**
     * Render a single cell into lines. Override for custom rendering.
     * Default: toString each line of the cell.
     */
    protected List<String> renderCell(Object cell, int width) {
        if (cell == null) return List.of();
        String text = cell.toString();
        if (text.isEmpty()) return List.of();
        return List.of(text.split("\n", -1));
    }

    /** Whether a cell is a tool output (for rail grouping). */
    protected boolean isToolCell(Object cell) {
        return false;
    }

    // ── Public API ──────────────────────────────────────────────────────

    public int totalLines() { return flatLines.size(); }
    public List<String> lines() { return Collections.unmodifiableList(flatLines); }
    public List<LineMeta> lineMeta() { return Collections.unmodifiableList(flatMeta); }
    public int width() { return width; }

    /** Invalidate all caches. */
    public void invalidate() {
        perCell.clear();
        flatLines.clear();
        flatMeta.clear();
    }
}
