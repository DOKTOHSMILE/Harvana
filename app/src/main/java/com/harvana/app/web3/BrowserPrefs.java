package com.harvana.app.web3;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

/** Browser settings, bookmarks and history (this phone only), and how they are applied to a WebView. */
public final class BrowserPrefs {
    private BrowserPrefs() {}

    private static final String PREFS = "browser_prefs";
    private static final int HISTORY_MAX = 300;

    public static final String[] ENGINES = {"DuckDuckGo", "Google", "Brave", "Bing", "Startpage"};
    private static final String[] ENGINE_URLS = {
            "https://duckduckgo.com/?q=", "https://www.google.com/search?q=", "https://search.brave.com/search?q=",
            "https://www.bing.com/search?q=", "https://www.startpage.com/do/search?q="};

    private static SharedPreferences sp(Context c) { return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    // ── settings ──────────────────────────────────────────────────────────────

    public static int engine(Context c) { return Math.max(0, Math.min(ENGINES.length - 1, sp(c).getInt("engine", 0))); }
    public static void setEngine(Context c, int i) { sp(c).edit().putInt("engine", i).apply(); }
    public static String searchUrl(Context c, String q) { return ENGINE_URLS[engine(c)] + Uri.encode(q); }

    public static int textZoom(Context c) { return sp(c).getInt("zoom", 100); }
    public static void setTextZoom(Context c, int z) { sp(c).edit().putInt("zoom", Math.max(50, Math.min(250, z))).apply(); }

    public static boolean desktop(Context c) { return sp(c).getBoolean("desktop", false); }
    public static void setDesktop(Context c, boolean on) { sp(c).edit().putBoolean("desktop", on).apply(); }

    public static boolean images(Context c) { return sp(c).getBoolean("images", true); }
    public static void setImages(Context c, boolean on) { sp(c).edit().putBoolean("images", on).apply(); }

    public static boolean thirdPartyCookies(Context c) { return sp(c).getBoolean("cookies3p", false); }
    public static void setThirdPartyCookies(Context c, boolean on) { sp(c).edit().putBoolean("cookies3p", on).apply(); }

    public static boolean cookies(Context c) { return sp(c).getBoolean("cookies", true); }
    public static void setCookies(Context c, boolean on) { sp(c).edit().putBoolean("cookies", on).apply(); }

    public static boolean safeBrowsing(Context c) { return sp(c).getBoolean("safe", true); }
    public static void setSafeBrowsing(Context c, boolean on) { sp(c).edit().putBoolean("safe", on).apply(); }

    public static boolean clearOnExit(Context c) { return sp(c).getBoolean("clear_exit", false); }
    public static void setClearOnExit(Context c, boolean on) { sp(c).edit().putBoolean("clear_exit", on).apply(); }

    public static boolean saveHistory(Context c) { return sp(c).getBoolean("save_history", true); }
    public static void setSaveHistory(Context c, boolean on) { sp(c).edit().putBoolean("save_history", on).apply(); }

    /** Home page; empty means Discover. */
    public static String home(Context c) { return sp(c).getString("home", ""); }
    public static void setHome(Context c, String url) { sp(c).edit().putString("home", url == null ? "" : url.trim()).apply(); }

    // ── applying ──────────────────────────────────────────────────────────────

    /** Everything the settings decide, on one WebView. The browser's fixed safety settings are set by the screen. */
    public static void apply(Context c, WebView web) {
        WebSettings s = web.getSettings();
        s.setTextZoom(textZoom(c));
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setLoadsImagesAutomatically(images(c));
        s.setBlockNetworkImage(!images(c));
        if (Build.VERSION.SDK_INT >= 26) s.setSafeBrowsingEnabled(safeBrowsing(c));
        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(cookies(c));
        cm.setAcceptThirdPartyCookies(web, cookies(c) && thirdPartyCookies(c));
        setDesktopMode(web, desktop(c));
    }

    static final String METAMASK_UA = "MetaMaskMobile/7.57.0";

    /** Desktop site: a desktop Chrome user agent (same version) and a wide viewport. */
    public static void setDesktopMode(WebView web, boolean on) {
        WebSettings s = web.getSettings();
        String mobile = WebSettings.getDefaultUserAgent(web.getContext());
        if (on) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("Chrome/([\\d.]+)").matcher(mobile);
            String ver = m.find() ? m.group(1) : "124.0.0.0";
            s.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + ver + " Safari/537.36");
        } else {
            // MetaMask compatibility (owner, 13 Oct 2026): dApps using the MetaMask SDK or Reown/WalletConnect look for
            // "MetaMaskMobile" in the user agent to know they are inside a wallet's own browser. With it, "MetaMask" in
            // their wallet list connects straight to this app's wallet (window.ethereum) instead of opening the MetaMask app.
            s.setUserAgentString(mobile + " " + METAMASK_UA);
        }
        s.setUseWideViewPort(on);
        s.setLoadWithOverviewMode(on);
    }

    public static boolean isDesktop(WebView web) {
        String ua = web.getSettings().getUserAgentString();
        return ua != null && ua.contains("X11; Linux x86_64");
    }

    // ── clearing ──────────────────────────────────────────────────────────────

    public static void clearCache(WebView web) {
        web.clearCache(true);
        web.clearFormData();
    }

    public static void clearCookiesAndSiteData(Context c) {
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        WebStorage.getInstance().deleteAllData();
        WebViewDatabase db = WebViewDatabase.getInstance(c);
        db.clearHttpAuthUsernamePassword();
        if (Build.VERSION.SDK_INT < 26) db.clearFormData();
    }

    public static void clearHistory(Context c, WebView web) {
        if (web != null) web.clearHistory();
        sp(c).edit().remove("history").apply();
        Web3Sites.clearRecent(c);
    }

    // ── bookmarks ─────────────────────────────────────────────────────────────

    public static JSONArray bookmarks(Context c) {
        try { return new JSONArray(sp(c).getString("bookmarks", "[]")); } catch (Exception e) { return new JSONArray(); }
    }

    public static boolean isBookmarked(Context c, String url) {
        if (url == null) return false;
        JSONArray a = bookmarks(c);
        for (int i = 0; i < a.length(); i++) if (url.equals(a.optJSONObject(i) == null ? null : a.optJSONObject(i).optString("url"))) return true;
        return false;
    }

    /** Adds the page, or removes it when it is already saved. Returns true when it is now saved. */
    public static boolean toggleBookmark(Context c, String url, String title) {
        if (url == null || url.isEmpty()) return false;
        JSONArray old = bookmarks(c), out = new JSONArray();
        boolean had = false;
        for (int i = 0; i < old.length(); i++) {
            JSONObject o = old.optJSONObject(i);
            if (o == null) continue;
            if (url.equals(o.optString("url"))) { had = true; continue; }
            out.put(o);
        }
        if (!had) {
            try { out.put(new JSONObject().put("url", url).put("title", title == null ? "" : title).put("at", System.currentTimeMillis())); }
            catch (Exception ignored) { }
        }
        sp(c).edit().putString("bookmarks", out.toString()).apply();
        return !had;
    }

    public static void removeBookmark(Context c, String url) { if (isBookmarked(c, url)) toggleBookmark(c, url, null); }

    // ── history ───────────────────────────────────────────────────────────────

    public static JSONArray history(Context c) {
        try { return new JSONArray(sp(c).getString("history", "[]")); } catch (Exception e) { return new JSONArray(); }
    }

    public static void addHistory(Context c, String url, String title) {
        if (!saveHistory(c) || url == null || !url.startsWith("https://")) return;
        try {
            JSONArray old = history(c), out = new JSONArray();
            JSONObject first = old.optJSONObject(0);
            if (first != null && url.equals(first.optString("url"))) {                  // a reload: refresh the title and time
                first.put("title", title == null ? first.optString("title") : title).put("at", System.currentTimeMillis());
                sp(c).edit().putString("history", old.toString()).apply();
                return;
            }
            out.put(new JSONObject().put("url", url).put("title", title == null ? "" : title).put("at", System.currentTimeMillis()));
            for (int i = 0; i < old.length() && out.length() < HISTORY_MAX; i++) out.put(old.get(i));
            sp(c).edit().putString("history", out.toString()).apply();
        } catch (Exception ignored) { }
    }

    public static void removeHistory(Context c, String url, long at) {
        JSONArray old = history(c), out = new JSONArray();
        for (int i = 0; i < old.length(); i++) {
            JSONObject o = old.optJSONObject(i);
            if (o != null && !(url.equals(o.optString("url")) && at == o.optLong("at"))) out.put(o);
        }
        sp(c).edit().putString("history", out.toString()).apply();
    }
}
