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

    public static final String SWAP_URL = "https://harvannaprotocol.com/swap/";
    public static final String STAKE_URL = "https://harvannaprotocol.com/stake/";
    public static final String EXPLORER_URL = "https://harvannaprotocol.com/explorer/";

    private static final String[] TABS = {"Harvana", "Featured", "DEX", "Lending", "Yield", "NFT"};
    private static final String[][][] DAPPS = {
        { {"Harvana Swap", SWAP_URL}, {"Harvana Staking", STAKE_URL}, {"Harvana Explorer", EXPLORER_URL},
          {"Harvana Pools", SWAP_URL + "#pools"}, {"List a coin", SWAP_URL + "#list"} },
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

    /** The Discover page: no Earn section. */
    public Web3Home(Context c) { this(c, null, null, false); }

    public Web3Home(Context c, Runnable openStaking, Runnable openMiner) { this(c, openStaking, openMiner, true); }

    private Web3Home(Context c, Runnable openStaking, Runnable openMiner, boolean showEarn) {
        this.c = c;
        root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(24));

        // Search, and the open browser tabs beside it (tap to go back to them).
        LinearLayout top = new LinearLayout(c);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(search(), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tabsBtn = new TextView(c);
        tabsBtn.setBackgroundResource(R.drawable.bg_tab_count);
        tabsBtn.setGravity(Gravity.CENTER);
        tabsBtn.setTextSize(12);
        tabsBtn.setTypeface(null, Typeface.BOLD);
        tabsBtn.setTextColor(UiColors.textPrimary());
        tabsBtn.setContentDescription("Open tabs");
        tabsBtn.setOnClickListener(v -> openTabs(c));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(dp(28), dp(28));
        tlp.leftMargin = dp(12);
        tlp.rightMargin = dp(4);
        top.addView(tabsBtn, tlp);
        root.addView(top);

        staked = new TextView(c);
        if (showEarn) {
            root.addView(heading("Earn"));
            LinearLayout earn = card();
            earn.addView(line(R.drawable.ic_app_stake, "Staking", staked, openStaking != null ? openStaking : () -> open(STAKE_URL)));
            TextView pools = new TextView(c);
            pools.setText("Harvana Swap");
            earn.addView(line(R.drawable.ic_app_liquidity, "Liquidity", pools, () -> open(SWAP_URL + "#liquidity")));
            TextView mine = new TextView(c);
            mine.setText("Harvana Node Miner");
            earn.addView(line(R.drawable.ic_app_mining, "Mining", mine, openMiner));
            root.addView(earn);
        }
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

    private TextView tabsBtn;

    /** The browser with its open tabs, as it was left. */
    public static void openTabs(Context c) {
        c.startActivity(new Intent(c, DappBrowserActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
    }

    public void refreshRecent() {
        int n0 = BrowserTabs.count(c);
        tabsBtn.setText(String.valueOf(n0));
        tabsBtn.setVisibility(n0 > 0 ? View.VISIBLE : View.GONE);
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
        if ("harvannaprotocol.com".equals(host)) ic.setImageResource(iconFor(url));
        else Glide.with(c).load("https://icons.duckduckgo.com/ip3/" + host + ".ico")
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

    private void open(String text) { open(c, text); }

    /** Each Harvana site and section has its own icon; any other Harvana page shows the HARV coin. */
    public static int iconFor(String url) {
        if (url == null) return R.drawable.ic_harv_coin;
        if (url.startsWith(SWAP_URL)) {
            if (url.contains("#pools")) return R.drawable.ic_app_pools;
            if (url.contains("#list")) return R.drawable.ic_app_list;
            if (url.contains("#liquidity")) return R.drawable.ic_app_liquidity;
            return R.drawable.ic_app_swap;
        }
        if (url.startsWith(STAKE_URL)) return R.drawable.ic_app_stake;
        if (url.startsWith(EXPLORER_URL)) return R.drawable.ic_app_explorer;
        return R.drawable.ic_harv_coin;
    }

    /** Opens a site: the Harvana explorer in the explorer, everything else in the dApp browser. */
    public static void open(Context c, String text) {
        if (text != null && text.startsWith(EXPLORER_URL)) {
            c.startActivity(new Intent(c, com.harvana.app.ExplorerActivity.class)
                    .putExtra(com.harvana.app.wallet.CoinCatalog.EXTRA, "HARV")
                    .putExtra(com.harvana.app.ExplorerActivity.EXTRA_URL, text));
            return;
        }
        c.startActivity(new Intent(c, DappBrowserActivity.class).putExtra(DappBrowserActivity.EXTRA_URL, text)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));                    // the open browser, in a new tab
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
