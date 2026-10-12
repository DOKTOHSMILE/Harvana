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
        android.webkit.WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(false);
        ws.setAllowContentAccess(false);
        ws.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        ws.setSupportMultipleWindows(false);
        ws.setJavaScriptCanOpenWindowsAutomatically(false);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);
        if (android.os.Build.VERSION.SDK_INT >= 26) ws.setSafeBrowsingEnabled(true);

        final String home = url;
        android.widget.ProgressBar progress = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        com.harvana.app.web3.BrowserBar nav = new com.harvana.app.web3.BrowserBar(this, web, home, null);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setBackgroundColor(UiColors.background());
        column.addView(progress, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(3)));
        column.addView(web, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        android.view.View rule = new android.view.View(this);
        rule.setBackgroundColor(UiColors.surfaceAlt());
        column.addView(rule, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, dp(1) / 2)));
        column.addView(nav.view(), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        androidx.appcompat.widget.Toolbar bar = AppBar.setContent(this, column, coin.name + " explorer", host(url));
        bar.setNavigationOnClickListener(v -> finish());

        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest req) {
                android.net.Uri u = req.getUrl();
                String scheme = u == null ? null : u.getScheme();
                if ("https".equalsIgnoreCase(scheme)) return false;
                if ("http".equalsIgnoreCase(scheme)) { view.loadUrl("https://" + u.toString().substring(7)); return true; }
                return true;                                                    // other schemes stay out
            }
            @Override public void onPageStarted(WebView view, String u, android.graphics.Bitmap icon) {
                nav.setLoading(true);
                bar.setSubtitle(host(u));
            }
            @Override public void onPageFinished(WebView view, String u) {
                nav.setLoading(false);
                bar.setSubtitle(host(u));
            }
            @Override public void doUpdateVisitedHistory(WebView view, String u, boolean isReload) {
                nav.update();                                                   // the explorer moves by #/… links
            }
            @Override public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler h, android.net.http.SslError e) {
                h.cancel();
            }
        });
        web.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int p) {
                progress.setProgress(p);
                progress.setVisibility(p >= 100 ? android.view.View.INVISIBLE : android.view.View.VISIBLE);
            }
            @Override public void onReceivedTitle(WebView view, String title) {
                if (title != null && !title.isEmpty() && !title.startsWith("http")) bar.setTitle(title);
            }
            @Override public void onPermissionRequest(android.webkit.PermissionRequest r) { r.deny(); }
        });
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
