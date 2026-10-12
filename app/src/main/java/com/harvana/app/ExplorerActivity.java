package com.harvana.app;

import android.os.Bundle;
import android.view.Gravity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.harvana.app.utils.AppBar;
import com.harvana.app.utils.UiColors;
import com.harvana.app.wallet.CoinCatalog;

/**
 * One explorer page for every coin. Coins that already have a public explorer open it here.
 * Harvana opens here too, once {@link CoinCatalog#HARV_EXPLORER} is filled in.
 */
public class ExplorerActivity extends AppCompatActivity {
    /** Opens this page of the Harvana explorer instead of the wallet's address page. */
    public static final String EXTRA_URL = "url";
    private WebView web;

    /** The Harvana explorer's page for one transaction. */
    public static void openTx(android.content.Context c, String hash) {
        if (hash == null || hash.isEmpty()) return;
        c.startActivity(new android.content.Intent(c, ExplorerActivity.class)
                .putExtra(CoinCatalog.EXTRA, "HARV")
                .putExtra(EXTRA_URL, "https://harvannaprotocol.com/explorer/#/tx/" + hash));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CoinCatalog.Coin coin = CoinCatalog.get(getIntent().getStringExtra(CoinCatalog.EXTRA));
        String address = getIntent().getStringExtra("walletAddress");
        if (address == null) address = "";
        String url = CoinCatalog.explorerUrl(coin, address);
        String direct = getIntent().getStringExtra(EXTRA_URL);           // a page of the Harvana explorer (a transaction)
        if (direct != null && direct.startsWith("https://harvannaprotocol.com/explorer/")) url = direct;
        if (url.isEmpty()) {
            AppBar.setContent(this, waiting(coin, address), coin.name + " explorer", coin.liveNetwork());
            return;
        }
        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, String next) {
                if (next == null) return true;
                String low = next.toLowerCase(java.util.Locale.US);
                return !(low.startsWith("https://") || low.startsWith("http://"));
            }
        });
        androidx.appcompat.widget.Toolbar bar = AppBar.setContent(this, web, coin.name + " explorer", host(url));
        bar.setNavigationOnClickListener(v -> goBack());
        web.loadUrl(url);
    }

    private void goBack() {
        if (web != null && web.canGoBack()) web.goBack();
        else finish();
    }

    @Override
    public void onBackPressed() {
        goBack();
    }

    @Override
    protected void onDestroy() {
        if (web != null) { web.destroy(); web = null; }
        super.onDestroy();
    }

    private LinearLayout waiting(CoinCatalog.Coin coin, String address) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        int p = dp(24);
        box.setPadding(p, p, p, p);
        box.setBackgroundColor(UiColors.background());
        ImageView logo = new ImageView(this);
        int s = dp(72);
        logo.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        logo.setImageResource(coin.icon);
        box.addView(logo);
        TextView title = text(coin.name, 20, true, UiColors.textPrimary());
        title.setPadding(0, dp(16), 0, 0);
        box.addView(title);
        box.addView(text("The Harvana explorer website isn't linked yet. This page is ready for it. When the site is up, add its address in the coin list and it will open here.", 14, false, UiColors.textSecondary()));
        if (!address.isEmpty()) {
            TextView addr = text(address, 13, false, UiColors.textPrimary());
            addr.setPadding(0, dp(16), 0, 0);
            box.addView(addr);
        }
        return box;
    }

    private TextView text(String value, float size, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(8), 0, 0);
        return t;
    }

    private static String host(String url) {
        try { return android.net.Uri.parse(url).getHost(); } catch (Exception e) { return ""; }
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
