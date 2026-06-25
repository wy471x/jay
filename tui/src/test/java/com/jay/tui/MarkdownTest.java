package com.jay.tui;

import com.jay.tui.markdown.MarkdownParser;
import com.jay.tui.markdown.MarkdownRenderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownTest {

    // ── MarkdownParser ─────────────────────────────────────────────────

    @Nested
    class MarkdownParserTests {

        private final MarkdownParser parser = new MarkdownParser();

        @Test
        void emptyAndNullInput() {
            assertTrue(parser.parse(null).isEmpty());
            assertTrue(parser.parse("").isEmpty());
        }

        @Test
        void plainText() {
            var tokens = parser.parse("Hello world");
            assertEquals(1, tokens.size());
            assertEquals(MarkdownParser.TokenType.TEXT, tokens.get(0).type());
            assertEquals("Hello world", tokens.get(0).content());
        }

        @Test
        void headerLevels() {
            var tokens = parser.parse("# H1\n## H2\n### H3\n#### H4");
            assertEquals(4, tokens.size());
            assertEquals(MarkdownParser.TokenType.HEADER, tokens.get(0).type());
            assertEquals("H1", tokens.get(0).content());
            assertEquals(1, tokens.get(0).level());
            assertEquals(MarkdownParser.TokenType.HEADER, tokens.get(1).type());
            assertEquals("H2", tokens.get(1).content());
            assertEquals(2, tokens.get(1).level());
            assertEquals(3, tokens.get(2).level());
            assertEquals(4, tokens.get(3).level());
        }

        @Test
        void bulletLists() {
            var tokens = parser.parse("- item1\n* item2\n+ item3");
            assertEquals(3, tokens.size());
            tokens.forEach(t -> assertEquals(MarkdownParser.TokenType.BULLET, t.type()));
            assertEquals("item1", tokens.get(0).content());
            assertEquals("item2", tokens.get(1).content());
            assertEquals("item3", tokens.get(2).content());
        }

        @Test
        void horizontalRule() {
            var tokens = parser.parse("---");
            assertEquals(1, tokens.size());
            assertEquals(MarkdownParser.TokenType.HR, tokens.get(0).type());
        }

        @Test
        void horizontalRuleVariants() {
            assertEquals(MarkdownParser.TokenType.HR, parser.parse("___").get(0).type());
            assertEquals(MarkdownParser.TokenType.HR, parser.parse("***").get(0).type());
            assertEquals(MarkdownParser.TokenType.HR, parser.parse("------").get(0).type());
        }

        @Test
        void blankLines() {
            var tokens = parser.parse("hello\n\nworld");
            assertEquals(3, tokens.size());
            assertEquals(MarkdownParser.TokenType.TEXT, tokens.get(0).type());
            assertEquals(MarkdownParser.TokenType.BLANK, tokens.get(1).type());
            assertEquals(MarkdownParser.TokenType.TEXT, tokens.get(2).type());
        }

        @Test
        void codeBlock() {
            var tokens = parser.parse("```\nline1\nline2\n```");
            assertEquals(1, tokens.size());
            assertEquals(MarkdownParser.TokenType.CODE_BLOCK, tokens.get(0).type());
            assertEquals("line1\nline2", tokens.get(0).content());
        }

        @Test
        void codeBlockPreservesLeadingWhitespace() {
            var tokens = parser.parse("```\n  indented\n  code\n```");
            assertEquals(1, tokens.size());
            assertEquals(MarkdownParser.TokenType.CODE_BLOCK, tokens.get(0).type());
            assertTrue(tokens.get(0).content().contains("indented"));
        }

        @Test
        void unclosedCodeBlockEmittedAtEnd() {
            var tokens = parser.parse("```\nfoo\nbar");
            assertEquals(1, tokens.size());
            assertEquals(MarkdownParser.TokenType.CODE_BLOCK, tokens.get(0).type());
            assertTrue(tokens.get(0).content().contains("foo"));
            assertTrue(tokens.get(0).content().contains("bar"));
        }

        @Test
        void mixedContent() {
            var tokens = parser.parse("# Title\n\nSome text\n\n- bullet\n\n```\ncode\n```");
            assertEquals(7, tokens.size());
            assertEquals(MarkdownParser.TokenType.HEADER, tokens.get(0).type());
            assertEquals(MarkdownParser.TokenType.BLANK, tokens.get(1).type());
            assertEquals(MarkdownParser.TokenType.TEXT, tokens.get(2).type());
            assertEquals(MarkdownParser.TokenType.BLANK, tokens.get(3).type());
            assertEquals(MarkdownParser.TokenType.BULLET, tokens.get(4).type());
            assertEquals(MarkdownParser.TokenType.BLANK, tokens.get(5).type());
            assertEquals(MarkdownParser.TokenType.CODE_BLOCK, tokens.get(6).type());
        }
    }

    // ── MarkdownRenderer ───────────────────────────────────────────────

    @Nested
    class MarkdownRendererTests {

        private final MarkdownParser parser = new MarkdownParser();
        private final MarkdownRenderer renderer = new MarkdownRenderer();

        @Test
        void rendersHeader() {
            var tokens = parser.parse("# Hello");
            var lines = renderer.render(tokens, 80);
            assertTrue(lines.size() >= 2);
            // First line is blank before header
            var headerLine = lines.stream()
                    .filter(l -> l.text().contains("Hello"))
                    .findFirst();
            assertTrue(headerLine.isPresent());
            assertTrue(headerLine.get().text().startsWith("#"));
        }

        @Test
        void rendersText() {
            var tokens = parser.parse("plain text");
            var lines = renderer.render(tokens, 80);
            assertEquals(1, lines.size());
            assertEquals("plain text", lines.get(0).text());
        }

        @Test
        void rendersCodeBlock() {
            var tokens = parser.parse("```\ncode\n```");
            var lines = renderer.render(tokens, 80);
            assertTrue(lines.size() >= 3); // blank + code + blank
            var codeLine = lines.stream()
                    .filter(l -> l.text().contains("code"))
                    .findFirst();
            assertTrue(codeLine.isPresent());
            assertTrue(codeLine.get().text().startsWith("  "));
            assertEquals(2, codeLine.get().indent());
        }

        @Test
        void rendersBullet() {
            var tokens = parser.parse("- item");
            var lines = renderer.render(tokens, 80);
            assertEquals(1, lines.size());
            assertTrue(lines.get(0).text().contains("\u2022"));
            assertTrue(lines.get(0).text().contains("item"));
        }

        @Test
        void rendersHorizontalRule() {
            var tokens = parser.parse("---");
            var lines = renderer.render(tokens, 80);
            assertEquals(1, lines.size());
            assertTrue(lines.get(0).text().contains("\u2500")); // box-drawing dash
        }

        @Test
        void rendersBlankLine() {
            var tokens = parser.parse("");
            var lines = renderer.render(tokens, 80);
            // empty input -> no tokens -> empty lines
            assertTrue(lines.isEmpty());
        }

        @Test
        void wordWrapLongText() {
            String longText = "a ".repeat(50).trim();
            var tokens = parser.parse(longText);
            var lines = renderer.render(tokens, 40);
            assertTrue(lines.size() > 1);
            lines.forEach(l -> assertTrue(l.text().length() <= 40, "Line too long: " + l.text()));
        }

        @Test
        void minimumWidthEnforced() {
            var tokens = parser.parse("text");
            var lines = renderer.render(tokens, 5); // below minimum
            // Should still render without crashing
            assertFalse(lines.isEmpty());
        }

        @Test
        void rendersH1WithSeparator() {
            var tokens = parser.parse("# H1");
            var lines = renderer.render(tokens, 80);
            // Should have blank, header, separator line
            assertTrue(lines.size() >= 3);
            var separator = lines.get(2);
            assertTrue(separator.text().contains("\u2500"));
        }
    }
}
