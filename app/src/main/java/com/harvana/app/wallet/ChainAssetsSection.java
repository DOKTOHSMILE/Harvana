package com.harvana.app.wallet;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.harvana.app.ChainWalletsActivity;
import com.harvana.app.R;

import java.util.Map;

/**
 * The other-chain coins shown under HARV on the Assets tab. The user picks which coins to show with
 * "Add coins"; the choice is kept per device. Addresses come from the same recovery phrase.
 */
public final class ChainAssetsSection {
    private ChainAssetsSection() {}

    private static final String PREFS = "asset_chain_toggles";
    public static final String PI = "PI";

    public static boolean shown(Context c, String key) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(key, false);
    }

    private static void setShown(Context c, String key, boolean on) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(key, on).apply();
    }

    /** Opens the picker; {@code onChange} runs after any toggle so the list can redraw. */
    public static void showPicker(Context c, Runnable onChange) {
        BottomSheetDialog d = new BottomSheetDialog(c);
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 20);
        root.setPadding(p, p, p, p);
        TextView title = new TextView(c);
        title.setText("Add coins");
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(ContextCompat.getColor(c, R.color.colorTextPrimary));
        root.addView(title);
        TextView sub = new TextView(c);
        sub.setText("Turn a coin on to show it next to HARV. Every coin uses your same recovery phrase.");
        sub.setTextSize(13);
        sub.setTextColor(ContextCompat.getColor(c, R.color.colorTextSecondary));
        sub.setPadding(0, dp(c, 4), 0, dp(c, 12));
        root.addView(sub);
        root.addView(toggleRow(c, PI, "Pi Network", "Pi", onChange));
        for (ChainKeys.Chain ch : ChainKeys.Chain.values()) root.addView(toggleRow(c, ch.name(), ch.title, ch.ticker, onChange));
        d.setContentView(root);
        d.show();
    }

    private static View toggleRow(Context c, String key, String name, String ticker, Runnable onChange) {
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(c, 8), 0, dp(c, 8));
        row.addView(badge(c, ticker));
        TextView t = new TextView(c);
        t.setText(name);
        t.setTextSize(15);
        t.setTextColor(ContextCompat.getColor(c, R.color.colorTextPrimary));
        t.setPadding(dp(c, 12), 0, 0, 0);
        row.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        SwitchMaterial sw = new SwitchMaterial(c);
        sw.setChecked(shown(c, key));
        sw.setOnCheckedChangeListener((b, on) -> { setShown(c, key, on); if (onChange != null) onChange.run(); });
        row.addView(sw);
        row.setOnClickListener(v -> sw.toggle());
        return row;
    }

    /** Draws a row per shown coin into {@code box}, then fills balances in the background. */
    public static void render(Context c, LinearLayout box, String harvAddress) {
        if (box == null) return;
        box.removeAllViews();
        if (c == null || harvAddress == null || harvAddress.isEmpty()) return;
        final Context app = c.getApplicationContext();
        new Thread(() -> {
            Map<ChainKeys.Chain, String> addrs = ChainWalletStore.ensure(app, harvAddress);
            String pi = PiWalletStore.get(app, harvAddress);
            box.post(() -> {
                box.removeAllViews();
                if (shown(app, PI) && pi != null && !pi.isEmpty()) addRow(c, box, "Pi Network", "Pi", pi, null);
                for (Map.Entry<ChainKeys.Chain, String> e : addrs.entrySet())
                    if (shown(app, e.getKey().name())) addRow(c, box, e.getKey().title, e.getKey().ticker, e.getValue(), e.getKey());
            });
        }).start();
    }

    private static void addRow(Context c, LinearLayout box, String name, String ticker, String address, ChainKeys.Chain chain) {
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int p = dp(c, 16);
        row.setPadding(p, dp(c, 12), p, dp(c, 12));
        row.addView(badge(c, ticker));
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(c, 12), 0, 0, 0);
        TextView n = new TextView(c);
        n.setText(name);
        n.setTextSize(15);
        n.setTypeface(Typeface.DEFAULT_BOLD);
        n.setTextColor(ContextCompat.getColor(c, R.color.colorTextPrimary));
        TextView a = new TextView(c);
        a.setText(address.length() > 16 ? address.substring(0, 8) + "…" + address.substring(address.length() - 6) : address);
        a.setTextSize(12);
        a.setTextColor(ContextCompat.getColor(c, R.color.colorTextTertiary));
        col.addView(n);
        col.addView(a);
        row.addView(col, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView bal = new TextView(c);
        bal.setText("…");
        bal.setTextSize(13);
        bal.setGravity(Gravity.END);
        bal.setMaxWidth(dp(c, 160));
        bal.setTextColor(ContextCompat.getColor(c, R.color.colorTextPrimary));
        row.addView(bal);
        row.setBackgroundResource(android.R.drawable.list_selector_background);
        row.setOnClickListener(v -> c.startActivity(new Intent(c, ChainWalletsActivity.class)));
        box.addView(row);
        new Thread(() -> {
            String s = chain == null ? ChainBalances.pi(address) : ChainBalances.describe(chain, address);
            bal.post(() -> bal.setText(s));
        }).start();
    }

    private static TextView badge(Context c, String ticker) {
        TextView b = new TextView(c);
        int s = dp(c, 40);
        b.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        b.setGravity(Gravity.CENTER);
        b.setText(ticker.length() > 4 ? ticker.substring(0, 4) : ticker);
        b.setTextSize(11);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(ContextCompat.getColor(c, R.color.colorPrimary));
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(ContextCompat.getColor(c, R.color.colorPrimary) & 0x00FFFFFF | 0x26000000);
        b.setBackground(g);
        return b;
    }

    private static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }
}
