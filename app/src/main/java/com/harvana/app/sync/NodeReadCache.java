package com.harvana.app.sync;

import android.content.Context;
import android.util.LruCache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Every answer the node gives to a read (GET) is kept on the phone, like WhatsApp keeps chats: a screen opened
 * offline, or on a slow network, shows the last answer instead of an error, and a screen can paint from it at once
 * while the fresh answer loads (HarvanaNodeClient.nodeGetFast).
 *
 * Kept: balances, history, groups, members, lines, prices, profiles' trust, staking, P2P listings. Never kept, because
 * a stale copy would be wrong rather than old: nonces, fee estimates, mining jobs and sign-in challenges.
 * Stored in the app's private files (node_reads/), one file per path, with an in-memory copy of the recent ones.
 */
public final class NodeReadCache {
    private NodeReadCache() {}

    private static volatile File dir;
    private static final LruCache<String, Entry> MEM = new LruCache<>(256);

    /** A saved answer and when it was saved. */
    public static final class Entry {
        public final String body;
        public final long savedAt;
        Entry(String body, long savedAt) { this.body = body; this.savedAt = savedAt; }
    }

    public static void init(Context c) {
        if (c == null || dir != null) return;
        File d = new File(c.getApplicationContext().getFilesDir(), "node_reads");
        if (!d.exists() && !d.mkdirs()) return;
        dir = d;
    }

    /** False for reads whose old answer would be wrong to show (nonces, fees, mining work, sign-in). */
    public static boolean cacheable(String path) {
        if (path == null) return false;
        String p = path.toLowerCase(java.util.Locale.US);
        return !(p.contains("/nonce") || p.contains("estimate-fee") || p.contains("/mining/") || p.contains("/auth")
                || p.contains("/challenge") || p.contains("/explorer/live") || p.contains("getjob"));
    }

    public static void put(String path, String body) {
        if (!cacheable(path) || body == null || body.isEmpty()) return;
        Entry e = new Entry(body, System.currentTimeMillis());
        MEM.put(path, e);
        File d = dir;
        if (d == null) return;
        try (FileOutputStream out = new FileOutputStream(new File(d, name(path)))) {
            out.write((e.savedAt + "\n" + body).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) { }
    }

    /** The last answer for this path, or null. */
    public static Entry get(String path) {
        if (!cacheable(path)) return null;
        Entry e = MEM.get(path);
        if (e != null) return e;
        File d = dir;
        if (d == null) return null;
        File f = new File(d, name(path));
        if (!f.exists()) return null;
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] all = new byte[(int) f.length()];
            int n = 0;
            while (n < all.length) { int r = in.read(all, n, all.length - n); if (r < 0) break; n += r; }
            String s = new String(all, 0, n, StandardCharsets.UTF_8);
            int nl = s.indexOf('\n');
            if (nl <= 0) return null;
            e = new Entry(s.substring(nl + 1), Long.parseLong(s.substring(0, nl)));
            MEM.put(path, e);
            return e;
        } catch (Exception ex) {
            return null;
        }
    }

    /** Removes every saved answer (sign-out, wallet removed). */
    public static void clear() {
        MEM.evictAll();
        File d = dir;
        File[] files = d == null ? null : d.listFiles();
        if (files != null) for (File f : files) //noinspection ResultOfMethodCallIgnored
            f.delete();
    }

    private static String name(String path) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-1").digest(path.getBytes(StandardCharsets.UTF_8));
            StringBuilder b = new StringBuilder();
            for (byte x : h) b.append(String.format("%02x", x));
            return b.toString();
        } catch (Exception e) {
            return Integer.toHexString(path.hashCode());
        }
    }
}
