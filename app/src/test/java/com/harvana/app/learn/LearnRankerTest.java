package com.harvana.app.learn;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

public class LearnRankerTest {
    private static final long NOW = 1_800_000_000_000L;
    private static final long H = 3_600_000L;

    private static class P implements LearnRanker.Profile {
        Map<String, Double> cat = new HashMap<>(), aut = new HashMap<>();
        Map<String, Integer> imp = new HashMap<>();
        Set<String> opened = new HashSet<>();
        public double category(String c) { return cat.getOrDefault(c, 0.0); }
        public double author(String a) { return aut.getOrDefault(a, 0.0); }
        public int impressions(String id) { return imp.getOrDefault(id, 0); }
        public boolean opened(String id) { return opened.contains(id); }
    }

    private static LearnRanker.Item item(String id, String cat, String author, long ageHours, long views) {
        return new LearnRanker.Item(id, cat, author, NOW - ageHours * H, views);
    }

    private static List<String> ids(List<LearnRanker.Item> l) { List<String> r = new ArrayList<>(); for (LearnRanker.Item i : l) r.add(i.id); return r; }

    @Test public void coldStartIsNewestFirst() {
        List<LearnRanker.Item> in = Arrays.asList(item("old", "A", "u1", 100, 0), item("new", "B", "u2", 1, 0), item("mid", "C", "u3", 30, 0));
        assertEquals(Arrays.asList("new", "mid", "old"), ids(LearnRanker.rank(in, new P(), NOW, "me", 1)));
    }

    @Test public void interestLiftsAnOlderPost() {
        P p = new P();
        p.cat.put("Crypto", 20.0);
        List<LearnRanker.Item> in = Arrays.asList(item("fresh", "Finance", "u1", 2, 0), item("liked", "Crypto", "u2", 24, 0));
        assertEquals("liked", LearnRanker.rank(in, p, NOW, "me", 1).get(0).id);
    }

    @Test public void openedAndRepeatedPostsSinkAndOthersRise() {
        P p = new P();
        p.opened.add("read");
        p.imp.put("shown", 4);
        List<LearnRanker.Item> in = Arrays.asList(item("read", "A", "u1", 1, 0), item("shown", "B", "u2", 1, 0), item("unseen", "C", "u3", 10, 0));
        assertEquals("unseen", LearnRanker.rank(in, p, NOW, "me", 1).get(0).id);
    }

    @Test public void topIsAMixNotOneTopicInARow() {
        P p = new P();
        p.cat.put("A", 10.0);
        List<LearnRanker.Item> in = new ArrayList<>();
        for (int i = 0; i < 6; i++) in.add(item("a" + i, "A", "x" + i, 2 + i, 0));
        in.add(item("b0", "B", "y", 3, 0));
        List<LearnRanker.Item> out = LearnRanker.rank(in, p, NOW, "me", 1);
        boolean threeInARow = out.get(0).category.equals("A") && out.get(1).category.equals("A") && out.get(2).category.equals("A");
        assertFalse(threeInARow);
    }

    @Test public void sameSeedGivesTheSameOrder() {
        List<LearnRanker.Item> in = new ArrayList<>();
        for (int i = 0; i < 20; i++) in.add(item("p" + i, "C" + (i % 3), "u" + (i % 5), 5, 0));
        assertEquals(ids(LearnRanker.rank(in, new P(), NOW, "me", 7)), ids(LearnRanker.rank(in, new P(), NOW, "me", 7)));
    }
}
