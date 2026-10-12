package com.harvana.app.web3;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.harvana.app.R;
import com.harvana.app.utils.UiColors;

/** Browser buttons under a WebView: back, forward, reload (stop while loading), home, and the menu (or share, long press copies). */
public final class BrowserBar {

    private final LinearLayout bar;
    private final ImageView back, forward, reload;
    private final WebView web;
    private boolean loading;

    public BrowserBar(Context c, WebView web, String homeUrl, Runnable onHome) { this(c, web, homeUrl, onHome, null); }

    /** With {@code onMenu}, the last button opens the browser menu instead of sharing. */
    public BrowserBar(Context c, WebView web, String homeUrl, Runnable onHome, Runnable onMenu) {
        this.web = web;
        bar = new LinearLayout(c);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(UiColors.surface());
        int p = dp(c, 4);
        bar.setPadding(p, p, p, p);
        back = button(c, R.drawable.ic_arrow_back_line, "Back");
        forward = button(c, R.drawable.icon_arrow_forward_round_line, "Forward");
        reload = button(c, R.drawable.ic_refresh_line, "Reload");
        ImageView home = button(c, R.drawable.icon_house_outline_line, "Home");
        ImageView share = button(c, onMenu != null ? R.drawable.ic_more_vert_line : R.drawable.icon_share_round_line, onMenu != null ? "Menu" : "Share");
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
            Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url);
            try { c.startActivity(Intent.createChooser(i, "Share")); } catch (Exception ignored) { }
        });
        share.setOnLongClickListener(v -> {
            String url = web.getUrl();
            if (url == null) return true;
            ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Link", url));
            Toast.makeText(c, "Link copied", Toast.LENGTH_SHORT).show();
            return true;
        });
        for (ImageView b : new ImageView[]{back, forward, reload, home, share}) {
            bar.addView(b, new LinearLayout.LayoutParams(0, dp(c, 44), 1f));
        }
        update();
    }

    public LinearLayout view() { return bar; }

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

    private static ImageView button(Context c, int icon, String label) {
        ImageView iv = new ImageView(c);
        iv.setImageResource(icon);
        iv.setContentDescription(label);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);                  // every icon drawn at 24dp, whatever its own size
        int pad = dp(c, 10);
        iv.setPadding(pad, pad, pad, pad);
        iv.setColorFilter(UiColors.textPrimary());
        android.util.TypedValue tv = new android.util.TypedValue();
        if (c.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true)) iv.setBackgroundResource(tv.resourceId);
        iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return iv;
    }

    private static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }
}
