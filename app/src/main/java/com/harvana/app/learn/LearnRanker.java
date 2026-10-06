package com.harvana.app.learn;

import java.util.ArrayList;
import java.util.List;

/**
 * Orders the Learn feed for one reader. Pure arithmetic, no Android, so it can be tested.
 *
 * A post's score is built from what is known about the post and about the reader:
 *   freshness   newer posts score higher, halving every 36 hours
 *   interest    how much the reader has engaged with this category and this author (the profile decays with time)
 *   popularity  a gentle lift for posts other people opened, on a log scale so one huge post cannot bury the rest
 *   repeats     a post already shown is pushed down each time, and one already opened far more
 * Then the list is picked one place at a time, and a post whose category or author was just picked is held back a little, so the
 * top of the feed is a mix instead of one topic in a row. A small steady per-reader wobble breaks ties, so two ties do not always
 * resolve the same way. The feed order is computed once when it loads, not every time Firestore reports a change.
 */
public final class LearnRanker {
    private LearnRanker() {}

    /** What the app has learned about the reader. */
    public interface Profile {
        /** Engagement with a category, 0 when nothing yet; grows with opens, fires, comments, shares. */
        double category(String category);
        double author(String authorUid);
        /** How many times a post has been shown to the reader. */
        int impressions(String postId);
        boolean opened(String postId);
    }

    public static final class Item {
        public final String id, category, author;
        public final long createdAt, views;
        public double score;
        public Item(String id, String category, String author, long createdAt, long views) {
            this.id = id == null ? "" : id; this.category = category == null ? "" : category; this.author = author == null ? "" : author;
            this.createdAt = createdAt; this.views = views;
        }
    }

    static final double FRESH_HALF_LIFE_HOURS = 36.0;

    public static double score(Item it, Profile p, long now, String me, long seed) {
        double ageH = Math.max(0, now - it.createdAt) / 3_600_000.0;
        double fresh = Math.pow(0.5, ageH / FRESH_HALF_LIFE_HOURS);                       // 1 .. 0
        double pop = Math.min(1.0, Math.log10(1.0 + Math.max(0, it.views)) / 3.0);        // 1000 views is full
        double cat = Math.tanh(p.category(it.category) / 6.0);                             // 0 .. 1
        double aut = Math.tanh(p.author(it.author) / 6.0);

        double s = 3.0 * fresh + 1.0 * pop + 3.0 * cat + 2.0 * aut;
        int seen = Math.min(4, p.impressions(it.id));
        s *= Math.pow(0.75, seen);                                                         // shown before: lower each time
        if (p.opened(it.id)) s *= 0.3;                                                     // already read: well down
        if (me != null && !me.isEmpty() && me.equals(it.author)) s *= 0.7;                 // your own posts: you know them
        s += wobble(it.id, seed) * 0.15;
        return s;
    }

    /** Best first. Items are not modified except for their score. */
    public static List<Item> rank(List<Item> items, Profile p, long now, String me, long seed) {
        List<Item> pool = new ArrayList<>(items);
        for (Item it : pool) it.score = score(it, p, now, me, seed);

        List<Item> out = new ArrayList<>(pool.size());
        String last1Cat = null, last2Cat = null, lastAuthor = null;
        while (!pool.isEmpty()) {
            int best = 0;
            double bestAdjusted = -1e9;
            for (int i = 0; i < pool.size(); i++) {
                Item it = pool.get(i);
                double adjusted = it.score;
                if (!it.category.isEmpty() && it.category.equals(last1Cat) && it.category.equals(last2Cat)) adjusted *= 0.4;   // third in a row
                else if (!it.category.isEmpty() && it.category.equals(last1Cat)) adjusted *= 0.88;
                if (!it.author.isEmpty() && it.author.equals(lastAuthor)) adjusted *= 0.8;
                if (adjusted > bestAdjusted) { bestAdjusted = adjusted; best = i; }
            }
            Item pick = pool.remove(best);
            out.add(pick);
            last2Cat = last1Cat; last1Cat = pick.category; lastAuthor = pick.author;
        }
        return out;
    }

    /** A steady value in -1 .. 1 for this post and this reader. */
    static double wobble(String id, long seed) {
        long h = id.hashCode() * 0x9E3779B97F4A7C15L ^ seed * 0xC2B2AE3D27D4EB4FL;
        h ^= (h >>> 29); h *= 0xBF58476D1CE4E5B9L; h ^= (h >>> 32);
        return ((h & 0xFFFF) / 32767.5) - 1.0;
    }
}
