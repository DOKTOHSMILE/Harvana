package com.harvana.app.web3;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.harvana.app.R;
import com.harvana.app.ui.AppSheet;
import com.harvana.app.utils.InfoTable;
import com.harvana.app.utils.UiColors;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** The browser menu and its sheets: bookmarks, history, find, zoom, desktop site, page info, sites, clear data, settings. */
public final class BrowserMenu {

    /** What the menu needs from the browser screen. */
    public interface Host {
        WebView web();
        /** Navigates through the screen's own rules (https only). */
        void openUrl(String url);
        void find();
        void home();
        /** True when this browser has the wallet (Connected sites, Wallet row). */
        boolean hasWallet();
        /** Wallet accounts this page is connected with ("Ethereum · 0x12…", "Harvana · HV0x…"); empty when none. */
        List<String> pageConnections();
        void disconnectPage();
        /** Every connected site, "origin". */
        List<String> connectedSites();
        void disconnectSite(String origin);
    }

    private final Context c;
    private final Host host;

    public BrowserMenu(Context c, Host host) { this.c = c; this.host = host; }

    // ── the menu ──────────────────────────────────────────────────────────────

    public void show() {
        WebView web = host.web();
        String url = web.getUrl() == null ? "" : web.getUrl();
        String title = web.getTitle() == null ? "" : web.getTitle();
        AppSheet sheet = new AppSheet(c);
        LinearLayout box = column();

        LinearLayout head = new LinearLayout(c);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(0, 0, 0, dp(12));
        LinearLayout names = new LinearLayout(c);
        names.setOrientation(LinearLayout.VERTICAL);
        names.addView(text(title.isEmpty() ? host(url) : title, 16, true, UiColors.textPrimary(), 1));
        names.addView(text(url.isEmpty() ? "—" : url, 12, false, UiColors.textSecondary(), 1));
        head.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ImageView star = new ImageView(c);
        boolean saved = BrowserPrefs.isBookmarked(c, url);
        star.setImageResource(saved ? R.drawable.ic_br_bookmark_on : R.drawable.ic_br_bookmark);
        star.setColorFilter(saved ? UiColors.accent() : UiColors.textPrimary());
        star.setScaleType(ImageView.ScaleType.FIT_CENTER);
        star.setPadding(dp(10), dp(10), dp(10), dp(10));
        star.setContentDescription("Bookmark");
        star.setOnClickListener(v -> {
            if (url.isEmpty()) return;
            boolean now = BrowserPrefs.toggleBookmark(c, url, title);
            star.setImageResource(now ? R.drawable.ic_br_bookmark_on : R.drawable.ic_br_bookmark);
            star.setColorFilter(now ? UiColors.accent() : UiColors.textPrimary());
            toast(now ? "Bookmarked" : "Bookmark removed");
        });
        head.addView(star, new LinearLayout.LayoutParams(dp(44), dp(44)));
        box.addView(head);
        box.addView(rule());

        GridLayout grid = new GridLayout(c);
        grid.setColumnCount(4);
        grid.setPadding(0, dp(8), 0, 0);
        boolean desktop = BrowserPrefs.isDesktop(web);
        tile(grid, R.drawable.ic_br_bookmark, "Bookmarks", false, () -> { sheet.dismiss(); bookmarks(); });
        tile(grid, R.drawable.icon_history_twotone_line, "History", false, () -> { sheet.dismiss(); history(); });
        tile(grid, R.drawable.ic_br_find, "Find in page", false, () -> { sheet.dismiss(); host.find(); });
        tile(grid, R.drawable.ic_br_zoom, "Zoom " + web.getSettings().getTextZoom() + "%", false, () -> { sheet.dismiss(); zoom(); });
        tile(grid, R.drawable.ic_br_desktop, "Desktop site", desktop, () -> {
            sheet.dismiss();
            BrowserPrefs.setDesktopMode(web, !desktop);
            web.reload();
        });
        tile(grid, R.drawable.icon_share_round_line, "Share", false, () -> { sheet.dismiss(); share(url); });
        tile(grid, R.drawable.icon_link_twotone_line, "Copy link", false, () -> { sheet.dismiss(); copy(url); });
        tile(grid, R.drawable.ic_br_open, "Open outside", false, () -> { sheet.dismiss(); outside(url); });
        tile(grid, R.drawable.icon_info_round_line, "Page info", false, () -> { sheet.dismiss(); pageInfo(); });
        if (host.hasWallet()) tile(grid, R.drawable.ic_br_wallet, "Connected sites", false, () -> { sheet.dismiss(); sites(); });
        tile(grid, R.drawable.ic_br_clean, "Clear data", false, () -> { sheet.dismiss(); clearData(); });
        tile(grid, R.drawable.ic_settings_line, "Settings", false, () -> { sheet.dismiss(); settings(); });
        box.addView(grid);

        sheet.setContentView(scroll(box));
        sheet.show();
    }

    private void tile(GridLayout grid, int icon, String label, boolean on, Runnable r) {
        LinearLayout t = new LinearLayout(c);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setPadding(dp(2), dp(10), dp(2), dp(10));
        ImageView iv = new ImageView(c);
        iv.setImageResource(icon);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);                  // 24dp icon in a 48dp circle
        iv.setPadding(dp(12), dp(12), dp(12), dp(12));
        iv.setColorFilter(on ? UiColors.onAccent() : UiColors.textPrimary());
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(on ? UiColors.accent() : UiColors.surfaceAlt());
        iv.setBackground(bg);
        t.addView(iv, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView l = text(label, 11.5f, false, UiColors.textPrimary(), 2);
        l.setGravity(Gravity.CENTER);
        l.setPadding(0, dp(6), 0, 0);
        t.addView(l, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ripple(t);
        t.setOnClickListener(v -> r.run());
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
        lp.width = 0;
        grid.addView(t, lp);
    }

    // ── bookmarks and history ─────────────────────────────────────────────────

    public void bookmarks() {
        JSONArray a = BrowserPrefs.bookmarks(c);
        List<JSONObject> items = new ArrayList<>();
        for (int i = a.length() - 1; i >= 0; i--) if (a.optJSONObject(i) != null) items.add(a.optJSONObject(i));
        listSheet("Bookmarks", null, null, items, o -> BrowserPrefs.removeBookmark(c, o.optString("url")));
    }

    public void history() {
        JSONArray a = BrowserPrefs.history(c);
        List<JSONObject> items = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) if (a.optJSONObject(i) != null) items.add(a.optJSONObject(i));
        listSheet("History", "Clear", () -> new MaterialAlertDialogBuilder(c).setTitle("Clear history")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Clear", (d, w) -> { BrowserPrefs.clearHistory(c, host.web()); toast("History cleared"); }).show(),
                items, o -> BrowserPrefs.removeHistory(c, o.optString("url"), o.optLong("at")));
    }

    private interface Remove { void run(JSONObject o); }

    private void listSheet(String title, String action, Runnable onAction, List<JSONObject> items, Remove remove) {
        AppSheet sheet = new AppSheet(c);
        LinearLayout box = column();
        LinearLayout head = new LinearLayout(c);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text(title, 18, true, UiColors.textPrimary(), 1), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(text(String.valueOf(items.size()), 13, true, UiColors.textSecondary(), 1));
        if (action != null && !items.isEmpty()) {
            TextView a = text(action, 14, true, UiColors.error(), 1);
            a.setPadding(dp(16), dp(8), 0, dp(8));
            a.setOnClickListener(v -> { sheet.dismiss(); onAction.run(); });
            head.addView(a);
        }
        box.addView(head);
        LinearLayout list = new LinearLayout(c);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(8), 0, 0);
        SimpleDateFormat day = new SimpleDateFormat("MMM d, HH:mm", Locale.US);
        if (items.isEmpty()) {
            TextView none = text("—", 14, false, UiColors.textTertiary(), 1);
            none.setGravity(Gravity.CENTER);
            none.setPadding(0, dp(24), 0, dp(24));
            list.addView(none);
        }
        for (JSONObject o : items) {
            String url = o.optString("url"), name = o.optString("title");
            LinearLayout row = new LinearLayout(c);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(10), 0, dp(10));
            ImageView ic = new ImageView(c);
            ic.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            if ("harvannaprotocol.com".equals(host(url))) ic.setImageResource(R.drawable.ic_harv_coin);
            else com.bumptech.glide.Glide.with(c).load("https://icons.duckduckgo.com/ip3/" + host(url) + ".ico")
                    .placeholder(R.drawable.icon_link_twotone_line).error(R.drawable.icon_link_twotone_line).into(ic);
            row.addView(ic, new LinearLayout.LayoutParams(dp(28), dp(28)));
            LinearLayout col = new LinearLayout(c);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setPadding(dp(12), 0, dp(8), 0);
            col.addView(text(name.isEmpty() ? host(url) : name, 14.5f, true, UiColors.textPrimary(), 1));
            col.addView(text(url, 12, false, UiColors.textSecondary(), 1));
            row.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            if (o.has("at")) row.addView(text(day.format(new Date(o.optLong("at"))), 11.5f, false, UiColors.textTertiary(), 1));
            ImageView del = new ImageView(c);
            del.setImageResource(R.drawable.ic_dismiss_circle_line);
            del.setColorFilter(UiColors.textTertiary());
            del.setScaleType(ImageView.ScaleType.FIT_CENTER);
            del.setPadding(dp(8), dp(8), dp(8), dp(8));
            del.setContentDescription("Remove");
            del.setOnClickListener(v -> { remove.run(o); list.removeView(row); });
            row.addView(del, new LinearLayout.LayoutParams(dp(36), dp(36)));
            ripple(row);
            row.setOnClickListener(v -> { sheet.dismiss(); host.openUrl(url); });
            list.addView(row);
        }
        box.addView(list);
        sheet.setContentView(scroll(box));
        sheet.show();
    }

    // ── zoom ──────────────────────────────────────────────────────────────────

    public void zoom() {
        WebView web = host.web();
        AppSheet sheet = new AppSheet(c);
        LinearLayout box = column();
        box.addView(text("Zoom", 18, true, UiColors.textPrimary(), 1));
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(16), 0, dp(8));
        TextView minus = pill("−");
        TextView value = text(web.getSettings().getTextZoom() + "%", 22, true, UiColors.textPrimary(), 1);
        value.setGravity(Gravity.CENTER);
        TextView plus = pill("+");
        row.addView(minus, new LinearLayout.LayoutParams(dp(56), dp(44)));
        row.addView(value, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(plus, new LinearLayout.LayoutParams(dp(56), dp(44)));
        box.addView(row);
        SeekBar bar = new SeekBar(c);
        bar.setMax(20);                                        // 50% … 250% in steps of 10
        bar.setProgress((web.getSettings().getTextZoom() - 50) / 10);
        box.addView(bar);
        LinearLayout marks = new LinearLayout(c);
        marks.addView(text("50%", 11.5f, false, UiColors.textTertiary(), 1), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView hi = text("250%", 11.5f, false, UiColors.textTertiary(), 1);
        marks.addView(hi);
        box.addView(marks);
        SeekBar.OnSeekBarChangeListener l = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                int z = 50 + p * 10;
                web.getSettings().setTextZoom(z);
                BrowserPrefs.setTextZoom(c, z);
                value.setText(z + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        };
        bar.setOnSeekBarChangeListener(l);
        minus.setOnClickListener(v -> bar.setProgress(Math.max(0, bar.getProgress() - 1)));
        plus.setOnClickListener(v -> bar.setProgress(Math.min(20, bar.getProgress() + 1)));
        TextView reset = pill("Reset 100%");
        reset.setOnClickListener(v -> bar.setProgress(5));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        rp.topMargin = dp(16);
        box.addView(reset, rp);
        sheet.setContentView(box);
        sheet.show();
    }

    // ── page info and sites ───────────────────────────────────────────────────

    public void pageInfo() {
        WebView web = host.web();
        String url = web.getUrl() == null ? "" : web.getUrl();
        AppSheet sheet = new AppSheet(c);
        LinearLayout box = column();
        box.addView(text(host(url).isEmpty() ? "Page info" : host(url), 18, true, UiColors.textPrimary(), 1));
        InfoTable t = new InfoTable(c);
        boolean https = url.startsWith("https://");
        t.section("Page")
                .row("Title", web.getTitle())
                .row("Address", url)
                .row("Connection", https ? "HTTPS" : "Not secure", https ? UiColors.success() : UiColors.error());
        SslCertificate cert = web.getCertificate();
        if (cert != null) {
            SimpleDateFormat f = new SimpleDateFormat("MMM d, yyyy", Locale.US);
            t.section("Certificate")
                    .row("Issued to", name(cert.getIssuedTo()))
                    .row("Issued by", name(cert.getIssuedBy()))
                    .row("Valid from", cert.getValidNotBeforeDate() == null ? "—" : f.format(cert.getValidNotBeforeDate()))
                    .row("Valid until", cert.getValidNotAfterDate() == null ? "—" : f.format(cert.getValidNotAfterDate()));
        }
        t.section("Settings")
                .row("Zoom", web.getSettings().getTextZoom() + "%")
                .row("Desktop site", BrowserPrefs.isDesktop(web) ? "On" : "Off")
                .row("Bookmarked", BrowserPrefs.isBookmarked(c, url) ? "Yes" : "No")
                .row("Pop-ups", "Blocked")
                .row("Camera, microphone, location", "Blocked");
        List<String> conn = host.hasWallet() ? host.pageConnections() : new ArrayList<>();
        if (host.hasWallet()) {
            t.section("Wallet");
            if (conn.isEmpty()) t.row("Status", "Not connected");
            for (String s : conn) t.row("Connected", s, UiColors.success());
        }
        box.addView(t.view());
        if (!conn.isEmpty()) {
            TextView d = pill("Disconnect");
            d.setTextColor(UiColors.error());
            d.setOnClickListener(v -> { sheet.dismiss(); host.disconnectPage(); toast("Disconnected"); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
            lp.topMargin = dp(16);
            box.addView(d, lp);
        }
        sheet.setContentView(scroll(box));
        sheet.show();
    }

    public void sites() {
        AppSheet sheet = new AppSheet(c);
        LinearLayout box = column();
        List<String> all = host.connectedSites();
        LinearLayout head = new LinearLayout(c);
        head.addView(text("Connected sites", 18, true, UiColors.textPrimary(), 1), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(text(String.valueOf(all.size()), 13, true, UiColors.textSecondary(), 1));
        box.addView(head);
        LinearLayout list = new LinearLayout(c);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(8), 0, 0);
        if (all.isEmpty()) {
            TextView none = text("—", 14, false, UiColors.textTertiary(), 1);
            none.setGravity(Gravity.CENTER);
            none.setPadding(0, dp(24), 0, dp(24));
            list.addView(none);
        }
        for (String origin : all) {
            LinearLayout row = new LinearLayout(c);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(10), 0, dp(10));
            row.addView(text(origin, 14, true, UiColors.textPrimary(), 1), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView d = text("Disconnect", 13, true, UiColors.error(), 1);
            d.setPadding(dp(12), dp(6), 0, dp(6));
            d.setOnClickListener(v -> { host.disconnectSite(origin); list.removeView(row); });
            row.addView(d);
            list.addView(row);
        }
        box.addView(list);
        sheet.setContentView(scroll(box));
        sheet.show();
    }

    // ── clear data ────────────────────────────────────────────────────────────

    public void clearData() {
        List<String> labels = new ArrayList<>();
        labels.add("Cache");
        labels.add("Cookies and site data");
        labels.add("History");
        if (host.hasWallet()) labels.add("Connected sites");
        boolean[] pick = new boolean[labels.size()];
        pick[0] = true;
        pick[1] = true;
        new MaterialAlertDialogBuilder(c).setTitle("Clear data")
                .setMultiChoiceItems(labels.toArray(new String[0]), pick, (d, which, on) -> pick[which] = on)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear", (d, w) -> {
                    WebView web = host.web();
                    if (pick[0]) BrowserPrefs.clearCache(web);
                    if (pick[1]) BrowserPrefs.clearCookiesAndSiteData(c);
                    if (pick[2]) BrowserPrefs.clearHistory(c, web);
                    if (pick.length > 3 && pick[3]) for (String o : host.connectedSites()) host.disconnectSite(o);
                    toast("Cleared");
                    web.reload();
                }).show();
    }

    // ── settings ──────────────────────────────────────────────────────────────

    public void settings() {
        WebView web = host.web();
        AppSheet sheet = new AppSheet(c);
        LinearLayout box = column();
        box.addView(text("Browser settings", 18, true, UiColors.textPrimary(), 1));

        box.addView(section("General"));
        TextView engine = valueRow(box, "Search engine", BrowserPrefs.ENGINES[BrowserPrefs.engine(c)]);
        ((View) engine.getParent()).setOnClickListener(v -> new MaterialAlertDialogBuilder(c).setTitle("Search engine")
                .setSingleChoiceItems(BrowserPrefs.ENGINES, BrowserPrefs.engine(c), (d, w) -> {
                    BrowserPrefs.setEngine(c, w);
                    engine.setText(BrowserPrefs.ENGINES[w]);
                    d.dismiss();
                }).show());
        TextView home = valueRow(box, "Home page", homeLabel());
        ((View) home.getParent()).setOnClickListener(v -> pickHome(home));
        TextView zoom = valueRow(box, "Text size", BrowserPrefs.textZoom(c) + "%");
        ((View) zoom.getParent()).setOnClickListener(v -> { sheet.dismiss(); zoom(); });

        box.addView(section("Content"));
        switchRow(box, "Desktop site", BrowserPrefs.desktop(c), on -> { BrowserPrefs.setDesktop(c, on); BrowserPrefs.setDesktopMode(web, on); web.reload(); });
        switchRow(box, "Load images", BrowserPrefs.images(c), on -> { BrowserPrefs.setImages(c, on); BrowserPrefs.apply(c, web); web.reload(); });
        fixedRow(box, "JavaScript", "On");
        fixedRow(box, "Pop-ups", "Blocked");
        fixedRow(box, "Non-HTTPS pages", "Blocked");

        box.addView(section("Privacy and security"));
        switchRow(box, "Cookies", BrowserPrefs.cookies(c), on -> { BrowserPrefs.setCookies(c, on); BrowserPrefs.apply(c, web); });
        switchRow(box, "Third-party cookies", BrowserPrefs.thirdPartyCookies(c), on -> { BrowserPrefs.setThirdPartyCookies(c, on); BrowserPrefs.apply(c, web); });
        switchRow(box, "Safe Browsing", BrowserPrefs.safeBrowsing(c), on -> { BrowserPrefs.setSafeBrowsing(c, on); BrowserPrefs.apply(c, web); });
        switchRow(box, "Save history", BrowserPrefs.saveHistory(c), on -> BrowserPrefs.setSaveHistory(c, on));
        switchRow(box, "Clear data on exit", BrowserPrefs.clearOnExit(c), on -> BrowserPrefs.setClearOnExit(c, on));
        fixedRow(box, "Camera, microphone, location", "Blocked");
        fixedRow(box, "Certificate errors", "Blocked");

        box.addView(section("Data"));
        TextView clear = valueRow(box, "Clear data", "");
        clear.setTextColor(UiColors.error());
        ((View) clear.getParent()).setOnClickListener(v -> { sheet.dismiss(); clearData(); });
        if (host.hasWallet()) {
            TextView s = valueRow(box, "Connected sites", String.valueOf(host.connectedSites().size()));
            ((View) s.getParent()).setOnClickListener(v -> { sheet.dismiss(); sites(); });
        }
        TextView b = valueRow(box, "Bookmarks", String.valueOf(BrowserPrefs.bookmarks(c).length()));
        ((View) b.getParent()).setOnClickListener(v -> { sheet.dismiss(); bookmarks(); });
        TextView h = valueRow(box, "History", String.valueOf(BrowserPrefs.history(c).length()));
        ((View) h.getParent()).setOnClickListener(v -> { sheet.dismiss(); history(); });

        sheet.setContentView(scroll(box));
        sheet.show();
    }

    private String homeLabel() {
        String h = BrowserPrefs.home(c);
        return h.isEmpty() ? "Web3" : host(h);
    }

    private void pickHome(TextView value) {
        String current = host.web().getUrl();
        String[] items = current != null && current.startsWith("https://")
                ? new String[]{"Web3", "This page", "Enter address"} : new String[]{"Web3", "Enter address"};
        new MaterialAlertDialogBuilder(c).setTitle("Home page").setItems(items, (d, w) -> {
            String pick = items[w];
            if ("Web3".equals(pick)) { BrowserPrefs.setHome(c, ""); value.setText(homeLabel()); }
            else if ("This page".equals(pick)) { BrowserPrefs.setHome(c, current); value.setText(homeLabel()); }
            else {
                EditText in = new EditText(c);
                in.setSingleLine(true);
                in.setHint("https://");
                in.setText(BrowserPrefs.home(c));
                LinearLayout pad = new LinearLayout(c);
                pad.setPadding(dp(20), dp(8), dp(20), 0);
                pad.addView(in, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                new MaterialAlertDialogBuilder(c).setTitle("Home page").setView(pad)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Save", (x, y) -> {
                            String t = in.getText().toString().trim();
                            if (!t.isEmpty() && !t.startsWith("https://")) t = "https://" + t.replaceFirst("(?i)^http://", "");
                            if (!t.isEmpty() && !URLUtil.isHttpsUrl(t)) { toast("Invalid address"); return; }
                            BrowserPrefs.setHome(c, t);
                            value.setText(homeLabel());
                        }).show();
            }
        }).show();
    }

    private interface Toggle { void set(boolean on); }

    private void switchRow(LinearLayout box, String label, boolean on, Toggle t) {
        LinearLayout r = row(label);
        SwitchCompat s = new SwitchCompat(c);
        s.setChecked(on);
        s.setOnCheckedChangeListener((v, checked) -> t.set(checked));
        r.addView(s);
        r.setOnClickListener(v -> s.toggle());
        box.addView(r);
    }

    private TextView valueRow(LinearLayout box, String label, String value) {
        LinearLayout r = row(label);
        TextView v = text(value, 14, true, UiColors.textSecondary(), 1);
        r.addView(v);
        ImageView chev = new ImageView(c);
        chev.setImageResource(R.drawable.icon_keyboard_arrow_right_round_line);
        chev.setColorFilter(UiColors.textTertiary());
        r.addView(chev, new LinearLayout.LayoutParams(dp(24), dp(24)));
        ripple(r);
        box.addView(r);
        return v;
    }

    private void fixedRow(LinearLayout box, String label, String value) {
        LinearLayout r = row(label);
        r.addView(text(value, 14, true, UiColors.textSecondary(), 1));
        box.addView(r);
    }

    private LinearLayout row(String label) {
        LinearLayout r = new LinearLayout(c);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setMinimumHeight(dp(52));
        r.addView(text(label, 15, false, UiColors.textPrimary(), 2), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return r;
    }

    private TextView section(String title) {
        TextView t = text(title.toUpperCase(Locale.US), 11.5f, true, UiColors.textTertiary(), 1);
        t.setLetterSpacing(0.06f);
        t.setPadding(0, dp(18), 0, dp(4));
        return t;
    }

    // ── links and downloads ───────────────────────────────────────────────────

    /** Long press on a link or image: open, copy, share. */
    public void attachLinkMenu() {
        WebView web = host.web();
        web.setOnLongClickListener(v -> {
            WebView.HitTestResult r = web.getHitTestResult();
            int type = r.getType();
            String extra = r.getExtra();
            if (extra == null || !(type == WebView.HitTestResult.SRC_ANCHOR_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
                    || type == WebView.HitTestResult.IMAGE_TYPE)) return false;
            boolean image = type == WebView.HitTestResult.IMAGE_TYPE;
            String[] items = {image ? "Open image" : "Open link", image ? "Copy image address" : "Copy link", "Share", "Open outside"};
            new MaterialAlertDialogBuilder(c).setTitle(extra.length() > 80 ? extra.substring(0, 80) + "…" : extra)
                    .setItems(items, (d, w) -> {
                        if (w == 0) host.openUrl(extra);
                        else if (w == 1) copy(extra);
                        else if (w == 2) share(extra);
                        else outside(extra);
                    }).show();
            return true;
        });
    }

    /** Downloads go to the phone's own browser or download app (https only). */
    public void attachDownloads() {
        host.web().setDownloadListener((url, agent, disposition, mime, length) -> {
            if (url == null || !url.startsWith("https://")) { toast("Blocked"); return; }
            InfoTable t = new InfoTable(c);
            t.row("File", URLUtil.guessFileName(url, disposition, mime))
                    .row("Type", mime == null || mime.isEmpty() ? "—" : mime)
                    .row("Size", length > 0 ? size(length) : "—")
                    .row("From", host(url));
            LinearLayout pad = new LinearLayout(c);
            pad.setPadding(dp(20), dp(8), dp(20), 0);
            pad.addView(t.view(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            new MaterialAlertDialogBuilder(c).setTitle("Download").setView(pad)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Download", (d, w) -> outside(url)).show();
        });
    }

    // ── small actions ─────────────────────────────────────────────────────────

    private void share(String url) {
        if (url == null || url.isEmpty()) return;
        Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url);
        try { c.startActivity(Intent.createChooser(i, "Share")); } catch (Exception ignored) { }
    }

    private void copy(String url) {
        if (url == null || url.isEmpty()) return;
        ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Link", url));
        toast("Link copied");
    }

    private void outside(String url) {
        if (url == null || !url.startsWith("https://")) return;
        try { c.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)); }
        catch (Exception e) { toast("No app can open this"); }
    }

    private void toast(String s) { Toast.makeText(c, s, Toast.LENGTH_SHORT).show(); }

    private static String name(SslCertificate.DName n) {
        if (n == null) return "—";
        String cn = n.getCName(), o = n.getOName();
        if (cn != null && !cn.isEmpty()) return o != null && !o.isEmpty() && !o.equals(cn) ? cn + " · " + o : cn;
        return o == null || o.isEmpty() ? "—" : o;
    }

    private static String size(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format(Locale.US, "%.1f KB", b / 1024.0);
        if (b < 1024L * 1024 * 1024) return String.format(Locale.US, "%.1f MB", b / 1048576.0);
        return String.format(Locale.US, "%.2f GB", b / 1073741824.0);
    }

    static String host(String url) {
        try { String h = Uri.parse(url).getHost(); return h == null ? "" : h; } catch (Exception e) { return ""; }
    }

    // ── views ─────────────────────────────────────────────────────────────────

    private LinearLayout column() {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(16), dp(20), dp(24));
        return box;
    }

    private ScrollView scroll(View v) {
        ScrollView s = new ScrollView(c);
        s.addView(v, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return s;
    }

    private View rule() {
        View v = new View(c);
        v.setBackgroundColor(UiColors.surfaceAlt());
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1))));
        return v;
    }

    private TextView pill(String label) {
        TextView t = text(label, 16, true, UiColors.textPrimary(), 1);
        t.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(UiColors.surfaceAlt());
        t.setBackground(bg);
        return t;
    }

    private TextView text(String s, float size, boolean bold, int color, int lines) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setMaxLines(lines);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        return t;
    }

    private void ripple(View v) {
        android.util.TypedValue tv = new android.util.TypedValue();
        if (c.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true)) v.setBackgroundResource(tv.resourceId);
    }

    private int dp(int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }
}
