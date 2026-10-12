package com.harvana.app;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.harvana.app.sync.NodeEngagementClient;
import com.harvana.app.trust.NetworkTrust;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * How the signed-in person reads: reads per day for 30 days, the popular days of the week and the peak hours. The numbers
 * come from the node, which counts only verified reads. Only the owner can fetch them (the request is signed by the wallet).
 */
public class ActivityStatsActivity extends AppCompatActivity {

    private static final String[] DAYS = {"SU", "MO", "TU", "WE", "TH", "FR", "SA"};

    private TextView state, note;
    private BarChart daily, hours;
    private LinearLayout weekdays, usage;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_activity_stats);
        Toolbar toolbar = findViewById(R.id._toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        state = findViewById(R.id.tv_stats_state);
        note = findViewById(R.id.tv_stats_note);
        daily = findViewById(R.id.chart_daily);
        hours = findViewById(R.id.chart_hours);
        weekdays = findViewById(R.id.ll_weekdays);
        usage = findViewById(R.id.ll_usage);
        tile(R.id.tile_reads, "Verified reads", "--");
        tile(R.id.tile_days, "Active days (30)", "--");
        tile(R.id.tile_gifts, "Gifts sent", "--");
        tile(R.id.tile_trust, "Trust score", "--");
        styleChart(daily);
        styleChart(hours);
        showLocal();

        // Milestones first: the activity below, turned into awards (network, Learn Hub, and habits counted on this phone).
        final android.view.ViewGroup page = (android.view.ViewGroup) state.getParent();
        final LinearLayout shelfHolder = new LinearLayout(this);
        shelfHolder.setOrientation(LinearLayout.VERTICAL);
        page.addView(shelfHolder, page.indexOfChild(state) + 1);
        String meUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null ? ""
                : com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        com.harvana.app.milestone.MilestoneLoader.load(this, WalletManager.getActiveWalletAddress(this), meUid, true, items -> {
            if (isFinishing()) return;
            shelfHolder.removeAllViews();
            shelfHolder.addView(com.harvana.app.milestone.MilestoneShelf.card(this, items, true));
        });

        String wallet = WalletManager.getActiveWalletAddress(this);
        if (wallet == null || wallet.isEmpty()) {
            state.setText("Create or import a wallet to see your activity. Reads and gifts are recorded against your wallet.");
            return;
        }
        load(wallet);
        state.setOnClickListener(v -> load(wallet));
        NetworkTrust.wallet(wallet, score -> runOnUiThread(() ->
                tile(R.id.tile_trust, "Trust score", score.published && score.percent >= 0 ? score.percent + "% · " + score.tier : "Not scored yet")));
    }

    /** Everything the app has seen you do on this phone: taps, screens, time, by day, weekday and hour. */
    private void showLocal() {
        com.harvana.app.utils.ActivityTracker.Summary t = com.harvana.app.utils.ActivityTracker.summary(this);
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 30; i++) entries.add(new BarEntry(i, t.daily30[i]));
        fill(daily, entries, new ValueFormatter() {
            @Override public String getFormattedValue(float v) {
                int back = 29 - (int) v;
                return back == 0 ? "today" : back % 7 == 0 ? back + "d" : "";
            }
        });
        int maxWd = 1;
        for (int v : t.weekdays) maxWd = Math.max(maxWd, v);
        weekdays.removeAllViews();
        for (int i = 0; i < 7; i++) weekdays.addView(weekdayRow(DAYS[i], t.weekdays[i], maxWd));
        List<BarEntry> hourEntries = new ArrayList<>();
        for (int h = 0; h < 24; h++) hourEntries.add(new BarEntry(h, t.hours[h]));
        fill(hours, hourEntries, new ValueFormatter() {
            @Override public String getFormattedValue(float v) { int h = (int) v; return h % 6 == 0 ? h + ":00" : ""; }
        });
        usage.removeAllViews();
        usage.addView(usageRow("Time in the app today", minutes(t.secondsToday)));
        usage.addView(usageRow("Time in the app (30 days)", minutes(t.seconds30)));
        usage.addView(usageRow("Days you used the app (30)", String.valueOf(t.activeDays30)));
        usage.addView(usageRow("Visits", String.valueOf(t.sessions)));
        usage.addView(usageRow("Taps", String.valueOf(t.taps)));
        java.util.List<java.util.Map.Entry<String, Integer>> top = new ArrayList<>(t.screens.entrySet());
        java.util.Collections.sort(top, (x, y) -> y.getValue() - x.getValue());
        for (int i = 0; i < top.size() && i < 6; i++) usage.addView(usageRow(friendly(top.get(i).getKey()), top.get(i).getValue() + " opens"));
    }

    private static String minutes(long sec) { return sec < 60 ? sec + " s" : sec < 3600 ? (sec / 60) + " min" : (sec / 3600) + " h " + ((sec % 3600) / 60) + " min"; }

    private static String friendly(String cls) {
        String n = cls.replace("Activity", "").replace("Fragment", "");
        return n.replaceAll("([a-z])([A-Z])", "$1 $2");
    }

    private View usageRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, dp(6), 0, dp(6));
        TextView l = new TextView(this);
        l.setText(label); l.setTextSize(13);
        l.setTextColor(ContextCompat.getColor(this, R.color.colorTextTertiary));
        row.addView(l, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView v = new TextView(this);
        v.setText(value); v.setTextSize(13); v.setTypeface(null, android.graphics.Typeface.BOLD);
        v.setTextColor(ContextCompat.getColor(this, R.color.colorTextPrimary));
        row.addView(v);
        return row;
    }

    private void load(String wallet) {
        state.setText("Loading your activity\u2026");
        NodeEngagementClient.ownerStats(getApplicationContext(), wallet, new NodeEngagementClient.Callback() {
            @Override public void onSuccess(JSONObject json) { runOnUiThread(() -> show(json)); }
            @Override public void onError(String error) {
                runOnUiThread(() -> state.setText("Could not load your activity: " + why(error) + " Tap here to try again."));
            }
        });
    }

    private static String why(String e) {
        if (e == null || e.isEmpty()) return "the network did not answer.";
        if (com.harvana.app.utils.ErrorText.is(e, "STALE_TIMESTAMP")) return "your phone's clock is off. Set it to automatic time.";
        if (com.harvana.app.utils.ErrorText.is(e, "INVALID_SIGNATURE") || com.harvana.app.utils.ErrorText.is(e, "PUBLIC_KEY")) return "this wallet could not be verified.";
        return "the network did not answer (" + (e.length() > 50 ? e.substring(0, 50) : e) + ").";
    }

    private void show(JSONObject j) {
        state.setText(j.optInt("reads", 0) == 0
                ? "No verified reads yet. Open someone else\u2019s Learn post, scroll to the end and stay a few seconds until it says \u201cCounted as a read\u201d. Your own posts do not count."
                : "Your activity over the last 30 days.");
        tile(R.id.tile_reads, "Verified reads", String.valueOf(j.optInt("reads", 0)));
        tile(R.id.tile_days, "Active days (30)", String.valueOf(j.optInt("activeDays30", 0)));
        tile(R.id.tile_gifts, "Gifts sent", String.valueOf(j.optInt("giftsSent", 0)));

        note.setText(j.optBoolean("automated", false)
                ? "Your recent reading looks automated, so it is not adding to your trust score right now. Reading at natural times, at your own pace, counts again."
                : "Only reads the network verified are counted: you reached the end, stayed long enough, and read each post once. Up to 15 reads a day count.");
    }

    private View weekdayRow(String label, int value, int max) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));
        TextView name = new TextView(this);
        name.setText(label);
        name.setTextSize(13);
        name.setTextColor(ContextCompat.getColor(this, R.color.colorTextTertiary));
        row.addView(name, new LinearLayout.LayoutParams(dp(34), -2));
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(max);
        bar.setProgress(value);
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorAccent)));
        bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorSurfaceAlt)));
        row.addView(bar, new LinearLayout.LayoutParams(0, dp(10), 1f));
        TextView count = new TextView(this);
        count.setText(String.valueOf(value));
        count.setTextSize(13);
        count.setTextColor(ContextCompat.getColor(this, R.color.colorTextPrimary));
        count.setGravity(android.view.Gravity.END);
        row.addView(count, new LinearLayout.LayoutParams(dp(34), -2));
        return row;
    }

    private void fill(BarChart chart, List<BarEntry> entries, ValueFormatter xLabels) {
        BarDataSet set = new BarDataSet(entries, "");
        set.setColor(ContextCompat.getColor(this, R.color.colorAccent));
        set.setDrawValues(false);
        BarData data = new BarData(set);
        data.setBarWidth(0.6f);
        float top = 4f;
        for (BarEntry e : entries) top = Math.max(top, e.getY() + 1f);
        chart.getAxisLeft().setAxisMaximum(top);
        chart.setData(data);
        chart.getXAxis().setValueFormatter(xLabels);
        chart.getXAxis().setLabelCount(entries.size(), false);
        chart.invalidate();
    }

    private void styleChart(BarChart chart) {
        int text = ContextCompat.getColor(this, R.color.colorTextTertiary);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setNoDataText("");
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setTextColor(text);
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisLeft().setAxisMaximum(4f);
        chart.getAxisLeft().setGranularity(1f);
        chart.getAxisLeft().setGridColor(ContextCompat.getColor(this, R.color.colorCardStroke));
        chart.getAxisLeft().setDrawAxisLine(false);
        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setTextColor(text);
        x.setDrawGridLines(false);
        x.setDrawAxisLine(false);
    }

    private void tile(int includeId, String label, String value) {
        View t = findViewById(includeId);
        ((TextView) t.findViewById(R.id.tv_tile_label)).setText(label);
        ((TextView) t.findViewById(R.id.tv_tile_value)).setText(value);
    }

    private static int[] ints(JSONArray a, int size) {
        int[] out = new int[size];
        if (a != null) for (int i = 0; i < size && i < a.length(); i++) out[i] = a.optInt(i, 0);
        return out;
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
}
