package com.harvana.app.learn;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.harvana.app.utils.UiColors;

import java.util.HashMap;
import java.util.Map;

/**
 * A video player for a Learn Hub article that stays out of the way until the reader presses play. Before that it is only a picture (the
 * video's thumbnail when the platform gives one) with a play button, so nothing is downloaded and no web page is loaded. On play it
 * opens the platform's own embeddable player (YouTube, Facebook, TikTok, Vimeo) in a WebView: the video streams straight from the
 * platform and is not stored or re-hosted. A link under it opens the video in the platform's own app if the author has switched
 * embedding off.
 */
public class VideoPlayerView extends LinearLayout {
    private static final String REFERER = "https://harvannaprotocol.com/";

    private final Activity host;
    private final VideoLinks.Video video;
    private final FrameLayout stage;
    private WebView web;
    private View customView;
    private WebChromeClient.CustomViewCallback customCallback;
    private Application.ActivityLifecycleCallbacks lifecycle;

    public VideoPlayerView(Activity host, VideoLinks.Video video) {
        super(host);
        this.host = host;
        this.video = video;
        setOrientation(VERTICAL);
        float d = getResources().getDisplayMetrics().density;

        stage = new FrameLayout(host);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF0B0F14);
        bg.setCornerRadius(12 * d);
        stage.setBackground(bg);
        stage.setClipToOutline(true);
        boolean tall = video.platform == VideoLinks.Platform.TIKTOK;
        int height = tall ? (int) (560 * d) : -1;          // wide videos get 16:9 once the width is known
        addView(stage, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height > 0 ? height : (int) (200 * d)));
        if (!tall) {
            stage.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
                int w = r - l;
                int want = w * 9 / 16;
                if (w > 0 && Math.abs(stage.getLayoutParams().height - want) > 2) {
                    stage.post(() -> { ViewGroup.LayoutParams lp = stage.getLayoutParams(); lp.height = want; stage.setLayoutParams(lp); });
                }
            });
        }
        showCover();

        TextView open = new TextView(host);
        open.setText("Can't play it here? Open it in " + video.platform.label + "  ›");
        open.setTextColor(UiColors.accent());
        open.setTextSize(12.5f);
        open.setPadding(0, (int) (8 * d), 0, 0);
        open.setOnClickListener(v -> openOutside(video.link));
        addView(open);

        watchLifecycle();
    }

    // ── Before play ──────────────────────────────────────────────────────

    private void showCover() {
        stage.removeAllViews();
        float d = getResources().getDisplayMetrics().density;
        if (!video.thumbnail.isEmpty()) {
            ImageView pic = new ImageView(host);
            pic.setScaleType(ImageView.ScaleType.CENTER_CROP);
            stage.addView(pic, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            try { Glide.with(host).load(video.thumbnail).into(pic); } catch (Exception ignored) { }
        }
        TextView play = new TextView(host);
        play.setText("▶");
        play.setTextColor(Color.WHITE);
        play.setTextSize(26f);
        play.setGravity(Gravity.CENTER);
        play.setPadding((int) (6 * d), 0, 0, 0);
        GradientDrawable disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        disc.setColor(0xCC000000);
        disc.setStroke((int) (2 * d), 0xFFFFFFFF);
        play.setBackground(disc);
        stage.addView(play, new FrameLayout.LayoutParams((int) (68 * d), (int) (68 * d), Gravity.CENTER));

        TextView chip = new TextView(host);
        chip.setText(video.platform.label);
        chip.setTextColor(Color.WHITE);
        chip.setTextSize(11f);
        chip.setTypeface(null, android.graphics.Typeface.BOLD);
        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setColor(0xAA000000);
        chipBg.setCornerRadius(10 * d);
        chip.setBackground(chipBg);
        chip.setPadding((int) (10 * d), (int) (4 * d), (int) (10 * d), (int) (4 * d));
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.BOTTOM);
        cp.setMargins((int) (10 * d), 0, 0, (int) (10 * d));
        stage.addView(chip, cp);
        stage.setOnClickListener(v -> startPlayback());
    }

    // ── Playing ──────────────────────────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    private void startPlayback() {
        if (web != null) return;
        stage.setOnClickListener(null);
        stage.removeAllViews();
        web = new WebView(host);
        web.setBackgroundColor(Color.BLACK);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);                     // the platforms' players are scripts
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);     // the reader has just pressed play
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setSupportMultipleWindows(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (stayInPlayer(url)) return false;
                openOutside(url);                          // a tap on a link inside the player leaves the article, in the browser
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onShowCustomView(View view, CustomViewCallback callback) { enterFullscreen(view, callback); }
            @Override public void onHideCustomView() { leaveFullscreen(); }
        });
        stage.addView(web, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        Map<String, String> headers = new HashMap<>();
        headers.put("Referer", REFERER);                  // YouTube refuses to play inside a page that does not say where it came from
        web.loadUrl(video.embedUrl, headers);
    }

    /** The player's own pages stay inside; anything else is a link out. */
    private boolean stayInPlayer(String url) {
        Uri u = Uri.parse(url);
        String host = u.getHost() == null ? "" : u.getHost().toLowerCase();
        switch (video.platform) {
            case YOUTUBE: return host.endsWith("youtube.com") && u.getPath() != null && u.getPath().startsWith("/embed/")
                    || host.endsWith("youtube-nocookie.com") && u.getPath() != null && u.getPath().startsWith("/embed/");
            case VIMEO: return host.equals("player.vimeo.com");
            case TIKTOK: return host.endsWith("tiktok.com") && u.getPath() != null && u.getPath().startsWith("/embed/");
            case FACEBOOK: return host.endsWith("facebook.com") && u.getPath() != null && u.getPath().startsWith("/plugins/");
            default: return false;
        }
    }

    private void openOutside(String url) {
        try { host.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) { }
    }

    // ── Full screen ──────────────────────────────────────────────────────

    private void enterFullscreen(View view, WebChromeClient.CustomViewCallback callback) {
        if (customView != null) { callback.onCustomViewHidden(); return; }
        customView = view;
        customCallback = callback;
        ViewGroup decor = (ViewGroup) host.getWindow().getDecorView();
        decor.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        view.setBackgroundColor(Color.BLACK);
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private void leaveFullscreen() {
        if (customView == null) return;
        ViewGroup decor = (ViewGroup) host.getWindow().getDecorView();
        decor.removeView(customView);
        decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        customView = null;
        if (customCallback != null) customCallback.onCustomViewHidden();
        customCallback = null;
    }

    // ── Lifecycle: stop the sound when the screen is left, free the player when it closes ──

    private void watchLifecycle() {
        lifecycle = new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityPaused(Activity a) { if (a == host && web != null) web.onPause(); }
            @Override public void onActivityResumed(Activity a) { if (a == host && web != null) web.onResume(); }
            @Override public void onActivityDestroyed(Activity a) {
                if (a != host) return;
                leaveFullscreen();
                if (web != null) { stage.removeAllViews(); web.destroy(); web = null; }
                host.getApplication().unregisterActivityLifecycleCallbacks(this);
            }
            @Override public void onActivityCreated(Activity a, Bundle b) { }
            @Override public void onActivityStarted(Activity a) { }
            @Override public void onActivityStopped(Activity a) { }
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }
        };
        host.getApplication().registerActivityLifecycleCallbacks(lifecycle);
    }
}
