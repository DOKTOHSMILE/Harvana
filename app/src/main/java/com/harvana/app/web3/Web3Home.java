package com.harvana.app.web3;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.harvana.app.R;
import com.harvana.app.utils.UiColors;

import org.json.JSONArray;
import org.json.JSONObject;

/** The Earn tab as a Web3 home: search, Earn, dApps by category, recent sites. */
public final class Web3Home {

    private static final String[] TABS = {"Featured", "DEX", "Lending", "Yield", "NFT"};
    private static final String[][][] DAPPS = {
        { {"Uniswap", "https://app.uniswap.org"}, {"PancakeSwap", "https://pancakeswap.finance"}, {"Aave", "https://app.aave.com"},
          {"1inch", "https://app.1inch.io"}, {"Lido", "https://stake.lido.fi"}, {"OpenSea", "https://opensea.io"} },
        { {"Uniswap", "https://app.uniswap.org"}, {"PancakeSwap", "https://pancakeswap.finance"}, {"SushiSwap", "https://www.sushi.com/swap"},
          {"Curve", "https://curve.fi"}, {"QuickSwap", "https://quickswap.exchange"}, {"Aerodrome", "https://aerodrome.finance"} },
        { {"Aave", "https://app.aave.com"}, {"Compound", "https://app.compound.finance"}, {"Venus", "https://app.venus.io"},
          {"Spark", "https://app.spark.fi"} },
        { {"Lido", "https://stake.lido.fi"}, {"Yearn", "https://yearn.fi"}, {"Beefy", "https://app.beefy.com"},
          {"Convex", "https://curve.convexfinance.com"} },
        { {"OpenSea", "https://opensea.io"}, {"Blur", "https://blur.io"}, {"Magic Eden", "https://magiceden.io"} },
    };

    private final Context c;
    private final LinearLayout root;
    private final LinearLayout list, recent, tabs;
    private TextView staked;
    private int tab = 0;

    public Web3Home(Context c, Runnable openStaking, Runnable openMiner) {
        this.c = c;
        root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(24));

        root.addView(search());

        root.addView(heading("Earn"));
        LinearLayout earn = card();
        staked = new TextView(c);
        earn.addView(line(R.drawable.ic_harv_coin, "Staking", staked, openStaking));
        TextView mine = new TextView(c);
        mine.setText("Harvana Node Miner");
        earn.addView(line(R.drawable.ic_harv_coin, "Mining", mine, openMiner));
        root.addView(earn);
        setStaked("0 HARV");

        root.addView(heading("Explore dApps"));
        HorizontalScrollView hs = new HorizontalScrollView(c);
        hs.setHorizontalScrollBarEnabled(false);
        tabs = new LinearLayout(c);
        hs.addView(tabs);
        root.addView(hs);
        list = card();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        root.addView(list, lp);
        drawTabs();

        root.addView(heading("Recent"));
        recent = card();
        root.addView(recent);
        refreshRecent();
    }

    public View view() { return root; }

    public void setStaked(String text) { staked.setText(text); }

    public void refreshRecent() {
        recent.removeAllViews();
        JSONArray r = Web3Sites.recent(c);
        int n = Math.min(r.length(), 8);
        for (int i = 0; i < n; i++) {
            JSONObject o = r.optJSONObject(i);
            if (o == null) continue;
            String url = o.optString("url");
            String title = o.optString("title");
            Uri u = Uri.parse(url);
            recent.addView(site(title.isEmpty() ? String.valueOf(u.getHost()) : title, url));
        }
        if (n == 0) {
            TextView none = new TextView(c);
            none.setText("—");
            none.setTextColor(UiColors.textTertiary());
            none.setPadding(dp(12), dp(12), dp(12), dp(12));
            recent.addView(none);
        }
    }

    private void drawTabs() {
        tabs.removeAllViews();
        for (int i = 0; i < TABS.length; i++) {
            final int k = i;
            TextView t = new TextView(c);
            t.setText(TABS[i]);
            t.setTextSize(13);
            t.setTypeface(null, Typeface.BOLD);
            t.setPadding(dp(14), dp(7), dp(14), dp(7));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(16));
            bg.setColor(i == tab ? UiColors.accent() : UiColors.surfaceAlt());
            t.setBackground(bg);
            t.setTextColor(i == tab ? UiColors.onAccent() : UiColors.textPrimary());
            t.setOnClickListener(v -> { tab = k; drawTabs(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8);
            tabs.addView(t, lp);
        }
        list.removeAllViews();
        for (String[] d : DAPPS[tab]) list.addView(site(d[0], d[1]));
    }

    private View search() {
        LinearLayout box = new LinearLayout(c);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(12), 0, dp(12), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(24));
        bg.setColor(UiColors.surfaceAlt());
        box.setBackground(bg);
        ImageView ic = new ImageView(c);
        ic.setImageResource(R.drawable.icon_search_24);
        ic.setColorFilter(UiColors.textSecondary());
        box.addView(ic, new LinearLayout.LayoutParams(dp(24), dp(24)));
        EditText q = new EditText(c);
        q.setHint("Search or enter dApp URL");
        q.setSingleLine(true);
        q.setTextSize(14);
        q.setBackground(null);
        q.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        q.setImeOptions(EditorInfo.IME_ACTION_GO);
        q.setOnEditorActionListener((v, id, ev) -> {
            String t = q.getText().toString().trim();
            if (t.isEmpty()) return false;
            open(t);
            q.setText("");
            return true;
        });
        box.addView(q, new LinearLayout.LayoutParams(0, dp(48), 1f));
        return box;
    }

    private View site(String name, String url) {
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        ImageView ic = new ImageView(c);
        String host = Uri.parse(url).getHost();
        Glide.with(c).load("https://icons.duckduckgo.com/ip3/" + host + ".ico")
                .placeholder(R.drawable.icon_search_24).error(R.drawable.icon_search_24).circleCrop().into(ic);
        row.addView(ic, new LinearLayout.LayoutParams(dp(36), dp(36)));
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(12), 0, 0, 0);
        TextView n = new TextView(c);
        n.setText(name);
        n.setTextSize(15);
        n.setTypeface(null, Typeface.BOLD);
        n.setTextColor(UiColors.textPrimary());
        n.setSingleLine(true);
        TextView h = new TextView(c);
        h.setText(host);
        h.setTextSize(12);
        h.setTextColor(UiColors.textSecondary());
        h.setSingleLine(true);
        col.addView(n);
        col.addView(h);
        row.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.setOnClickListener(v -> open(url));
        return row;
    }

    private View line(int icon, String label, TextView value, Runnable onClick) {
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        ImageView ic = new ImageView(c);
        ic.setImageResource(icon);
        row.addView(ic, new LinearLayout.LayoutParams(dp(32), dp(32)));
        TextView l = new TextView(c);
        l.setText(label);
        l.setTextSize(15);
        l.setTypeface(null, Typeface.BOLD);
        l.setTextColor(UiColors.textPrimary());
        l.setPadding(dp(12), 0, 0, 0);
        row.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        value.setTextSize(14);
        value.setTextColor(UiColors.textSecondary());
        row.addView(value);
        row.setOnClickListener(v -> { if (onClick != null) onClick.run(); });
        return row;
    }

    private void open(String text) {
        Intent i = new Intent(c, DappBrowserActivity.class).putExtra(DappBrowserActivity.EXTRA_URL, text);
        c.startActivity(i);
    }

    private TextView heading(String t) {
        TextView v = new TextView(c);
        v.setText(t);
        v.setTextSize(17);
        v.setTypeface(null, Typeface.BOLD);
        v.setTextColor(UiColors.textPrimary());
        v.setPadding(0, dp(20), 0, dp(8));
        return v;
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(UiColors.surface());
        bg.setStroke(dp(1), UiColors.surfaceAlt());
        l.setBackground(bg);
        return l;
    }

    private int dp(int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }
}
