package com.jay.tui.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jay.tui.client.ContentBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks in-flight SSE stream state: current content block kind,
 * tool-use input accumulation, fake wrapper filtering.
 *
 * <p>Mirrors Rust {@code streaming.rs} — local state during one
 * {@code handle_deepseek_turn} invocation.
 */
public class StreamState {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ContentBlock.Kind currentKind = ContentBlock.Kind.NONE;
    private int currentIndex;
    private final StringBuilder currentText = new StringBuilder();
    private final StringBuilder currentThinking = new StringBuilder();
    private String currentSignature;
    private final List<ToolUseState> toolUses = new ArrayList<>();
    private boolean anyContentReceived;

    /** Tool call being accumulated during streaming. */
    public record ToolUseState(String id, String name, StringBuilder inputBuffer) {
        public JsonNode finalizeInput() {
            String raw = inputBuffer.toString();
            if (raw.isEmpty()) return MAPPER.createObjectNode();
            try {
                return MAPPER.readTree(raw);
            } catch (Exception e) {
                // JSON repair: try wrapping in quotes for bare strings
                try {
                    return MAPPER.readTree("\"" + raw + "\"");
                } catch (Exception e2) {
                    return MAPPER.createObjectNode().put("_raw", raw);
                }
            }
        }
    }

    // ── Block transitions ──────────────────────────────────────────

    /** Called when a new ContentBlock starts in the SSE stream. */
    public void startBlock(ContentBlock.Kind kind, int index) {
        this.currentKind = kind;
        this.currentIndex = index;
        switch (kind) {
            case TEXT -> currentText.setLength(0);
            case THINKING -> { currentThinking.setLength(0); currentSignature = null; }
            case TOOL_USE -> {} // ToolUseState created externally
        }
    }

    /** Append a text delta. */
    public void appendText(String delta) {
        anyContentReceived = true;
        currentText.append(delta);
    }

    /** Append a thinking delta. */
    public void appendThinking(String delta) {
        anyContentReceived = true;
        currentThinking.append(delta);
    }

    /** Set the thinking signature. */
    public void setSignature(String signature) {
        this.currentSignature = signature;
    }

    /** Add a tool-use that was detected during streaming. */
    public ToolUseState addToolUse(String id, String name) {
        var state = new ToolUseState(id, name, new StringBuilder());
        toolUses.add(state);
        return state;
    }

    /** Append to the current tool's input buffer. */
    public void appendToolInput(String json) {
        anyContentReceived = true;
        if (!toolUses.isEmpty()) {
            toolUses.get(toolUses.size() - 1).inputBuffer().append(json);
        }
    }

    // ── Finalization ────────────────────────────────────────────────

    /** Build the current text ContentBlock. */
    public ContentBlock.Text finalizeText() {
        return new ContentBlock.Text(currentText.toString());
    }

    /** Build the current thinking ContentBlock. */
    public ContentBlock.Thinking finalizeThinking() {
        return new ContentBlock.Thinking(
                currentThinking.toString(), currentSignature);
    }

    /** Finalize all tool-use blocks. */
    public List<ContentBlock.ToolUse> finalizeToolUses() {
        return toolUses.stream()
                .map(tu -> new ContentBlock.ToolUse(
                        tu.id(), tu.name(), tu.finalizeInput()))
                .toList();
    }

    /** Build content blocks accumulated so far. */
    public List<ContentBlock> buildContentBlocks() {
        var blocks = new ArrayList<ContentBlock>();
        if (!currentText.isEmpty()) {
            blocks.add(finalizeText());
        }
        if (!currentThinking.isEmpty()) {
            blocks.add(finalizeThinking());
        }
        blocks.addAll(finalizeToolUses());
        return blocks;
    }

    // ── Fake wrapper detection ──────────────────────────────────────

    private static final String[] FAKE_WRAPPER_MARKERS = {
            "[TOOL_CALL]", "<codewhale:tool_call", "<tool_call",
            "<invoke ", "<function_calls>"
    };

    /** Check if text looks like a forged tool-call wrapper. */
    public static boolean containsFakeWrapper(String text) {
        for (var marker : FAKE_WRAPPER_MARKERS) {
            if (text.contains(marker)) return true;
        }
        return false;
    }

    /**
     * Filter out tool-call wrapper fragments from streaming text.
     * Returns the cleaned text.
     */
    public static String filterToolCallDelta(String delta, boolean inWrapper) {
        if (inWrapper) {
            // Accumulate until we see a closing marker
            for (var end : new String[]{"[/TOOL_CALL]", "</codewhale:tool_call>",
                    "</tool_call>", "</invoke>", "</function_calls>"}) {
                if (delta.contains(end)) return delta.substring(delta.indexOf(end) + end.length());
            }
            return "";
        }
        // Check if this delta opens a fake wrapper
        for (var start : FAKE_WRAPPER_MARKERS) {
            int idx = delta.indexOf(start);
            if (idx >= 0) return delta.substring(0, idx);
        }
        return delta;
    }

    // ── Getters ──────────────────────────────────────────────────────

    public ContentBlock.Kind currentKind() { return currentKind; }
    public String currentText() { return currentText.toString(); }
    public boolean anyContentReceived() { return anyContentReceived; }
    public List<ToolUseState> toolUses() { return List.copyOf(toolUses); }

    public void reset() {
        currentKind = ContentBlock.Kind.NONE;
        currentIndex = 0;
        currentText.setLength(0);
        currentThinking.setLength(0);
        currentSignature = null;
        toolUses.clear();
        anyContentReceived = false;
    }
}
