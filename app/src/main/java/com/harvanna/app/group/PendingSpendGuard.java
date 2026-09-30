package com.harvanna.app.group;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Client-side reservation so double-taps don't broadcast two spends.
 * Node still enforces available = confirmed − mempool pending.
 *
 * Lifecycle:
 *   tryReserve  → lock while signing/broadcasting
 *   markBroadcast → keep lock while waiting for confirmation (shorter window)
 *   release     → always call on confirm, timeout, or error
 *
 * Stale windows are short: pure double-tap protection, not a 15-minute ban.
 */
public final class PendingSpendGuard {
    private static final String PREF = "harvanna_pending_spend";

    /** While still signing / about to broadcast — block rapid double-taps. */
    private static final long RESERVE_STALE_MS = 60 * 1000L;          // 60s

    /** After broadcast, while waiting for chain confirmation. */
    private static final long BROADCAST_STALE_MS = 3 * 60 * 1000L;    // 3 min

    private PendingSpendGuard() {}

    public static boolean tryReserve(Context ctx, String address, double amountHarv) {
        if (ctx == null || address == null) return false;
        SharedPreferences p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String key = address.toLowerCase() + "_amt";
        float existing = p.getFloat(key, 0f);
        long ts = p.getLong(address.toLowerCase() + "_ts", 0L);
        String phase = p.getString(address.toLowerCase() + "_phase", "reserve");

        long staleMs = "broadcast".equals(phase) ? BROADCAST_STALE_MS : RESERVE_STALE_MS;
        if (existing > 0 && System.currentTimeMillis() - ts < staleMs) {
            return false;
        }
        p.edit()
                .putFloat(key, (float) amountHarv)
                .putLong(address.toLowerCase() + "_ts", System.currentTimeMillis())
                .putString(address.toLowerCase() + "_phase", "reserve")
                .remove(address.toLowerCase() + "_tx")
                .apply();
        return true;
    }

    public static void markBroadcast(Context ctx, String address, double amountHarv, String txHash) {
        if (ctx == null || address == null) return;
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putFloat(address.toLowerCase() + "_amt", (float) amountHarv)
                .putString(address.toLowerCase() + "_tx", txHash != null ? txHash : "")
                .putString(address.toLowerCase() + "_phase", "broadcast")
                .putLong(address.toLowerCase() + "_ts", System.currentTimeMillis())
                .apply();
    }

    /** Always call when the transfer is confirmed, abandoned, or failed. */
    public static void release(Context ctx, String address) {
        if (ctx == null || address == null) return;
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .remove(address.toLowerCase() + "_amt")
                .remove(address.toLowerCase() + "_tx")
                .remove(address.toLowerCase() + "_ts")
                .remove(address.toLowerCase() + "_phase")
                .apply();
    }

    /** @deprecated Use release(ctx, address) — amount is ignored. */
    public static void release(Context ctx, String address, double amountHarv) {
        release(ctx, address);
    }

    public static double getPendingOutbound(Context ctx, String address) {
        if (ctx == null || address == null) return 0;
        SharedPreferences p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        float existing = p.getFloat(address.toLowerCase() + "_amt", 0f);
        if (existing <= 0) return 0;
        long ts = p.getLong(address.toLowerCase() + "_ts", 0L);
        String phase = p.getString(address.toLowerCase() + "_phase", "reserve");
        long staleMs = "broadcast".equals(phase) ? BROADCAST_STALE_MS : RESERVE_STALE_MS;
        if (System.currentTimeMillis() - ts >= staleMs) return 0;
        return existing;
    }

    /** Force-clear any stuck lock for this address (debug / recovery). */
    public static void forceClear(Context ctx, String address) {
        release(ctx, address);
    }
}
