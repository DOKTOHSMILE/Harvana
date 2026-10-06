package com.harvana.app.learn;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the light formatting people type into a Learn post into plain text plus a list of styled ranges. No Android here, so it can be
 * tested; {@link LearnMarkdownView} paints the ranges. What is understood:
 *
 *   # Heading, ## Heading, ### Heading
 *   **bold**  __bold__   *italic*  _italic_   ***bold italic***   ~~strikethrough~~   `code`
 *   - item  * item  + item  (nested with two spaces)      1. numbered item
 *   > quote       ---  a divider line      [text](https://link)  and bare https:// links
 *
 * Anything else is shown exactly as typed. A marker that is never closed stays visible instead of swallowing the rest of the text.
 */
public final class LearnMarkdown {
    private LearnMarkdown() {}

    public enum Kind { H1, H2, H3, BOLD, ITALIC, STRIKE, CODE, LINK, QUOTE, BULLET, RULE }

    public static final class Span {
        public final Kind kind;
        public final int start, end;
        public final String url;          // LINK only
        public final int level;           // BULLET: nesting depth, 0 first
        Span(Kind kind, int start, int end, String url, int level) { this.kind = kind; this.start = start; this.end = end; this.url = url; this.level = level; }
    }

    public static final class Result {
        public final String text;
        public final List<Span> spans;
        Result(String text, List<Span> spans) { this.text = text; this.spans = spans; }
    }

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.*?)\\s*#*\\s*$");
    private static final Pattern BULLET = Pattern.compile("^(\\s*)([-*+\\u2022])\\s+(.*)$");
    private static final Pattern NUMBERED = Pattern.compile("^(\\s*)(\\d{1,3})[.)]\\s+(.*)$");
    private static final Pattern QUOTE = Pattern.compile("^\\s*>\\s?(.*)$");
    private static final Pattern RULE = Pattern.compile("^\\s*([-*_])(\\s*\\1){2,}\\s*$");
    private static final Pattern BARE_URL = Pattern.compile("https?://[^\\s<>()\\[\\]]+[^\\s<>()\\[\\].,;:!?'\"]");

    public static Result parse(String source) {
        String src = source == null ? "" : source.replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder out = new StringBuilder();
        List<Span> spans = new ArrayList<>();
        String[] lines = src.split("\n", -1);
        int blank = 0;
        boolean wrote = false;
        for (String raw : lines) {
            String line = raw.replace("\t", "  ");
            if (line.trim().isEmpty()) { blank++; continue; }
            if (wrote) out.append(blank > 0 || HEADING.matcher(line).matches() ? "\n\n" : "\n");     // a heading always starts a new block
            blank = 0;
            wrote = true;
            int lineStart = out.length();

            Matcher m;
            if ((m = RULE.matcher(line)).matches()) {
                out.append("──────────");
                spans.add(new Span(Kind.RULE, lineStart, out.length(), null, 0));
            } else if ((m = HEADING.matcher(line)).matches()) {
                int level = Math.min(3, m.group(1).length());
                inline(m.group(2), out, spans);
                spans.add(new Span(level == 1 ? Kind.H1 : level == 2 ? Kind.H2 : Kind.H3, lineStart, out.length(), null, 0));
            } else if ((m = BULLET.matcher(line)).matches()) {
                int level = Math.min(3, m.group(1).length() / 2);
                out.append("•  ");
                inline(m.group(3), out, spans);
                spans.add(new Span(Kind.BULLET, lineStart, out.length(), null, level));
            } else if ((m = NUMBERED.matcher(line)).matches()) {
                int level = Math.min(3, m.group(1).length() / 2);
                out.append(m.group(2)).append(".  ");
                inline(m.group(3), out, spans);
                spans.add(new Span(Kind.BULLET, lineStart, out.length(), null, level));
            } else if ((m = QUOTE.matcher(line)).matches()) {
                inline(m.group(1), out, spans);
                spans.add(new Span(Kind.QUOTE, lineStart, out.length(), null, 0));
            } else {
                inline(line.trim(), out, spans);
            }
        }
        return new Result(out.toString(), spans);
    }

    /** The text with every marker removed and nothing styled, for a share message or a one-line preview. */
    public static String plain(String source) { return parse(source).text; }

    // ── inline ────────────────────────────────────────────────────

    /** Appends {@code s} to {@code out}, dropping the markers and recording a span for each styled run. */
    private static void inline(String s, StringBuilder out, List<Span> spans) {
        int i = 0, n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < n && "*_~`[]\\#>-".indexOf(s.charAt(i + 1)) >= 0) {       // \* shows a literal *
                out.append(s.charAt(i + 1));
                i += 2;
                continue;
            }
            if (c == '`') {
                int close = s.indexOf('`', i + 1);
                if (close > i + 1) {
                    int st = out.length();
                    out.append(s, i + 1, close);
                    spans.add(new Span(Kind.CODE, st, out.length(), null, 0));
                    i = close + 1;
                    continue;
                }
            }
            if (c == '[') {
                int mid = s.indexOf("](", i + 1);
                int close = mid < 0 ? -1 : s.indexOf(')', mid + 2);
                if (mid > i + 1 && close > mid + 2) {
                    String url = s.substring(mid + 2, close).trim();
                    if (url.startsWith("https://") || url.startsWith("http://")) {
                        int st = out.length();
                        inline(s.substring(i + 1, mid), out, spans);
                        spans.add(new Span(Kind.LINK, st, out.length(), url, 0));
                        i = close + 1;
                        continue;
                    }
                }
            }
            if (c == 'h' && (s.startsWith("https://", i) || s.startsWith("http://", i))) {
                Matcher u = BARE_URL.matcher(s);
                if (u.find(i) && u.start() == i) {
                    int st = out.length();
                    out.append(u.group());
                    spans.add(new Span(Kind.LINK, st, out.length(), u.group(), 0));
                    i = u.end();
                    continue;
                }
            }
            if (c == '*' || c == '_' || c == '~') {
                int run = 1;
                while (i + run < n && s.charAt(i + run) == c) run++;
                int use = c == '~' ? (run >= 2 ? 2 : 0) : Math.min(run, 3);
                // An opening marker is followed by a non-space; an underscore inside a word (snake_case) is not a marker.
                boolean opens = use > 0 && i + use < n && !Character.isWhitespace(s.charAt(i + use));
                boolean wordInside = c == '_' && i > 0 && Character.isLetterOrDigit(s.charAt(i - 1));
                if (opens && !wordInside) {
                    String marker = s.substring(i, i + use);
                    int close = findClose(s, i + use, marker, c);
                    if (close > 0) {
                        int st = out.length();
                        inline(s.substring(i + use, close), out, spans);
                        int en = out.length();
                        if (c == '~') spans.add(new Span(Kind.STRIKE, st, en, null, 0));
                        else {
                            if (use >= 2) spans.add(new Span(Kind.BOLD, st, en, null, 0));
                            if (use == 1 || use == 3) spans.add(new Span(Kind.ITALIC, st, en, null, 0));
                        }
                        i = close + use;
                        continue;
                    }
                }
                out.append(s, i, i + run);          // not formatting: show the characters as typed
                i += run;
                continue;
            }
            out.append(c);
            i++;
        }
    }

    /** The index of the closing marker, which must follow a non-space and, for underscores, not sit inside a word. */
    private static int findClose(String s, int from, String marker, char c) {
        int at = from;
        while (true) {
            at = s.indexOf(marker, at);
            if (at < 0) return -1;
            boolean afterSpace = at == 0 || Character.isWhitespace(s.charAt(at - 1));
            boolean longerRun = at + marker.length() < s.length() && s.charAt(at + marker.length()) == c && marker.length() < 3 && c != '~';
            boolean wordAfter = c == '_' && at + marker.length() < s.length() && Character.isLetterOrDigit(s.charAt(at + marker.length()));
            if (at > from && !afterSpace && !wordAfter && !longerRun) return at;
            at += longerRun ? 1 : marker.length();
        }
    }
}
