package com.harvana.app.web3;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
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
        root = (LinearLayout) LayoutInflater.from(c).inflate(R.layout.view_web3_home, null, false);
        tabsBtn = root.findViewById(R.id.tv_web3_tabs);
        tabsBtn.setOnClickListener(v -> openTabs(c));
        EditText q = root.findViewById(R.id.et_web3_search);
        q.setOnEditorActionListener((v, id, ev) -> {
            String t = q.getText().toString().trim();
            if (t.isEmpty()) return false;
            open(t);
            q.setText("");
            return true;
        });

        staked = root.findViewById(R.id.tv_web3_staked);
        if (showEarn) {
            root.findViewById(R.id.web3_earn_section).setVisibility(View.VISIBLE);
            root.findViewById(R.id.row_web3_staking).setOnClickListener(v -> { if (openStaking != null) openStaking.run(); else open(STAKE_URL); });
            root.findViewById(R.id.row_web3_liquidity).setOnClickListener(v -> open(SWAP_URL + "#liquidity"));
            root.findViewById(R.id.row_web3_mining).setOnClickListener(v -> { if (openMiner != null) openMiner.run(); });
        }
        setStaked("0 HARV");

        tabs = root.findViewById(R.id.web3_chips);
        list = root.findViewById(R.id.web3_dapps);
        drawTabs();
        recent = root.findViewById(R.id.web3_recent);
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
            TextView none = (TextView) LayoutInflater.from(c).inflate(R.layout.item_list_empty, recent, false);
            none.setText("—");
            none.setPadding(dp(12), dp(12), dp(12), dp(12));
            recent.addView(none);
        }
    }

    private void drawTabs() {
        tabs.removeAllViews();
        for (int i = 0; i < TABS.length; i++) {
            final int k = i;
            TextView t = (TextView) LayoutInflater.from(c).inflate(R.layout.item_web3_chip, tabs, false);
            t.setText(TABS[i]);
            t.setBackgroundResource(i == tab ? R.drawable.bg_pill_accent : R.drawable.bg_pill_surface_alt);
            t.setTextColor(i == tab ? UiColors.onAccent() : UiColors.textPrimary());
            t.setOnClickListener(v -> { tab = k; drawTabs(); });
            tabs.addView(t);
        }
        list.removeAllViews();
        for (String[] d : DAPPS[tab]) list.addView(site(d[0], d[1]));
    }

    /** One site: res/layout/item_web3_site.xml. */
    private View site(String name, String url) {
        View row = LayoutInflater.from(c).inflate(R.layout.item_web3_site, null, false);
        ImageView ic = row.findViewById(R.id.iv_web3_site_icon);
        String host = Uri.parse(url).getHost();
        if ("harvannaprotocol.com".equals(host)) ic.setImageResource(iconFor(url));
        else Glide.with(c).load("https://icons.duckduckgo.com/ip3/" + host + ".ico")
                .placeholder(R.drawable.icon_search_24).error(R.drawable.icon_search_24).circleCrop().into(ic);
        ((TextView) row.findViewById(R.id.tv_web3_site_name)).setText(name);
        ((TextView) row.findViewById(R.id.tv_web3_site_host)).setText(host);
        row.setOnClickListener(v -> open(url));
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

    private int dp(int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }
}
