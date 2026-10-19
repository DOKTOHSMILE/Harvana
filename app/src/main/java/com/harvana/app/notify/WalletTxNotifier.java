package com.harvana.app.notify;

import android.content.Context;
import android.content.SharedPreferences;

import com.harvana.app.CoinInfo;
import com.harvana.app.husd.HusdClient;
import com.harvana.app.husd.Stable;
import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks the active wallet's recent confirmed transactions on the node and raises a notification for every one that is
 * new since the last check: money received, money sent, and payments the wallet signed for groups and trades.
 * The first ever check only sets the starting point, so old history never floods the inbox.
 */
public final class WalletTxNotifier {
    private WalletTxNotifier() {}

    /** How many announced transactions are remembered, so none is announced twice. */
    static final int TOLD_CAP = 400;
    /** Status bar only for a confirmation this recent; older ones go to the inbox quietly (owner, 19 Oct 2026). */
    static final long TRAY_WINDOW_MS = 15L * 60 * 1000;
    /** Inbox only this far back, so a broken baseline cannot dump a wallet's history into notifications. */
    static final long INBOX_WINDOW_MS = 24L * 60 * 60 * 1000;

    public interface Done { void onDone(List<TxEvents.Event> newEvents); }

    /** One check at a time: the socket, the 90-second sync and the background worker used to overlap on opening. */
    private static final java.util.concurrent.atomic.AtomicLong BUSY_SINCE = new java.util.concurrent.atomic.AtomicLong();

    /** Takes the one slot; a check that never answered frees it after a minute. */
    private static boolean take() {
        long now = System.currentTimeMillis(), was = BUSY_SINCE.get();
        return (was == 0 || now - was > 60_000L) && BUSY_SINCE.compareAndSet(was, now);
    }

    public static void run(Context ctx, Done done) {
        final Context c = ctx.getApplicationContext();
        final String me = WalletManager.getActiveWalletAddress(c);
        // No signed-in account yet (the app still starting): wait, so nothing is recorded as told against no inbox.
        if (me == null || me.trim().isEmpty() || com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null
                || !take()) {
            if (done != null) done.onDone(new ArrayList<>());
            return;
        }
        final Done inner = done;
        done = events -> { BUSY_SINCE.set(0); if (inner != null) inner.onDone(events); };
        try { check(c, me, done); } catch (RuntimeException e) { done.onDone(new ArrayList<>()); }
    }

    private static void check(final Context c, final String me, final Done done) {
        final SharedPreferences sp = c.getSharedPreferences("wallet_tx_notify", Context.MODE_PRIVATE);
        final String key = "baseline_" + me;
        // A wallet just switched to starts from now: what happened while it wasn't the active wallet is old news, and
        // announcing it on every switch repeated old transactions. (Money that arrives while the phone is off is still announced.)
        final boolean switched = !sp.getString("watching", "").isEmpty() && !me.equals(sp.getString("watching", ""));
        sp.edit().putString("watching", me).apply();
        final long baseline = switched ? -1L : sp.getLong(key, -1L);
        HarvanaNodeClient.getHistory(me, 0, 30, new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject json) {
                List<TxEvents.Tx> txs = parse(json);
                long newest = 0;
                for (TxEvents.Tx t : txs) newest = Math.max(newest, t.timestamp);
                if (baseline < 0) { // first run: remember where we are, tell nobody
                    sp.edit().putLong(key, Math.max(newest, System.currentTimeMillis() - 60_000L)).apply();
                    if (done != null) done.onDone(new ArrayList<>());
                    return;
                }
                List<TxEvents.Event> events = TxEvents.classify(txs, me, baseline, CoinInfo.ticker());
                List<TxEvents.Event> fresh = new ArrayList<>();
                java.util.Set<String> told = new java.util.LinkedHashSet<>(java.util.Arrays.asList(sp.getString("told", "").split(",")));
                long now = System.currentTimeMillis();
                for (TxEvents.Event e : events) {
                    if (told.contains(e.key)) continue;                     // announced before, even if since cleared
                    long at = e.at;
                    long age = at > 0 ? now - at : Long.MAX_VALUE;
                    if (age > INBOX_WINDOW_MS) { told.add(e.key); continue; } // too old for the inbox; the activity list still has it
                    boolean tray = age <= TRAY_WINDOW_MS;
                    if (NotificationCenter.post(c, NotificationCenter.TX, e.key, e.title, e.body, "", -1, at, tray)) fresh.add(e);
                    told.add(e.key);
                }
                java.util.List<String> keep = new java.util.ArrayList<>(told);
                if (keep.size() > TOLD_CAP) keep = keep.subList(keep.size() - TOLD_CAP, keep.size());
                SharedPreferences.Editor ed = sp.edit().putString("told", android.text.TextUtils.join(",", keep));
                if (newest > baseline) ed.putLong(key, newest);
                ed.commit();
                if (done != null) done.onDone(fresh);
            }
            @Override public void onError(String error) { if (done != null) done.onDone(new ArrayList<>()); }
        });
    }

    static List<TxEvents.Tx> parse(JSONObject json) {
        List<TxEvents.Tx> out = new ArrayList<>();
        JSONArray arr = json == null ? null : json.optJSONArray("transactions");
        if (arr == null && json != null) arr = json.optJSONArray("data");
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            TxEvents.Tx t = new TxEvents.Tx();
            t.hash = o.optString("txHash", "");
            t.from = o.optString("senderAddress", o.optString("from", ""));
            t.to = o.optString("recipientAddress", o.optString("to", ""));
            t.memo = o.optString("memo", "");
            t.status = o.optString("status", "confirmed");
            String asset = o.optString("asset", o.optString("mintId", ""));
            if (Stable.isStable(asset) && o.has("amount") && !o.isNull("amount")) {
                t.amount = o.optLong("amount", 0L) / (double) HusdClient.UNIT;
                t.symbol = Stable.symbol(asset);
            } else {
                t.amount = o.has("amount") && !o.isNull("amount") ? HarvanaNodeClient.toHarvDisplay(o.optLong("amount", 0L))
                    : o.optDouble("value", o.optDouble("value_harv", 0.0));
            }
            long ts = o.optLong("timestamp", 0L);
            if (ts > 0 && ts < 1_000_000_000_000L) ts *= 1000L;
            t.timestamp = ts;
            out.add(t);
        }
        return out;
    }
}
