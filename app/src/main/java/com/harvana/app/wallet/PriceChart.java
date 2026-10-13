package com.harvana.app.wallet;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.utils.UiColors;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A coin's price over 1 day, 7 days or 30 days, in the display currency: price and change on top, the line under
 * them, the range buttons at the bottom. Touching the line shows that point's price and time.
 * HARV comes from the node's pool history (/api/v1/swap/price/history); other coins from CoinGecko's market chart.
 */
public final class PriceChart {

    private static final int[] DAYS = {1, 7, 30};
    private static final String[] NAMES = {"1D", "7D", "30D"};
    private static final long CACHE_MS = 5 * 60_000L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    /** "key:days" → {fetchedAtMs, points}. Points are [timeMs, usd]. */
    private static final Map<String, Object[]> CACHE = new HashMap<>();

    private final Context c;
    private final String key;
    private final LinearLayout root;
    private final TextView price, change, when;
    private final LineChart chart;
    private final TextView[] chips = new TextView[DAYS.length];
    private int days = 1;
    private int loadSeq;
    private List<double[]> shown = new ArrayList<>();

    /** {@code key} is "HARV" or a coin key with a CoinGecko id ("BTC", "ETH", …). */
    /** Sign-out: nothing of the last account stays in memory. */
    public static void forgetAll() { synchronized (CACHE) { CACHE.clear(); } }

    public static boolean supports(String key) {
        return "HARV".equals(key) || AssetPrices.geckoId(key) != null;
    }

    public static LinearLayout create(Context c, String key) {
        return new PriceChart(c, key).root;
    }

    private PriceChart(Context c, String key) {
        this.c = c;
        this.key = key;
        root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);

        price = text(24, true, UiColors.textPrimary());
        root.addView(price);
        LinearLayout sub = new LinearLayout(c);
        sub.setOrientation(LinearLayout.HORIZONTAL);
        change = text(13, true, UiColors.textSecondary());
        when = text(12, false, UiColors.textTertiary());
        when.setPadding(dp(8), 0, 0, 0);
        sub.addView(change);
        sub.addView(when);
        root.addView(sub);

        chart = new LineChart(c);
        chart.setNoDataText("—");
        chart.setNoDataTextColor(UiColors.textSecondary());
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisLeft().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setEnabled(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setHighlightPerDragEnabled(true);
        chart.setViewPortOffsets(0f, dp(8), 0f, dp(8));
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override public void onValueSelected(Entry e, Highlight h) {
                int i = Math.round(e.getX());
                if (i >= 0 && i < shown.size()) header(shown.get(i)[1], shown.get(i)[0]);
            }
            @Override public void onNothingSelected() { header(Double.NaN, 0); }
        });
        // Dragging along the line reads prices; it must not swipe the page or scroll the screen.
        chart.setOnTouchListener((v, e) -> {
            int a = e.getActionMasked();
            if (v.getParent() != null) v.getParent().requestDisallowInterceptTouchEvent(
                    a != android.view.MotionEvent.ACTION_UP && a != android.view.MotionEvent.ACTION_CANCEL);
            if (a == android.view.MotionEvent.ACTION_UP || a == android.view.MotionEvent.ACTION_CANCEL) {
                chart.highlightValues(null);
                header(Double.NaN, 0);
            }
            return false;
        });
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180));
        clp.topMargin = dp(8);
        root.addView(chart, clp);

        LinearLayout bar = new LinearLayout(c);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        for (int i = 0; i < DAYS.length; i++) {
            final int d = DAYS[i];
            TextView chip = text(13, true, UiColors.textSecondary());
            chip.setText(NAMES[i]);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(16), dp(6), dp(16), dp(6));
            chip.setOnClickListener(v -> { if (days != d) { days = d; load(); } });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(4), 0, dp(4), 0);
            bar.addView(chip, lp);
            chips[i] = chip;
        }
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = dp(8);
        root.addView(bar, blp);

        price.setText("…");
        load();
    }

    private void load() {
        paintChips();
        final int seq = ++loadSeq;
        final int d = days;
        String cacheKey = key + ":" + d;
        synchronized (CACHE) {
            Object[] hit = CACHE.get(cacheKey);
            if (hit != null && System.currentTimeMillis() - (Long) hit[0] < CACHE_MS) {
                @SuppressWarnings("unchecked") List<double[]> pts = (List<double[]>) hit[1];
                FxRates.load(c, () -> { if (seq == loadSeq) paint(pts); });
                return;
            }
        }
        Callback done = pts -> {
            if (pts != null && !pts.isEmpty()) synchronized (CACHE) { CACHE.put(cacheKey, new Object[]{System.currentTimeMillis(), pts}); }
            FxRates.load(c, () -> { if (seq == loadSeq) paint(pts); });
        };
        if ("HARV".equals(key)) harv(d, done);
        else gecko(AssetPrices.geckoId(key), d, done);
    }

    private interface Callback { void on(List<double[]> points); }

    /** HARV in dollars from the hUSD pool; when that pool has no history, from the hNGN pool at today's naira rate. */
    private void harv(int d, Callback cb) {
        int hours = d * 24;
        HarvanaNodeClient.nodeGet("/api/v1/swap/price/history?code=HUSD&hours=" + hours, new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject j) {
                List<double[]> pts = pool(j, 1.0);
                if (pts.size() >= 2) { cb.on(pts); return; }
                HarvanaNodeClient.nodeGet("/api/v1/swap/price/history?code=HNGN&hours=" + hours, new HarvanaNodeClient.JsonCallback() {
                    @Override public void onSuccess(JSONObject k) {
                        FxRates.load(c, () -> {
                            double ngn = FxRates.perUsd(c, "NGN");
                            cb.on(ngn > 0 ? pool(k, 1.0 / ngn) : new ArrayList<>());
                        });
                    }
                    @Override public void onError(String e) { cb.on(pts); }
                });
            }
            @Override public void onError(String e) { cb.on(null); }
        });
    }

    /** {points: [[hourStartMs, coinRawPerHarv], …]} → [timeMs, usd]. The pool coin has 6 decimals. */
    private static List<double[]> pool(JSONObject j, double usdPerCoin) {
        List<double[]> out = new ArrayList<>();
        JSONArray a = j == null ? null : j.optJSONArray("points");
        if (a == null) return out;
        for (int i = 0; i < a.length(); i++) {
            JSONArray p = a.optJSONArray(i);
            if (p == null || p.length() < 2 || p.optLong(1) <= 0) continue;
            out.add(new double[]{p.optLong(0), p.optLong(1) / 1e6 * usdPerCoin});
        }
        return out;
    }

    private static void gecko(String id, int d, Callback cb) {
        new Thread(() -> {
            List<double[]> out = null;
            try {
                HttpURLConnection h = (HttpURLConnection) new URL("https://api.coingecko.com/api/v3/coins/" + id
                        + "/market_chart?vs_currency=usd&days=" + d).openConnection();
                h.setConnectTimeout(10_000);
                h.setReadTimeout(15_000);
                h.setRequestProperty("Accept", "application/json");
                if (h.getResponseCode() == 200) {
                    try (InputStream in = h.getInputStream()) {
                        ByteArrayOutputStream buf = new ByteArrayOutputStream();
                        byte[] b = new byte[8192];
                        for (int n; (n = in.read(b)) > 0; ) buf.write(b, 0, n);
                        JSONArray a = new JSONObject(buf.toString("UTF-8")).optJSONArray("prices");
                        out = new ArrayList<>();
                        for (int i = 0; a != null && i < a.length(); i++) {
                            JSONArray p = a.optJSONArray(i);
                            if (p != null && p.length() >= 2) out.add(new double[]{p.optDouble(0), p.optDouble(1)});
                        }
                    }
                }
                h.disconnect();
            } catch (Exception ignored) { }
            final List<double[]> r = out;
            MAIN.post(() -> cb.on(r));
        }, "price-chart").start();
    }

    private void paint(List<double[]> pts) {
        shown = pts == null ? new ArrayList<>() : pts;
        if (shown.size() < 2) {
            chart.clear();
            chart.invalidate();
            price.setText(shown.isEmpty() ? "—" : FxRates.money(c, shown.get(0)[1]));
            change.setText("");
            when.setText("");
            return;
        }
        double first = shown.get(0)[1], last = shown.get(shown.size() - 1)[1];
        int color = last >= first ? 0xFF16A34A : 0xFFDC2626;
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < shown.size(); i++) entries.add(new Entry(i, (float) FxRates.fiat(c, shown.get(i)[1])));
        LineDataSet set = new LineDataSet(entries, key);
        set.setColor(color);
        set.setLineWidth(2f);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setDrawFilled(true);
        set.setFillColor(color);
        set.setFillAlpha(40);
        set.setHighLightColor(UiColors.textTertiary());
        set.setDrawHorizontalHighlightIndicator(false);
        set.setMode(LineDataSet.Mode.LINEAR);
        chart.setData(new LineData(set));
        chart.highlightValues(null);
        chart.invalidate();
        header(Double.NaN, 0);
    }

    /** The newest price and the change over the range; or, while a point is touched, that point. */
    private void header(double usd, double atMs) {
        if (shown.size() < 2) return;
        double first = shown.get(0)[1];
        double v = Double.isNaN(usd) ? shown.get(shown.size() - 1)[1] : usd;
        price.setText(FxRates.money(c, v));
        double pct = first > 0 ? (v - first) / first * 100.0 : 0;
        change.setText(String.format(Locale.US, "%s%.2f%%", pct >= 0 ? "+" : "", pct));
        change.setTextColor(pct >= 0 ? 0xFF16A34A : 0xFFDC2626);
        when.setText(Double.isNaN(usd) ? NAMES[indexOf(days)]
                : new SimpleDateFormat(days == 1 ? "HH:mm" : "d MMM, HH:mm", Locale.getDefault()).format(new Date((long) atMs)));
    }

    private void paintChips() {
        for (int i = 0; i < chips.length; i++) {
            boolean on = DAYS[i] == days;
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(16));
            bg.setColor(on ? UiColors.surfaceAlt() : Color.TRANSPARENT);
            chips[i].setBackground(bg);
            chips[i].setTextColor(on ? UiColors.textPrimary() : UiColors.textSecondary());
        }
    }

    private static int indexOf(int d) {
        for (int i = 0; i < DAYS.length; i++) if (DAYS[i] == d) return i;
        return 0;
    }

    private TextView text(float sp, boolean bold, int color) {
        TextView t = new TextView(c);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private int dp(int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }
}
