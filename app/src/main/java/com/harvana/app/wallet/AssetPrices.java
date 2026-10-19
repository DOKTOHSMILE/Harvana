package com.harvana.app.wallet;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One free price call for every coin except HARV, which has no public market. CoinGecko's public price endpoint needs no key. The whole app shares one result and will not ask again for ten minutes, or for twenty after a refusal, so it stays under the free limit.
 */
public final class AssetPrices {
    private AssetPrices() {
    }

    public static final class Quote {
        public final double usd;
        public final double change;

        Quote(double usd, double change) {
            this.usd = usd;
            this.change = change;
        }
    }

    private static final long FRESH_MS = 10L * 60L * 1000L;
    private static final long BACKOFF_MS = 20L * 60L * 1000L;
    private static final String PREFS = "asset_prices";
    /** Coin key, then the CoinGecko id. BASE is ETH on another chain, so it uses the ETH price. */
    private static final String[][] IDS = { { "BTC", "bitcoin" }, { "ETH", "ethereum" },
            { "BASE", "ethereum" }, { "SOL", "solana" }, { "LTC", "litecoin" },
            { "DOGE", "dogecoin" }, { "BNB", "binancecoin" }, { "POL", "polygon-ecosystem-token" },
            { "TRX", "tron" }, { "PI", "pi-network" } };

    /** The CoinGecko id for a coin key ("BTC" → "bitcoin"), or null. */
    public static String geckoId(String key) {
        for (String[] r : IDS)
            if (r[0].equals(key))
                return r[1];
        CustomAssets.Asset a = CustomAssets.find(key);
        return a == null || a.geckoId == null || a.geckoId.isEmpty() ? null : a.geckoId;
    }

    /** Every {key, CoinGecko id}: the built-in coins, then the chains and tokens the person added. */
    private static List<String[]> allIds() {
        List<String[]> out = new ArrayList<>(java.util.Arrays.asList(IDS));
        for (CustomAssets.Asset a : CustomAssets.cached())
            if (a.geckoId != null && !a.geckoId.isEmpty())
                out.add(new String[] { a.key(), a.geckoId });
        return out;
    }

    private static boolean builtIn(String key) {
        for (String[] r : IDS)
            if (r[0].equals(key))
                return true;
        return false;
    }

    private static final String ENDPOINT = "https://api.coingecko.com/api/v3/simple/price?vs_currencies=usd&include_24hr_change=true&ids="
            + "bitcoin,ethereum,solana,litecoin,dogecoin,binancecoin,polygon-ecosystem-token,tron,pi-network";

    private static final Map<String, Quote> mem = new HashMap<>();
    private static final List<Runnable> waiters = new ArrayList<>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static long fetchedAt;
    private static long nextAllowed;
    private static boolean inFlight;
    private static boolean diskRead;

    /** The last price this phone already has, including one saved from an earlier visit. Never calls the network. */
    public static Quote peek(Context c, String key) {
        if (key == null)
            return null;
        synchronized (mem) {
            if (!diskRead && c != null)
                readDisk(c.getApplicationContext());
            return mem.get(key);
        }
    }

    /**
     * Delivers prices on the main thread. Uses the saved ones when they are still fresh, and otherwise makes the single shared call.
     */
    public static void load(Context c, Runnable onMain) {
        if (c == null)
            return;
        Context app = c.getApplicationContext();
        synchronized (mem) {
            if (!diskRead)
                readDisk(app);
            long now = System.currentTimeMillis();
            if ((fetchedAt > 0 && now - fetchedAt < FRESH_MS && !mem.isEmpty())
                    || now < nextAllowed) {
                if (onMain != null)
                    MAIN.post(onMain);
                return;
            }
            if (onMain != null)
                waiters.add(onMain);
            if (inFlight)
                return;
            inFlight = true;
        }
        new Thread(() -> {
            boolean ok = fetch(app);
            List<Runnable> go;
            synchronized (mem) {
                inFlight = false;
                long now = System.currentTimeMillis();
                if (ok) {
                    fetchedAt = sharedAt > 0 ? sharedAt : now;
                    nextAllowed = fetchedAt + FRESH_MS;
                    writeDisk(app);
                } else {
                    nextAllowed = now + BACKOFF_MS;
                    writeDisk(app);
                }
                go = new ArrayList<>(waiters);
                waiters.clear();
            }
            for (Runnable r : go)
                MAIN.post(r);
        }, "asset-prices").start();
    }

    /** A coin gained a CoinGecko id: the next {@link #load} asks again instead of serving the saved prices. */
    public static void forget(Context c) {
        synchronized (mem) {
            fetchedAt = 0;
            nextAllowed = 0;
        }
    }

    public static String usd(double v) {
        DecimalFormatSymbols us = DecimalFormatSymbols.getInstance(Locale.US);
        DecimalFormat fmt;
        if (v >= 1000)
            fmt = new DecimalFormat("#,##0", us);
        else if (v >= 1)
            fmt = new DecimalFormat("#,##0.00", us);
        else if (v >= 0.01)
            fmt = new DecimalFormat("0.00##", us);
        else
            fmt = new DecimalFormat("0.########", us);
        return "$" + fmt.format(v);
    }

    /** "+1.20% today" or an empty string when the change was not sent. */
    public static String change(Quote q) {
        if (q == null || Double.isNaN(q.change))
            return "";
        DecimalFormat fmt = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return (q.change >= 0 ? " · +" : " · ") + fmt.format(q.change) + "% today";
    }

    /** The leading number in "0.01 BTC", or null when the line is not an amount. */
    public static Double amount(CharSequence line) {
        if (line == null)
            return null;
        String t = line.toString().trim().replace(",", "");
        int n = 0;
        while (n < t.length() && (Character.isDigit(t.charAt(n)) || t.charAt(n) == '.'
                || (n == 0 && t.charAt(n) == '-')))
            n++;
        if (n == 0)
            return null;
        try {
            return Double.parseDouble(t.substring(0, n));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static long sharedAt;

    /** Shared prices first. CoinGecko only when nobody has published a fresh copy. */
    private static boolean fetch(Context app) {
        if (app == null)
            return false;
        sharedAt = 0;
        if (readShared()) {
            boolean missing = false;
            synchronized (mem) {
                for (String[] r : allIds())
                    if (!mem.containsKey(r[0]))
                        missing = true;
            }
            if (missing)
                fetchRemote(); // an added coin the shared copy doesn't carry
            return true;
        }
        if (!fetchRemote())
            return false;
        writeShared();
        return true;
    }

    private static boolean readShared() {
        try {
            if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null)
                return false;
            com.google.firebase.firestore.DocumentSnapshot snap = com.google.android.gms.tasks.Tasks
                    .await(com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("MARKET_PRICES").document("spot").get(), 5,
                            java.util.concurrent.TimeUnit.SECONDS);
            if (snap == null || !snap.exists())
                return false;
            com.google.firebase.Timestamp ts = snap.getTimestamp("updatedAt");
            if (ts == null)
                return false;
            long at = ts.toDate().getTime();
            if (System.currentTimeMillis() - at > FRESH_MS)
                return false;
            Object raw = snap.get("quotes");
            if (!(raw instanceof Map))
                return false;
            int n = 0;
            synchronized (mem) {
                for (Map.Entry<?, ?> e : ((Map<?, ?>) raw).entrySet()) {
                    if (!(e.getKey() instanceof String) || !(e.getValue() instanceof Map))
                        continue;
                    Map<?, ?> q = (Map<?, ?>) e.getValue();
                    double usd = num(q.get("usd"));
                    if (usd <= 0)
                        continue;
                    mem.put((String) e.getKey(), new Quote(usd, num(q.get("change"))));
                    n++;
                }
            }
            if (n == 0)
                return false;
            sharedAt = at;
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void writeShared() {
        try {
            if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null)
                return;
            Map<String, Object> quotes = new HashMap<>();
            synchronized (mem) {
                for (Map.Entry<String, Quote> e : mem.entrySet()) {
                    if (!builtIn(e.getKey()))
                        continue; // what a person added stays on their phone
                    Map<String, Object> q = new HashMap<>();
                    double ch = e.getValue().change;
                    q.put("usd", e.getValue().usd);
                    q.put("change", Double.isNaN(ch) ? 0 : ch);
                    quotes.put(e.getKey(), q);
                }
            }
            if (quotes.isEmpty())
                return;
            Map<String, Object> doc = new HashMap<>();
            doc.put("source", "app");
            doc.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
            doc.put("quotes", quotes);
            com.google.android.gms.tasks.Tasks.await(
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("MARKET_PRICES").document("spot").set(doc),
                    5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    private static double num(Object v) {
        if (v instanceof Number)
            return ((Number) v).doubleValue();
        return Double.NaN;
    }

    private static boolean fetchRemote() {
        HttpURLConnection c = null;
        try {
            StringBuilder extra = new StringBuilder();
            for (String[] r : allIds())
                if (!builtIn(r[0]))
                    extra.append(',').append(java.net.URLEncoder.encode(r[1], "UTF-8"));
            c = (HttpURLConnection) new URL(ENDPOINT + extra).openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            c.setRequestProperty("Accept", "application/json");
            c.setRequestProperty("User-Agent", "Harvana");
            int code = c.getResponseCode();
            if (code != 200) {
                // Rate-limited or error on CoinGecko: Fall back to Binance & DexScreener
                return fetchFallbackMultiSource();
            }
            JSONObject o = new JSONObject(read(c.getInputStream()));
            synchronized (mem) {
                for (String[] pair : allIds()) {
                    JSONObject row = o.optJSONObject(pair[1]);
                    if (row == null || !row.has("usd"))
                        continue;
                    double usd = row.optDouble("usd", Double.NaN);
                    if (Double.isNaN(usd) || usd <= 0)
                        continue;
                    double ch = row.has("usd_24h_change")
                            ? row.optDouble("usd_24h_change", Double.NaN)
                            : Double.NaN;
                    mem.put(pair[0], new Quote(usd, ch));
                }
            }
            return !mem.isEmpty();
        } catch (Exception ignored) {
            return false;
        } finally {
            if (c != null)
                c.disconnect();
        }
    }

    private static String read(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[2048];
        int n, total = 0;
        while ((n = in.read(buf)) != -1 && total < 65536) {
            out.write(buf, 0, n);
            total += n;
        }
        return out.toString("UTF-8");
    }

    private static void readDisk(Context app) {
        diskRead = true;
        android.content.SharedPreferences p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        fetchedAt = p.getLong("fetchedAt", 0L);
        nextAllowed = p.getLong("nextAllowed", 0L);
        String raw = p.getString("quotes", "");
        if (raw == null || raw.isEmpty())
            return;
        for (String part : raw.split(";")) {
            String[] bits = part.split("=");
            if (bits.length != 2)
                continue;
            String[] nums = bits[1].split(",");
            try {
                double usd = Double.parseDouble(nums[0]);
                double ch = nums.length > 1 ? Double.parseDouble(nums[1]) : Double.NaN;
                if (usd > 0)
                    mem.put(bits[0], new Quote(usd, ch));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private static void writeDisk(Context app) {
        StringBuilder raw = new StringBuilder();
        for (Map.Entry<String, Quote> e : mem.entrySet()) {
            if (raw.length() > 0)
                raw.append(';');
            raw.append(e.getKey()).append('=').append(e.getValue().usd).append(',')
                    .append(e.getValue().change);
        }
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("quotes", raw.toString()).putLong("fetchedAt", fetchedAt)
                .putLong("nextAllowed", nextAllowed).apply();
    }

    private static boolean fetchFallbackMultiSource() {
        boolean gotAny = false;
        try {
            HttpURLConnection bin = (HttpURLConnection) new URL(
                    "https://api.binance.com/api/v3/ticker/24hr").openConnection();
            bin.setConnectTimeout(8000);
            bin.setReadTimeout(8000);
            bin.setRequestProperty("Accept", "application/json");
            if (bin.getResponseCode() == 200) {
                JSONArray arr = new JSONArray(read(bin.getInputStream()));
                Map<String, double[]> bMap = new HashMap<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject row = arr.getJSONObject(i);
                    String s = row.optString("symbol", "");
                    double price = row.optDouble("lastPrice", 0.0);
                    double change = row.optDouble("priceChangePercent", 0.0);
                    if (price > 0)
                        bMap.put(s, new double[] { price, change });
                }
                synchronized (mem) {
                    if (bMap.containsKey("BTCUSDT")) {
                        mem.put("BTC", new Quote(bMap.get("BTCUSDT")[0], bMap.get("BTCUSDT")[1]));
                        gotAny = true;
                    }
                    if (bMap.containsKey("ETHUSDT")) {
                        mem.put("ETH", new Quote(bMap.get("ETHUSDT")[0], bMap.get("ETHUSDT")[1]));
                        mem.put("BASE", new Quote(bMap.get("ETHUSDT")[0], bMap.get("ETHUSDT")[1]));
                        mem.put("ARB", new Quote(bMap.get("ETHUSDT")[0], bMap.get("ETHUSDT")[1]));
                        mem.put("OP", new Quote(bMap.get("ETHUSDT")[0], bMap.get("ETHUSDT")[1]));
                        gotAny = true;
                    }
                    if (bMap.containsKey("SOLUSDT")) {
                        mem.put("SOL", new Quote(bMap.get("SOLUSDT")[0], bMap.get("SOLUSDT")[1]));
                        gotAny = true;
                    }
                    if (bMap.containsKey("BNBUSDT")) {
                        mem.put("BNB", new Quote(bMap.get("BNBUSDT")[0], bMap.get("BNBUSDT")[1]));
                        gotAny = true;
                    }
                    if (bMap.containsKey("DOGEUSDT")) {
                        mem.put("DOGE",
                                new Quote(bMap.get("DOGEUSDT")[0], bMap.get("DOGEUSDT")[1]));
                        gotAny = true;
                    }
                    if (bMap.containsKey("LTCUSDT")) {
                        mem.put("LTC", new Quote(bMap.get("LTCUSDT")[0], bMap.get("LTCUSDT")[1]));
                        gotAny = true;
                    }
                    if (bMap.containsKey("MATICUSDT")) {
                        mem.put("POL",
                                new Quote(bMap.get("MATICUSDT")[0], bMap.get("MATICUSDT")[1]));
                        gotAny = true;
                    }
                    if (bMap.containsKey("TRXUSDT")) {
                        mem.put("TRX", new Quote(bMap.get("TRXUSDT")[0], bMap.get("TRXUSDT")[1]));
                        gotAny = true;
                    }
                    if (bMap.containsKey("AVAXUSDT")) {
                        mem.put("AVAX",
                                new Quote(bMap.get("AVAXUSDT")[0], bMap.get("AVAXUSDT")[1]));
                        gotAny = true;
                    }
                    if (bMap.containsKey("FTMUSDT")) {
                        mem.put("FTM", new Quote(bMap.get("FTMUSDT")[0], bMap.get("FTMUSDT")[1]));
                        gotAny = true;
                    }
                }
            }
            bin.disconnect();
        } catch (Exception ignored) {
        }
        return gotAny;
    }
}
