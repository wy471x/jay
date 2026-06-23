package com.jay.tui.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Simple markdown tokenizer. Converts raw markdown text into a flat
 * list of styled tokens for terminal rendering.
 */
public class MarkdownParser {

    private static final Pattern HEADER_PATTERN = Pattern.compile("^(#{1,4})\\s+(.+)$");
    private static final Pattern BULLET_PATTERN = Pattern.compile("^\\s*[-*+]\\s+(.+)$");
    private static final Pattern HR_PATTERN = Pattern.compile("^\\s*(_{3,}|\\*{3,}|-{3,})\\s*$");

    public enum TokenType {
        HEADER, BOLD, CODE_BLOCK, INLINE_CODE, BULLET, HR, TEXT, BLANK
    }

    public record Token(TokenType type, String content, int level) {
        public Token(TokenType type, String content) {
            this(type, content, 0);
        }
    }

    private boolean inCodeBlock;
    private final StringBuilder codeBuf = new StringBuilder();

    public List<Token> parse(String text) {
        List<Token> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) return tokens;

        String[] rawLines = text.split("\n", -1);
        for (String line : rawLines) {
            String trimmed = line.stripTrailing();

            // Code block fences
            if (trimmed.strip().startsWith("```")) {
                if (!inCodeBlock) {
                    inCodeBlock = true;
                    codeBuf.setLength(0);
                    continue;
                } else {
                    inCodeBlock = false;
                    if (!codeBuf.isEmpty()) {
                        tokens.add(new Token(TokenType.CODE_BLOCK, codeBuf.toString().stripTrailing()));
                        codeBuf.setLength(0);
                    }
                    continue;
                }
            }

            if (inCodeBlock) {
                if (!codeBuf.isEmpty()) codeBuf.append('\n');
                codeBuf.append(line);
                continue;
            }

            if (trimmed.isEmpty()) {
                tokens.add(new Token(TokenType.BLANK, ""));
                continue;
            }

            // HR
            if (HR_PATTERN.matcher(trimmed).matches()) {
                tokens.add(new Token(TokenType.HR, ""));
                continue;
            }

            // Header
            var headerMatcher = HEADER_PATTERN.matcher(trimmed);
            if (headerMatcher.matches()) {
                int level = headerMatcher.group(1).length();
                tokens.add(new Token(TokenType.HEADER, headerMatcher.group(2), level));
                continue;
            }

            // Bullet
            var bulletMatcher = BULLET_PATTERN.matcher(trimmed);
            if (bulletMatcher.matches()) {
                tokens.add(new Token(TokenType.BULLET, bulletMatcher.group(1)));
                continue;
            }

            tokens.add(new Token(TokenType.TEXT, trimmed));
        }

        // Unclosed code block
        if (inCodeBlock && !codeBuf.isEmpty()) {
            tokens.add(new Token(TokenType.CODE_BLOCK, codeBuf.toString().stripTrailing()));
        }

        return tokens;
    }
}
