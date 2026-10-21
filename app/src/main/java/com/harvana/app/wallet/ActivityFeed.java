package com.harvana.app.wallet;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.harvana.app.sync.HarvanaNodeClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Every transaction of one wallet, from every source, in one list (owner, 20 Oct 2026: the activity list must not
 * glitch while one source loads and another waits). The sources are fetched together: the Harvana node's history
 * (HARV and the Harvana coins), its pending pool, the transfers this phone sent and is still waiting on
 * (PendingTxStore), and every other chain and added coin (ChainHistory, which includes this phone's own sends there
 * while they are pending). Nothing is shown until all have answered (or the time limit passed): then the rows are
 * merged, a pending row is replaced by its confirmed one, and the list is sorted newest first and published once.
 * The last list is kept on the phone so the screen opens on it at once.
 */
public final class ActivityFeed {

    public interface Listener { void onFeed(List<HashMap<String, Object>> rows, boolean fresh); }

    public static final int PAGE_SIZE = 20;
    private static final long WAIT_MS = 25_000L;
    private static final Map<String, ActivityFeed> BY_WALLET = new HashMap<>();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final String wallet;
    private final CopyOnWriteArraySet<Listener> listeners = new CopyOnWriteArraySet<>();
    private final List<HashMap<String, Object>> nodeRows = new ArrayList<>();
    private List<HashMap<String, Object>> chainRows = new ArrayList<>();
    private final List<HashMap<String, Object>> serverPending = new ArrayList<>();
    private List<HashMap<String, Object>> published = new ArrayList<>();
    private int nodePages = 0;
    private boolean hasMore = true;
    private boolean running, chainLoaded;
    private long generation;

    private ActivityFeed(String wallet) { this.wallet = wallet; }

    public static synchronized ActivityFeed of(String wallet) {
        ActivityFeed f = BY_WALLET.get(wallet);
        if (f == null) { f = new ActivityFeed(wallet); BY_WALLET.put(wallet, f); }
        return f;
    }

    public void listen(Listener l) { listeners.add(l); }
    public void unlisten(Listener l) { listeners.remove(l); }
    public boolean hasMore() { return hasMore; }

    /** What the screen shows before the sources answer: the last complete list (and this phone's pending sends). */
    public List<HashMap<String, Object>> cached(Context c) {
        if (!published.isEmpty()) return new ArrayList<>(published);
        List<HashMap<String, Object>> saved = WalletCache.loadList(c, "feed_" + wallet);
        List<HashMap<String, Object>> rows = new ArrayList<>(PendingTxStore.loadAll(c, wallet));
        if (saved != null) rows.addAll(saved);
        return merge(rows);
    }

    /**
     * Asks every source again and publishes once when all have answered. {@code withChains} false reuses the other
     * chains' last rows (their explorers are slower and are asked less often).
     */
    public synchronized void refresh(Context context, boolean withChains) {
        if (running) return;
        running = true;
        final long gen = ++generation;
        final Context app = context.getApplicationContext();
        final boolean chains = withChains || !chainLoaded;
        POOL.execute(() -> {
            PendingTxStore.checkStaleAndMarkFailed(app, wallet);
            final CountDownLatch done = new CountDownLatch(chains ? 3 : 2);
            final List<HashMap<String, Object>> node = new ArrayList<>();
            final List<HashMap<String, Object>> pend = new ArrayList<>();
            final boolean[] nodeOk = {false};
            HarvanaNodeClient.getHistory(wallet, 0, PAGE_SIZE, new HarvanaNodeClient.JsonCallback() {
                @Override public void onSuccess(JSONObject json) { node.addAll(parseNodePage(json)); nodeOk[0] = true; done.countDown(); }
                @Override public void onError(String e) { done.countDown(); }
            });
            HarvanaNodeClient.getPending(wallet, new HarvanaNodeClient.JsonCallback() {
                @Override public void onSuccess(JSONObject json) { pend.addAll(parsePending(json)); done.countDown(); }
                @Override public void onError(String e) { done.countDown(); }
            });
            final List<HashMap<String, Object>> chainNow = new ArrayList<>();
            if (chains) POOL.execute(() -> {
                try { chainNow.addAll(ChainHistory.load(app, wallet)); } catch (Exception ignored) { }
                done.countDown();
            });
            try { done.await(WAIT_MS, TimeUnit.MILLISECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            synchronized (ActivityFeed.this) {
                running = false;
                if (gen != generation) return;
                if (nodeOk[0]) {
                    nodeRows.clear();
                    nodeRows.addAll(node);
                    nodePages = 1;
                    hasMore = node.size() >= PAGE_SIZE;
                    List<String> hashes = new ArrayList<>();
                    for (HashMap<String, Object> m : node) hashes.add(String.valueOf(m.get("txHash")));
                    PendingTxStore.pruneConfirmed(app, wallet, hashes);
                }
                serverPending.clear();
                serverPending.addAll(pend);
                if (chains && !chainNow.isEmpty() || chains && done.getCount() == 0) { chainRows = chainNow; chainLoaded = true; }
                publish(app, true);
            }
        });
    }

    /** The next older page of the Harvana history, added to the list (published once it is in). */
    public synchronized void loadMore(Context context) {
        if (running || !hasMore) return;
        running = true;
        final Context app = context.getApplicationContext();
        final int page = nodePages;
        HarvanaNodeClient.getHistory(wallet, page, PAGE_SIZE, new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject json) {
                List<HashMap<String, Object>> more = parseNodePage(json);
                synchronized (ActivityFeed.this) {
                    running = false;
                    nodeRows.addAll(more);
                    nodePages = page + 1;
                    hasMore = more.size() >= PAGE_SIZE;
                    publish(app, true);
                }
            }
            @Override public void onError(String e) { synchronized (ActivityFeed.this) { running = false; } }
        });
    }

    private void publish(Context app, boolean fresh) {
        List<HashMap<String, Object>> all = new ArrayList<>(PendingTxStore.loadAll(app, wallet));
        all.addAll(serverPending);
        all.addAll(nodeRows);
        all.addAll(chainRows);
        List<HashMap<String, Object>> rows = merge(all);
        published = rows;
        WalletCache.saveList(app, "feed_" + wallet, rows.size() > 100 ? new ArrayList<>(rows.subList(0, 100)) : rows);
        final List<HashMap<String, Object>> out = new ArrayList<>(rows);
        MAIN.post(() -> { for (Listener l : listeners) l.onFeed(out, fresh); });
    }

    /** One row per transaction (a confirmed row wins over a pending one), newest first. */
    static List<HashMap<String, Object>> merge(List<HashMap<String, Object>> rows) {
        LinkedHashMap<String, HashMap<String, Object>> byHash = new LinkedHashMap<>();
        List<HashMap<String, Object>> noHash = new ArrayList<>();
        for (HashMap<String, Object> m : rows) {
            if (m == null) continue;
            String h = String.valueOf(m.get("txHash"));
            if (h.isEmpty() || "null".equals(h)) { noHash.add(m); continue; }
            String key = h + "|" + m.get("tokenSymbol");
            HashMap<String, Object> had = byHash.get(key);
            if (had == null || pending(had) && !pending(m)) byHash.put(key, m);
        }
        List<HashMap<String, Object>> out = new ArrayList<>(byHash.values());
        out.addAll(noHash);
        out.sort((a, b) -> Long.compare(ts(b), ts(a)));
        return out;
    }

    private static boolean pending(HashMap<String, Object> m) { return "pending".equalsIgnoreCase(String.valueOf(m.get("status"))); }

    static long ts(HashMap<String, Object> m) {
        Object v = m == null ? null : m.get("timestamp");
        long t = 0;
        if (v instanceof Number) t = ((Number) v).longValue();
        else if (v != null) try { t = Long.parseLong(String.valueOf(v).trim()); } catch (Exception ignored) { }
        if (t > 0 && t < 1_000_000_000_000L) t *= 1000L;
        return t;
    }

    // ── the Harvana node's answers, as rows ─────────────────────────────────

    public static List<HashMap<String, Object>> parseNodePage(JSONObject json) {
        List<HashMap<String, Object>> out = new ArrayList<>();
        JSONArray txs = json == null ? null : json.optJSONArray("transactions");
        if (txs == null && json != null) txs = json.optJSONArray("data");
        for (int i = 0; txs != null && i < txs.length(); i++) {
            JSONObject tx = txs.optJSONObject(i);
            if (tx == null) continue;
            HashMap<String, Object> item = new HashMap<>();
            item.put("senderAddress", tx.optString("senderAddress", tx.optString("from", "")));
            item.put("recipientAddress", tx.optString("recipientAddress", tx.optString("to", "")));
            item.put("memo", tx.optString("memo", ""));
            // Harvana coins (hUSD, hNGN, …) ride on the same history; their amounts have 6 decimals, not HARV's 8.
            String stableAsset = tx.optString("asset", tx.optString("mintId", ""));
            boolean coin = com.harvana.app.husd.Stable.isStable(stableAsset);
            double value;
            if (tx.has("amount") && !tx.isNull("amount")) {
                long raw = tx.optLong("amount", 0L);
                value = coin ? raw / (double) com.harvana.app.husd.HusdClient.UNIT : HarvanaNodeClient.toHarvDisplay(raw);
            } else {
                value = tx.optDouble("value", tx.optDouble("value_harv", 0.0));
            }
            item.put("value", value);
            item.put("tokenSymbol", coin ? com.harvana.app.husd.Stable.symbol(stableAsset)
                    : tx.optString("tokenSymbol", tx.optString("token", com.harvana.app.CoinInfo.ticker())));
            item.put("txHash", tx.optString("txHash", ""));
            item.put("status", tx.optString("status", "confirmed"));
            double fee;
            String feeSymbol = com.harvana.app.CoinInfo.ticker();
            if (coin && tx.optLong("gas", 0L) == 0L && tx.has("coinFeeRaw")) {
                fee = tx.optLong("coinFeeRaw", 0L) / (double) com.harvana.app.husd.HusdClient.UNIT;
                feeSymbol = com.harvana.app.husd.Stable.symbol(stableAsset);
            } else if (tx.has("gas") && !tx.isNull("gas")) {
                fee = HarvanaNodeClient.toHarvDisplay(tx.optLong("gas", 0L));
            } else {
                fee = tx.optDouble("harvFee", tx.optDouble("fee_harv", 0.0));
            }
            item.put("harvFee", fee);
            item.put("feeSymbol", feeSymbol);
            item.put("burnAmount", tx.optDouble("burnAmount", 0.0));
            item.put("blockNumber", tx.optLong("blockHeight", 0L));
            item.put("nonce", tx.optLong("nonce", 0L));
            item.put("confirmations", tx.optLong("confirmations", 0L));
            long confirmed = tx.optLong("confirmedAt", tx.optLong("timestamp", 0L));
            long initiated = tx.optLong("initiatedAt", 0L);
            if (confirmed > 0 && confirmed < 1_000_000_000_000L) confirmed *= 1000L;
            if (initiated > 0 && initiated < 1_000_000_000_000L) initiated *= 1000L;
            item.put("timestamp", confirmed > 0 ? confirmed : (initiated > 0 ? initiated : System.currentTimeMillis()));
            item.put("initiatedAt", initiated);
            item.put("confirmedAt", confirmed);
            out.add(item);
        }
        return out;
    }

    static List<HashMap<String, Object>> parsePending(JSONObject json) {
        List<HashMap<String, Object>> out = new ArrayList<>();
        JSONArray arr = json == null ? null : json.optJSONArray("pending");
        for (int i = 0; arr != null && i < arr.length(); i++) {
            JSONObject tx = arr.optJSONObject(i);
            if (tx == null || tx.optString("txHash", "").isEmpty()) continue;
            HashMap<String, Object> e = new HashMap<>();
            e.put("txHash", tx.optString("txHash"));
            e.put("senderAddress", tx.optString("from", ""));
            e.put("recipientAddress", tx.optString("to", ""));
            e.put("memo", tx.optString("memo", ""));
            String code = tx.optString("asset", tx.optString("mintId", ""));
            boolean coin = com.harvana.app.husd.Stable.isStable(code);
            e.put("value", coin ? tx.optLong("amount", 0L) / (double) com.harvana.app.husd.HusdClient.UNIT
                    : HarvanaNodeClient.toHarvDisplay(tx.optLong("amount", 0L)));
            e.put("tokenSymbol", coin ? com.harvana.app.husd.Stable.symbol(code) : com.harvana.app.CoinInfo.ticker());
            e.put("status", "pending");
            long at = tx.optLong("receivedAt", tx.optLong("timestamp", 0L));
            if (at <= 0) at = System.currentTimeMillis();
            else if (at < 1_000_000_000_000L) at *= 1000L;
            e.put("timestamp", at);
            e.put("initiatedAt", at);
            e.put("confirmedAt", 0L);
            out.add(e);
        }
        return out;
    }
}
