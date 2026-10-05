package com.harvana.app.learn;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LearnRulesTest {
    @Test public void wordCountSplitsOnWhitespaceAndSkipsBlanks() {
        assertEquals(5, LearnRules.wordCount("one two", "  ", null, "three  four\nfive"));
        assertEquals(0, LearnRules.wordCount((String) null));
        assertEquals(LearnRules.MAX_WORDS, LearnRules.wordCount("w ".repeat(10_000)));
    }

    @Test public void requiredDwellMatchesTheNodeRule() {
        assertEquals(8_000, LearnRules.requiredDwellMs(0));
        assertEquals(8_000, LearnRules.requiredDwellMs(50));
        assertEquals(24_000, LearnRules.requiredDwellMs(400));
        assertEquals(LearnRules.requiredDwellMs(LearnRules.MAX_WORDS), LearnRules.requiredDwellMs(99_999));
    }

    @Test public void countsAreCompact() {
        assertEquals("0", LearnRules.compactCount(-3));
        assertEquals("999", LearnRules.compactCount(999));
        assertEquals("1K", LearnRules.compactCount(1_000));
        assertEquals("1.2K", LearnRules.compactCount(1_234));
        assertEquals("15K", LearnRules.compactCount(15_400));
        assertEquals("1.1M", LearnRules.compactCount(1_100_000));
    }

    @Test public void timeAgoPicksTheRightUnit() {
        long now = 1_800_000_000_000L;
        assertEquals("", LearnRules.timeAgo(0, now));
        assertEquals("now", LearnRules.timeAgo(now - 30_000, now));
        assertEquals("5m ago", LearnRules.timeAgo(now - 5 * 60_000L, now));
        assertEquals("3h ago", LearnRules.timeAgo(now - 3 * 3_600_000L, now));
        assertEquals("1d ago", LearnRules.timeAgo(now - 86_400_000L, now));
        assertEquals("2w ago", LearnRules.timeAgo(now - 15 * 86_400_000L, now));
        assertEquals("4mo ago", LearnRules.timeAgo(now - 130 * 86_400_000L, now));
        assertEquals("2y ago", LearnRules.timeAgo(now - 800 * 86_400_000L, now));
    }
}
