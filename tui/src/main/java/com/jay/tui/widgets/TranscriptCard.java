package com.jay.tui.widgets;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a single history cell as a "card": role header, content body,
 * optional thinking expand/collapse, and separator.
 *
 * <p>Mirrors Rust's transcript card rendering — each message (User,
 * Assistant, Thinking, Tool, Error) is rendered as a visual card
 * with consistent styling.
 */
public class TranscriptCard {

    /** Role of the cell. */
    public enum Role { USER, ASSISTANT, THINKING, TOOL, ERROR, SYSTEM }

    private final Role role;
    private final String content;
    private final boolean streaming;
    private final boolean folded;
    private final String toolName;
    private final String toolId;
    private final boolean toolSuccess;
    private final String headerMetadata;  // e.g. "Turn 3" or "12:34 PM"

    private TranscriptCard(Builder builder) {
        this.role = builder.role;
        this.content = builder.content;
        this.streaming = builder.streaming;
        this.folded = builder.folded;
        this.toolName = builder.toolName;
        this.toolId = builder.toolId;
        this.toolSuccess = builder.toolSuccess;
        this.headerMetadata = builder.headerMetadata;
    }

    /**
     * Render this card into terminal lines adapted to the given width.
     * Each line may include visual prefixes (like "> " for user, role
     * headers like "── Assistant ──").
     */
    public List<String> render(int width) {
        List<String> lines = new ArrayList<>();
        int contentWidth = Math.max(20, width - 4); // 2-char margin each side

        // Spacer before card
        lines.add("");

        // Role header
        String header = renderHeader();
        if (!header.isEmpty()) {
            lines.add(header);
        }

        // Content body
        if (folded) {
            lines.add("  [" + role.name().toLowerCase() + " content folded]");
        } else if (content != null) {
            for (String para : content.split("\n", -1)) {
                for (String wrapped : wrapText(para, contentWidth)) {
                    String prefix = role == Role.USER ? "> " : "  ";
                    lines.add(prefix + wrapped);
                }
            }
        }

        // Streaming indicator
        if (streaming) {
            lines.add("  \u25CF"); // ● — streaming cursor
        }

        // Spacer after card
        lines.add("");

        return lines;
    }

    private String renderHeader() {
        String prefix = switch (role) {
            case USER    -> "\u2500\u2500 You ";
            case ASSISTANT -> "\u2500\u2500 Assistant ";
            case THINKING  -> "\u2500\u2500 Thinking ";
            case TOOL      -> "\u2500\u2500 Tool: " + (toolName != null ? toolName : "unknown")
                    + " ";
            case ERROR     -> "\u2500\u2500 Error ";
            case SYSTEM    -> "\u2500\u2500 System ";
        };

        // Fill remaining width with dashes
        int maxWidth = 80;
        StringBuilder sb = new StringBuilder(prefix);
        String suffix = headerMetadata != null ? " " + headerMetadata + " " : "";
        int remaining = maxWidth - prefix.length() - suffix.length();
        if (remaining > 0) {
            sb.append("\u2500".repeat(Math.max(0, remaining)));
        }
        sb.append(suffix);

        // Tool success/failure indicator
        if (role == Role.TOOL) {
            sb.append(toolSuccess ? " \u2713" : " \u2717");
        }

        return sb.toString();
    }

    /** Simple word-wrap helper. */
    private static List<String> wrapText(String text, int maxWidth) {
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

    // ── Builder ────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Role role = Role.ASSISTANT;
        private String content = "";
        private boolean streaming;
        private boolean folded;
        private String toolName;
        private String toolId;
        private boolean toolSuccess = true;
        private String headerMetadata;

        public Builder role(Role r) { this.role = r; return this; }
        public Builder content(String c) { this.content = c; return this; }
        public Builder streaming(boolean v) { this.streaming = v; return this; }
        public Builder folded(boolean v) { this.folded = v; return this; }
        public Builder toolName(String n) { this.toolName = n; return this; }
        public Builder toolId(String id) { this.toolId = id; return this; }
        public Builder toolSuccess(boolean v) { this.toolSuccess = v; return this; }
        public Builder headerMetadata(String m) { this.headerMetadata = m; return this; }

        public TranscriptCard build() { return new TranscriptCard(this); }
    }
}
