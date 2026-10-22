package com.harvana.app.web3;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.harvana.app.R;

/** Browser buttons under a WebView: back, forward, reload (stop while loading), home, and the menu (or share, long press copies). */
public final class BrowserBar {

    private final LinearLayout bar;
    private final ImageView back, forward, reload;
    private WebView web;
    private boolean loading;

    public BrowserBar(Context c, WebView web, String homeUrl, Runnable onHome) { this(c, web, homeUrl, onHome, null); }

    /** With {@code onMenu}, the last button opens the browser menu instead of sharing. */
    public BrowserBar(Context c, WebView first, String homeUrl, Runnable onHome, Runnable onMenu) {
        this.web = first;
        bar = (LinearLayout) android.view.LayoutInflater.from(c).inflate(R.layout.view_browser_bar, null, false);
        back = bar.findViewById(R.id.browser_bar_back);
        forward = bar.findViewById(R.id.browser_bar_forward);
        reload = bar.findViewById(R.id.browser_bar_reload);
        ImageView home = bar.findViewById(R.id.browser_bar_home);
        ImageView share = bar.findViewById(R.id.browser_bar_more);
        if (onMenu != null) { share.setImageResource(R.drawable.ic_more_vert_line); share.setContentDescription("Menu"); }
        back.setOnClickListener(v -> { if (web.canGoBack()) web.goBack(); });
        forward.setOnClickListener(v -> { if (web.canGoForward()) web.goForward(); });
        reload.setOnClickListener(v -> { if (loading) web.stopLoading(); else web.reload(); });
        home.setOnClickListener(v -> {
            if (onHome != null) onHome.run();
            else if (homeUrl != null) web.loadUrl(homeUrl);
        });
        share.setOnClickListener(v -> {
            if (onMenu != null) { onMenu.run(); return; }
            String url = web.getUrl();
            if (url == null) return;
            com.harvana.app.share.ShareCenter.text(c, "Share", url);
        });
        share.setOnLongClickListener(v -> {
            String url = web.getUrl();
            if (url == null) return true;
            ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Link", url));
            Toast.makeText(c, "Link copied", Toast.LENGTH_SHORT).show();
            return true;
        });
        update();
    }

    public LinearLayout view() { return bar; }

    /** The page these buttons act on (the browser's open tab). */
    public void setWeb(WebView w) { web = w; update(); }

    /** Call when a page starts or finishes, and when history changes. */
    public void setLoading(boolean on) {
        loading = on;
        reload.setImageResource(on ? R.drawable.ic_dismiss_circle_line : R.drawable.ic_refresh_line);
        reload.setContentDescription(on ? "Stop" : "Reload");
        update();
    }

    public void update() {
        back.setAlpha(web.canGoBack() ? 1f : 0.3f);
        forward.setAlpha(web.canGoForward() ? 1f : 0.3f);
    }
}
