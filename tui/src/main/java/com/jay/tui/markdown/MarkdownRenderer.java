package com.jay.tui.markdown;

import com.jay.tui.markdown.MarkdownParser.Token;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders parsed markdown tokens into a Frame buffer with word-wrapping,
 * code block backgrounds, and appropriate styles.
 */
public class MarkdownRenderer {

    private static final Style HEADER_STYLE = Style.create().fg(Color.GREEN).bold();
    private static final Style CODE_BLOCK_STYLE = Style.create().fg(Color.CYAN);
    private static final Style BULLET_STYLE = Style.create().fg(Color.WHITE);
    private static final Style HR_STYLE = Style.create().fg(Color.DARK_GRAY);
    private static final Style TEXT_STYLE = Style.create().fg(Color.WHITE);

    public record RenderedLine(String text, Style style, int indent) {
        public RenderedLine(String text, Style style) {
            this(text, style, 0);
        }
    }

    /**
     * Render tokens and return list of rendered lines. The caller should consume
     * these lines and draw them to positions determined by their own layout.
     *
     * @return lines ready to be drawn (word-wrapped, styled), plus total line count for scroll
     */
    public List<RenderedLine> render(List<Token> tokens, int maxWidth) {
        List<RenderedLine> lines = new ArrayList<>();
        int width = Math.max(20, maxWidth);

        for (Token token : tokens) {
            switch (token.type()) {
                case HEADER -> renderHeader(token, width, lines);
                case TEXT   -> renderText(token, width, lines);
                case CODE_BLOCK -> renderCodeBlock(token, width, lines);
                case BULLET -> renderBullet(token, width, lines);
                case HR     -> lines.add(new RenderedLine("\u2500".repeat(Math.min(width, 60)), HR_STYLE));
                case BLANK  -> lines.add(new RenderedLine("", TEXT_STYLE));
                default     -> lines.add(new RenderedLine(token.content(), TEXT_STYLE));
            }
        }
        return lines;
    }

    private void renderHeader(Token token, int width, List<RenderedLine> lines) {
        String prefix = "#".repeat(token.level()) + " ";
        lines.add(new RenderedLine("", TEXT_STYLE)); // blank before header
        lines.add(new RenderedLine(prefix + token.content(), HEADER_STYLE));
        if (token.level() <= 1) {
            lines.add(new RenderedLine("\u2500".repeat(Math.min(width, 60)), HR_STYLE));
        }
    }

    private void renderText(Token token, int width, List<RenderedLine> lines) {
        String content = token.content();
        for (String paragraph : content.split("\n")) {
            for (String wrapped : wrapText(paragraph, width - 2)) {
                lines.add(new RenderedLine(wrapped, TEXT_STYLE));
            }
        }
    }

    private void renderCodeBlock(Token token, int width, List<RenderedLine> lines) {
        lines.add(new RenderedLine("", TEXT_STYLE)); // blank before
        String[] codeLines = token.content().split("\n", -1);
        for (String codeLine : codeLines) {
            String display = codeLine.replace('\t', ' ');
            for (String wrapped : wrapText(display, width - 4)) {
                lines.add(new RenderedLine("  " + wrapped, CODE_BLOCK_STYLE, 2));
            }
        }
        lines.add(new RenderedLine("", TEXT_STYLE)); // blank after
    }

    private void renderBullet(Token token, int width, List<RenderedLine> lines) {
        String prefix = "  \u2022 ";
        String text = token.content();
        List<String> wrapped = wrapText(text, width - 5);
        if (wrapped.isEmpty()) {
            lines.add(new RenderedLine(prefix, BULLET_STYLE));
        } else {
            lines.add(new RenderedLine(prefix + wrapped.get(0), BULLET_STYLE));
            for (int i = 1; i < wrapped.size(); i++) {
                lines.add(new RenderedLine("    " + wrapped.get(i), BULLET_STYLE));
            }
        }
    }

    /** Simple word-wrap: split by width, try to break at word boundaries. */
    private List<String> wrapText(String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        if (text.isEmpty()) {
            result.add("");
            return result;
        }
        if (maxWidth <= 0) maxWidth = 40;

        String[] paragraphs = text.split("\n", -1);
        for (String para : paragraphs) {
            if (para.isEmpty()) {
                result.add("");
                continue;
            }
            String[] words = para.split(" ");
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
        }
        return result;
    }
}
