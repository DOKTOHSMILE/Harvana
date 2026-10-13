package com.harvana.app;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.harvana.app.utils.AppBar;
import com.harvana.app.wallet.CoinCatalog;

/**
 * One explorer page for every coin. Coins that already have a public explorer open it here.
 * Harvana opens here too, once {@link CoinCatalog#HARV_EXPLORER} is filled in.
 */
public class ExplorerActivity extends AppCompatActivity {
    /** Opens this page of the Harvana explorer instead of the wallet's address page. */
    public static final String EXTRA_URL = "url";
    private WebView web;
    private com.harvana.app.web3.FindBar find;

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
            AppBar.setContent(this, R.layout.activity_explorer_waiting, coin.name + " explorer", coin.liveNetwork());
            ((ImageView) findViewById(R.id.iv_explorer_coin)).setImageResource(coin.icon);
            ((TextView) findViewById(R.id.tv_explorer_coin)).setText(coin.name);
            if (!address.isEmpty()) {
                TextView addr = findViewById(R.id.tv_explorer_address);
                addr.setText(address);
                addr.setVisibility(android.view.View.VISIBLE);
            }
            return;
        }
        androidx.appcompat.widget.Toolbar bar = AppBar.setContent(this, R.layout.activity_explorer, coin.name + " explorer", host(url));
        bar.setNavigationOnClickListener(v -> finish());
        web = findViewById(R.id.explorer_web);
        android.webkit.WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(false);
        ws.setAllowContentAccess(false);
        ws.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        ws.setSupportMultipleWindows(false);
        ws.setJavaScriptCanOpenWindowsAutomatically(false);
        com.harvana.app.web3.BrowserPrefs.apply(this, web);           // zoom, desktop site, images, cookies, Safe Browsing

        final String home = url;
        android.widget.ProgressBar progress = findViewById(R.id.explorer_progress);
        com.harvana.app.web3.FindBar findBar = new com.harvana.app.web3.FindBar(this, web);
        find = findBar;
        com.harvana.app.web3.BrowserMenu menu = new com.harvana.app.web3.BrowserMenu(this, new com.harvana.app.web3.BrowserMenu.Host() {
            @Override public WebView web() { return web; }
            @Override public void openUrl(String u) {
                String t = u == null ? "" : u.trim();
                if (t.startsWith("http://")) t = "https://" + t.substring(7);
                if (t.startsWith("https://")) web.loadUrl(t);
            }
            @Override public void find() { findBar.show(); }
            @Override public void home() { web.loadUrl(home); }
            @Override public boolean hasWallet() { return false; }
            @Override public java.util.List<String> pageConnections() { return new java.util.ArrayList<>(); }
            @Override public void disconnectPage() { }
            @Override public java.util.List<String> connectedSites() { return new java.util.ArrayList<>(); }
            @Override public void disconnectSite(String origin) { }
        });
        menu.attachLinkMenu();
        menu.attachDownloads();
        com.harvana.app.web3.BrowserBar nav = new com.harvana.app.web3.BrowserBar(this, web, home, null, menu::show);

        ((android.widget.FrameLayout) findViewById(R.id.explorer_find_bar)).addView(findBar.view());
        ((android.widget.FrameLayout) findViewById(R.id.explorer_nav_bar)).addView(nav.view());

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
                com.harvana.app.web3.BrowserPrefs.addHistory(ExplorerActivity.this, u, view.getTitle());
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
        if (find != null && find.isShown()) { find.hide(); return; }
        goBack();
    }

    @Override protected void onPause() { super.onPause(); if (web != null) web.onPause(); }
    @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); }

    @Override
    protected void onDestroy() {
        if (web != null) { web.destroy(); web = null; }
        super.onDestroy();
    }

    private static String host(String url) {
        try { return android.net.Uri.parse(url).getHost(); } catch (Exception e) { return ""; }
    }
}
