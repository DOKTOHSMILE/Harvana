package com.harvana.app.learn;

import static org.junit.Assert.*;

import org.junit.Test;

public class LearnMarkdownTest {
    private static LearnMarkdown.Span find(LearnMarkdown.Result r, LearnMarkdown.Kind k) {
        for (LearnMarkdown.Span s : r.spans) if (s.kind == k) return s;
        return null;
    }
    private static String of(LearnMarkdown.Result r, LearnMarkdown.Span s) { return r.text.substring(s.start, s.end); }

    @Test public void headingsAreStrippedAndStyled() {
        LearnMarkdown.Result r = LearnMarkdown.parse("## Midnight Night-Vision\nBody");
        assertEquals("Midnight Night-Vision\nBody", r.text);
        LearnMarkdown.Span h = find(r, LearnMarkdown.Kind.H2);
        assertEquals("Midnight Night-Vision", of(r, h));
    }

    @Test public void boldItalicStrikeCode() {
        LearnMarkdown.Result r = LearnMarkdown.parse("a **bold** b *it* c ~~gone~~ d `x=1`");
        assertEquals("a bold b it c gone d x=1", r.text);
        assertEquals("bold", of(r, find(r, LearnMarkdown.Kind.BOLD)));
        assertEquals("it", of(r, find(r, LearnMarkdown.Kind.ITALIC)));
        assertEquals("gone", of(r, find(r, LearnMarkdown.Kind.STRIKE)));
        assertEquals("x=1", of(r, find(r, LearnMarkdown.Kind.CODE)));
    }

    @Test public void tripleIsBoldAndItalic() {
        LearnMarkdown.Result r = LearnMarkdown.parse("***both***");
        assertEquals("both", r.text);
        assertNotNull(find(r, LearnMarkdown.Kind.BOLD));
        assertNotNull(find(r, LearnMarkdown.Kind.ITALIC));
    }

    @Test public void bulletsAndNumbers() {
        LearnMarkdown.Result r = LearnMarkdown.parse(" * **The Midnight Grind:** It symbolizes\n * second\n1. one\n  - nested");
        assertTrue(r.text.startsWith("•  The Midnight Grind: It symbolizes\n•  second\n1.  one\n•"));
        assertEquals("The Midnight Grind:", of(r, find(r, LearnMarkdown.Kind.BOLD)));
    }

    @Test public void unclosedMarkersStayVisible() {
        assertEquals("2 * 3 = 6 and **oops", LearnMarkdown.plain("2 * 3 = 6 and **oops"));
        assertEquals("snake_case_name", LearnMarkdown.plain("snake_case_name"));
    }

    @Test public void linksBothKinds() {
        LearnMarkdown.Result r = LearnMarkdown.parse("see [Harvana](https://harvannaprotocol.com) or https://example.com/a.");
        assertEquals("see Harvana or https://example.com/a.", r.text);
        assertEquals("https://harvannaprotocol.com", find(r, LearnMarkdown.Kind.LINK).url);
    }

    @Test public void blankLinesAreKeptAsParagraphBreaks() {
        assertEquals("one\n\ntwo", LearnMarkdown.plain("one\n\n\n\ntwo"));
        assertEquals("one\ntwo", LearnMarkdown.plain("one\ntwo"));
    }

    @Test public void quoteAndRuleAndEscape() {
        LearnMarkdown.Result r = LearnMarkdown.parse("> wise\n---\n\\*not italic\\*");
        assertEquals("wise", of(r, find(r, LearnMarkdown.Kind.QUOTE)));
        assertNotNull(find(r, LearnMarkdown.Kind.RULE));
        assertTrue(r.text.endsWith("*not italic*"));
    }
}
