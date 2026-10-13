package com.harvana.app.trust;

import android.os.Handler;
import android.os.Looper;

import com.harvana.app.sync.HarvanaNodeClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The named badges the node computes for a wallet (GET /wallet/labels/{address}) plus the Insured Trader mark
 * (GET /p2p/insurance-status). Labels are decoration: they unlock nothing and the node recomputes them live, so the app
 * only reads and shows them. Cached for two minutes so a list of people does not ask twice for the same wallet.
 */
public final class NodeBadges {
    private NodeBadges() {}

    public static final class Badge {
        public final String key, title, detail;
        public final boolean permanent;
        Badge(String key, String title, String detail, boolean permanent) {
            this.key = key; this.title = title; this.detail = detail; this.permanent = permanent;
        }
    }

    public interface Callback { void onBadges(List<Badge> badges); }

    private static final long TTL_MS = 2 * 60 * 1000L;
    private static final Map<String, List<Badge>> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Long> CACHED_AT = new ConcurrentHashMap<>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** Calls back on the main thread, always with a list (empty when the node cannot be reached). */
    /** Sign-out: nothing of the last account stays in memory. */
    public static void forgetAll() { CACHE.clear(); CACHED_AT.clear(); }

    public static void load(String wallet, Callback cb) {
        if (wallet == null || wallet.trim().isEmpty()) { MAIN.post(() -> cb.onBadges(new ArrayList<>())); return; }
        Long at = CACHED_AT.get(wallet);
        List<Badge> cached = CACHE.get(wallet);
        if (cached != null && at != null && System.currentTimeMillis() - at < TTL_MS) { MAIN.post(() -> cb.onBadges(cached)); return; }

        final List<Badge> out = new ArrayList<>();
        final AtomicInteger left = new AtomicInteger(2);
        final Runnable done = () -> {
            if (left.decrementAndGet() != 0) return;
            CACHE.put(wallet, out);
            CACHED_AT.put(wallet, System.currentTimeMillis());
            MAIN.post(() -> cb.onBadges(out));
        };
        HarvanaNodeClient.nodeGet("/api/v1/wallet/labels/" + wallet, new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject j) {
                JSONArray a = j.optJSONArray("labels");
                synchronized (out) {
                    if (a != null) for (int i = 0; i < a.length(); i++) {
                        JSONObject o = a.optJSONObject(i);
                        String key = o != null ? o.optString("label", "") : String.valueOf(a.opt(i));
                        if (key.isEmpty()) continue;
                        out.add(new Badge(key, titleOf(key), describe(key, o), o != null && "PERMANENT".equals(o.optString("type"))));
                    }
                }
                done.run();
            }
            @Override public void onError(String e) { done.run(); }
        });
        HarvanaNodeClient.getP2PInsuranceStatus(wallet, new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject j) {
                if (j.optBoolean("meetsInsuranceThreshold", false)) {
                    synchronized (out) {
                        out.add(new Badge("INSURED_TRADER", "Insured trader", "Has locked a P2P insurance deposit, so buyers are protected.", false));
                    }
                }
                done.run();
            }
            @Override public void onError(String e) { done.run(); }
        });
    }

    public static String titleOf(String key) {
        switch (key) {
            case "CIRCLE_VETERAN": return "Circle veteran";
            case "FOUNDING_MEMBER": return "Founding member";
            case "ZERO_DEFAULT": return "Zero default";
            case "EARLY_PAYER": return "Early payer";
            case "VOUCHED": return "Vouched";
            case "INSURED_TRADER": return "Insured trader";
            case "FORGIVEN": return "Forgiven default";
            case "CURED_DEFAULT": return "Cured default";
            case "REHABILITATED": return "Rehabilitated";
            default: return key.replace('_', ' ').toLowerCase(java.util.Locale.US);
        }
    }

    private static String describe(String key, JSONObject o) {
        JSONObject d = o == null ? null : o.optJSONObject("detail");
        switch (key) {
            case "CIRCLE_VETERAN": return "Has finished rotating circles. Permanent.";
            case "FOUNDING_MEMBER": return "Joined in the network's first period. Permanent.";
            case "ZERO_DEFAULT": return "Has never defaulted on a group.";
            case "EARLY_PAYER": return "Pays contributions ahead of the due date.";
            case "FORGIVEN": return "A group voted to forgive a default. It no longer counts against this member. Permanent.";
            case "CURED_DEFAULT": return "A default was made good, so it no longer counts as a hard default.";
            case "REHABILITATED": return "Has finished clean circles since the last default.";
            case "VOUCHED": return "Vouched for by " + (d != null ? d.optInt("activeVouches", 0) : 0) + " people.";
            default: return "";
        }
    }
}
