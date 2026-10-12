package com.harvana.app.web3;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.JavaScriptReplyProxy;
import androidx.webkit.ScriptHandler;
import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import com.harvana.app.R;
import com.harvana.app.utils.UiColors;
import com.harvana.app.wallet.Net;
import com.harvana.app.wallet.Web3Signer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The dApp browser. Pages get an EIP-1193 wallet ("window.ethereum", also announced through EIP-6963) for the app's
 * EVM chains: Ethereum, BNB Chain, Polygon and Base.
 *
 * Safety rules, all enforced here:
 *  - HTTPS pages only; any certificate problem stops the page; other link schemes are blocked.
 *  - The wallet bridge (androidx.webkit WebMessageListener) answers the top-level page only, and only when the
 *    message's origin is the page currently shown. Frames inside a page get nothing.
 *  - A site sees the account only after the person connects it (per site, per account); eth_sign is refused.
 *  - Every signature and transaction is shown in full and needs the review sheet and then PIN or fingerprint.
 *  - One request needing the person at a time; a request left unanswered is refused after 5 minutes.
 *  - The private key never reaches the page (Web3Signer derives it for one signature and wipes it).
 *  - No file or content access, no mixed content, no third-party cookies, no pop-up windows, no camera,
 *    microphone or location for pages.
 */
public class DappBrowserActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "url";
    private static final long ANSWER_WITHIN_MS = 5 * 60_000L;

    private WebView web;
    private EditText urlBar;
    private ImageView lock;
    private TextView chainChip;
    private ProgressBar progress;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private String chainKey;
    private String account;                     // the active wallet's EVM address, once derived
    private String hvAccount;                   // the active wallet's Harvana address
    private BrowserBar nav;
    private String pageOrigin = "";             // origin of the page on screen
    private JavaScriptReplyProxy proxy;         // the page's end of the bridge
    private ScriptHandler injected;
    private boolean bridge;
    private int busyId = -1;                    // the request waiting for the person, or -1
    private final Runnable giveUp = () -> { if (busyId >= 0) { error(busyId, 4001, "User rejected the request."); busyId = -1; } };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        chainKey = Web3Sites.chain(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiColors.background());

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), dp(8), dp(8), dp(8));
        bar.setBackgroundColor(UiColors.surface());
        ImageView close = icon(R.drawable.ic_dismiss_circle_line);
        close.setContentDescription("Close");
        close.setOnClickListener(v -> finish());
        bar.addView(close, new LinearLayout.LayoutParams(dp(40), dp(40)));
        lock = icon(R.drawable.icon_lock_round_line);
        bar.addView(lock, new LinearLayout.LayoutParams(dp(28), dp(28)));
        urlBar = new EditText(this);
        urlBar.setSingleLine(true);
        urlBar.setTextSize(14);
        urlBar.setHint("Search or enter dApp URL");
        urlBar.setImeOptions(EditorInfo.IME_ACTION_GO);
        urlBar.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
        urlBar.setBackground(null);
        urlBar.setOnEditorActionListener((v, id, ev) -> {
            if (id == EditorInfo.IME_ACTION_GO || (ev != null && ev.getKeyCode() == KeyEvent.KEYCODE_ENTER)) { open(urlBar.getText().toString()); return true; }
            return false;
        });
        bar.addView(urlBar, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        chainChip = new TextView(this);
        chainChip.setTextSize(12);
        chainChip.setTypeface(null, Typeface.BOLD);
        chainChip.setTextColor(UiColors.textPrimary());
        chainChip.setPadding(dp(10), dp(6), dp(10), dp(6));
        android.graphics.drawable.GradientDrawable cb = new android.graphics.drawable.GradientDrawable();
        cb.setCornerRadius(dp(14));
        cb.setColor(UiColors.surfaceAlt());
        chainChip.setBackground(cb);
        chainChip.setOnClickListener(v -> pickChain());
        bar.addView(chainChip);
        ImageView reload = icon(R.drawable.ic_refresh_line);
        reload.setContentDescription("Reload");
        reload.setOnClickListener(v -> web.reload());
        bar.addView(reload, new LinearLayout.LayoutParams(dp(40), dp(40)));
        root.addView(bar);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        root.addView(progress, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));

        web = new WebView(this);
        root.addView(web, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        nav = new BrowserBar(this, web, null, this::finish);
        root.addView(nav.view(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(root);
        hvAccount = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(this);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        s.setGeolocationEnabled(false);
        s.setSaveFormData(false);
        if (Build.VERSION.SDK_INT >= 26) s.setSafeBrowsingEnabled(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);

        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                return u == null || !"https".equalsIgnoreCase(u.getScheme());      // https only; everything else stays out
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                pageChanged(url);
                nav.setLoading(true);
            }
            @Override public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                nav.update();
            }
            @Override public void onPageFinished(WebView view, String url) {
                pageChanged(url);
                nav.setLoading(false);
                Web3Sites.remember(DappBrowserActivity.this, url, view.getTitle());
            }
            @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();                                                     // never proceed past a bad certificate
                Toast.makeText(DappBrowserActivity.this, "Insecure connection blocked", Toast.LENGTH_LONG).show();
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int p) {
                progress.setProgress(p);
                progress.setVisibility(p >= 100 ? View.INVISIBLE : View.VISIBLE);
            }
            @Override public void onPermissionRequest(PermissionRequest request) { request.deny(); }
            @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback cb) { cb.invoke(origin, false, false); }
        });

        bridge = WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
                && WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT);
        if (bridge) {
            WebViewCompat.addWebMessageListener(web, "harvanaBridge", Collections.singleton("*"), this::onBridgeMessage);
            inject();
        }
        paintChain();

        io.execute(() -> {
            try { String a = Web3Signer.address(this); ui.post(() -> account = a); }
            catch (Exception e) { ui.post(() -> account = null); }
        });

        String start = getIntent() == null ? null : getIntent().getStringExtra(EXTRA_URL);
        if (start != null && !start.isEmpty()) open(start);
    }

    @Override public void onBackPressed() { if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed(); }

    @Override protected void onDestroy() {
        ui.removeCallbacks(giveUp);
        io.shutdownNow();
        if (web != null) { web.stopLoading(); web.destroy(); }
        super.onDestroy();
    }

    // ── navigation ────────────────────────────────────────────────────────────

    /** A web address (https is added), or a search. http:// is upgraded to https://. */
    private void open(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return;
        String url;
        if (t.matches("(?i)^https://\\S+$")) url = t;
        else if (t.matches("(?i)^http://\\S+$")) url = "https://" + t.substring(7);
        else if (!t.contains(" ") && t.contains(".") && !t.contains("://")) url = "https://" + t;
        else url = "https://duckduckgo.com/?q=" + Uri.encode(t);
        urlBar.setText(url);
        urlBar.clearFocus();
        web.loadUrl(url);
    }

    private void pageChanged(String url) {
        String origin = origin(url);
        if (!origin.equals(pageOrigin)) {
            pageOrigin = origin;
            proxy = null;                                   // a new site: nothing from the old one carries over
            if (busyId >= 0) { ui.removeCallbacks(giveUp); busyId = -1; }
        }
        if (!urlBar.hasFocus()) urlBar.setText(url);
        lock.setAlpha(url != null && url.startsWith("https://") ? 1f : 0.3f);
    }

    static String origin(String url) {
        try {
            Uri u = Uri.parse(url);
            if (u.getScheme() == null || u.getHost() == null) return "";
            int port = u.getPort();
            return u.getScheme().toLowerCase() + "://" + u.getHost().toLowerCase() + (port > 0 && port != 443 ? ":" + port : "");
        } catch (Exception e) { return ""; }
    }

    // ── chains ────────────────────────────────────────────────────────────────

    private Net.Evm net() { return Web3Signer.net(chainKey); }

    private static String coin(String key) { return "BNB".equals(key) ? "BNB" : "POL".equals(key) ? "POL" : "ETH"; }

    private String chainHex() { return "0x" + Long.toHexString(net().chainId); }

    private void paintChain() { chainChip.setText(net().name); }

    private void pickChain() {
        String[] names = new String[Web3Signer.CHAINS.length];
        for (int i = 0; i < names.length; i++) names[i] = Web3Signer.net(Web3Signer.CHAINS[i]).name;
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this).setTitle("Network")
                .setItems(names, (d, w) -> switchChain(Web3Signer.CHAINS[w])).show();
    }

    private void switchChain(String key) {
        if (key.equals(chainKey)) return;
        chainKey = key;
        Web3Sites.setChain(this, key);
        paintChain();
        if (bridge) inject();
        emit("chainChanged", chainHex());
    }

    private void inject() {
        if (injected != null) { injected.remove(); injected = null; }
        injected = WebViewCompat.addDocumentStartJavaScript(web, Provider.script(chainHex(), String.valueOf(net().chainId)), Collections.singleton("*"));
    }

    // ── the bridge ────────────────────────────────────────────────────────────

    private void onBridgeMessage(WebView view, WebMessageCompat message, Uri sourceOrigin, boolean isMainFrame, JavaScriptReplyProxy reply) {
        if (!isMainFrame || sourceOrigin == null) return;                 // frames inside a page get no wallet
        String origin = origin(sourceOrigin.toString());
        if (!origin.startsWith("https://") || !origin.equals(pageOrigin)) return;
        proxy = reply;
        JSONObject req;
        try { req = new JSONObject(message.getData()); } catch (Exception e) { return; }
        int id = req.optInt("id", -1);
        String method = req.optString("method", "");
        JSONArray params = req.optJSONArray("params");
        if (params == null) params = new JSONArray();
        try { handle(origin, id, method, params); }
        catch (Exception e) { error(id, -32603, "Internal error"); }
    }

    private void handle(String origin, int id, String method, JSONArray params) throws Exception {
        boolean connected = account != null && Web3Sites.connected(this, account, origin);
        switch (method) {
            case "harvana_chainId": ok(id, "harvana"); return;
            case "harvana_accounts":
                ok(id, hvConnected(origin) ? new JSONArray().put(hvAccount) : new JSONArray()); return;
            case "harvana_requestAccounts":
                if (hvAccount == null || hvAccount.isEmpty()) { error(id, 4100, "No wallet on this phone."); return; }
                if (hvConnected(origin)) { ok(id, new JSONArray().put(hvAccount)); return; }
                if (!claim(id)) return;
                connect(origin, id, hvAccount, "Harvana", () -> {
                    Web3Sites.connect(this, hvAccount, origin);
                    ok(id, new JSONArray().put(hvAccount));
                    emitHv("accountsChanged", new JSONArray().put(hvAccount));
                });
                return;
            case "harvana_disconnect":
                if (hvAccount != null) Web3Sites.disconnect(this, hvAccount, origin);
                ok(id, JSONObject.NULL);
                emitHv("accountsChanged", new JSONArray());
                return;
            case "harvana_sendAction":
                if (!hvConnected(origin)) { error(id, 4100, "Not connected."); return; }
                if (!claim(id)) return;
                harvanaAction(origin, id, params.optJSONObject(0));
                return;
            case "eth_chainId": ok(id, chainHex()); return;
            case "net_version": ok(id, String.valueOf(net().chainId)); return;
            case "eth_accounts": ok(id, connected ? new JSONArray().put(account) : new JSONArray()); return;
            case "eth_requestAccounts":
            case "wallet_requestPermissions":
                if (account == null) { error(id, 4100, "No wallet on this phone."); return; }
                if (connected) { okAccounts(id, method); return; }
                if (!claim(id)) return;
                connect(origin, id, account, net().name, () -> {
                    Web3Sites.connect(this, account, origin);
                    try { okAccounts(id, method); } catch (Exception ignored) { }
                    emit("accountsChanged", new JSONArray().put(account));
                });
                return;
            case "wallet_getPermissions":
                ok(id, connected ? new JSONArray().put(new JSONObject().put("parentCapability", "eth_accounts")) : new JSONArray()); return;
            case "wallet_revokePermissions":
                if (account != null) Web3Sites.disconnect(this, account, origin);
                ok(id, JSONObject.NULL);
                emit("accountsChanged", new JSONArray());
                return;
            case "wallet_switchEthereumChain":
            case "wallet_addEthereumChain": {
                JSONObject p = params.optJSONObject(0);
                String want = p == null ? "" : p.optString("chainId", "");
                for (String k : Web3Signer.CHAINS) {
                    if (("0x" + Long.toHexString(Web3Signer.net(k).chainId)).equalsIgnoreCase(want)) { switchChain(k); ok(id, JSONObject.NULL); return; }
                }
                error(id, 4902, "Unrecognized chain."); return;
            }
            case "eth_sign":
            case "eth_signTransaction":
                error(id, 4200, "Method not supported."); return;
            case "personal_sign":
                if (!connected) { error(id, 4100, "Not connected."); return; }
                if (!claim(id)) return;
                personalSign(origin, id, params);
                return;
            case "eth_signTypedData_v4":
            case "eth_signTypedData_v3":
                if (!connected) { error(id, 4100, "Not connected."); return; }
                if (!claim(id)) return;
                typedData(origin, id, params);
                return;
            case "eth_sendTransaction":
                if (!connected) { error(id, 4100, "Not connected."); return; }
                if (!claim(id)) return;
                sendTx(origin, id, params);
                return;
            default:
                if (Web3Signer.isReadOnly(method)) {
                    final JSONArray p = params;
                    final String rpcUrl = net().rpc;
                    io.execute(() -> {
                        try { Object r = Web3Signer.rpc(rpcUrl, method, p); ui.post(() -> ok(id, r == null ? JSONObject.NULL : r)); }
                        catch (Web3Signer.RpcError e) { ui.post(() -> error(id, e.code, e.getMessage())); }
                        catch (Exception e) { ui.post(() -> error(id, -32603, "Network error")); }
                    });
                    return;
                }
                error(id, 4200, "Method not supported.");
        }
    }

    /** Only one request may wait for the person; it is refused if left for 5 minutes. */
    private boolean claim(int id) {
        if (busyId >= 0) { error(id, -32002, "Request already pending."); return false; }
        busyId = id;
        ui.postDelayed(giveUp, ANSWER_WITHIN_MS);
        return true;
    }

    private void release() { ui.removeCallbacks(giveUp); busyId = -1; }

    private void okAccounts(int id, String method) throws Exception {
        if ("wallet_requestPermissions".equals(method)) ok(id, new JSONArray().put(new JSONObject().put("parentCapability", "eth_accounts")));
        else ok(id, new JSONArray().put(account));
    }

    /** The connect sheet: the site sees {@code addr} only after Connect. */
    private void connect(String origin, int id, String addr, String network, Runnable granted) {
        String host = Uri.parse(origin).getHost();
        com.harvana.app.utils.InfoTable t = new com.harvana.app.utils.InfoTable(this);
        t.row("Site", origin).row("Account", shortAddr(addr)).row("Network", network);
        final boolean[] done = {false};
        androidx.appcompat.app.AlertDialog d = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Connect to " + host).setView(padded(t.view()))
                .setNegativeButton("Cancel", (x, w) -> { done[0] = true; release(); error(id, 4001, "User rejected the request."); })
                .setPositiveButton("Connect", (x, w) -> {
                    done[0] = true;
                    release();
                    if (!origin.equals(pageOrigin)) { error(id, 4001, "User rejected the request."); return; }
                    granted.run();
                }).create();
        d.setOnDismissListener(x -> { if (!done[0]) { release(); error(id, 4001, "User rejected the request."); } });
        d.show();
    }

    // ── Harvana chain actions (the Harvana Swap and Staking sites, or any connected site) ─────────────────────

    private boolean hvConnected(String origin) {
        return hvAccount != null && !hvAccount.isEmpty() && Web3Sites.connected(this, hvAccount, origin);
    }

    private interface Reply { void ok(Object result); void fail(String message); }
    private interface AsyncWork { void run(Reply r); }

    /** One Harvana action from the page: the app builds the review from the values, then signs and sends it itself. */
    private void harvanaAction(String origin, int id, JSONObject a) {
        if (a == null) { release(); error(id, -32602, "Invalid params."); return; }
        final String hv = hvAccount;
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Site", origin});
        rows.add(new String[]{"Account", shortAddr(hv)});
        rows.add(new String[]{"Network", "Harvana"});
        String type = a.optString("type", "");
        switch (type) {
            case "swap": {
                String from = code(a.optString("from")), to = code(a.optString("to"));
                long amt = positive(a, "amountRaw"), min = positive(a, "minOutRaw");
                if (from == null || to == null || from.equals(to) || amt <= 0 || min <= 0) { bad(id); return; }
                rows.add(new String[]{"You pay", fmt(amt, from)});
                rows.add(new String[]{"You get at least", fmt(min, to)});
                reviewAsync("Swap", fmt(amt, from), rows, id, r -> com.harvana.app.sync.NodeGroupClient.swap(this, hv, from, to, amt, min, cb(r)));
                return;
            }
            case "addLiquidity": {
                String asset = code(a.optString("asset"));
                long h = positive(a, "harvRaw"), c = positive(a, "coinRaw");
                if (asset == null || "HARV".equals(asset) || h <= 0 || c <= 0) { bad(id); return; }
                boolean listing = a.optBoolean("listing", false);
                rows.add(new String[]{"Pool", sym(asset) + " / HARV"});
                rows.add(new String[]{"HARV", fmt(h, "HARV")});
                rows.add(new String[]{sym(asset), fmt(c, asset)});
                reviewAsync(listing ? "List coin" : "Add liquidity", fmt(c, asset), rows, id,
                        r -> com.harvana.app.sync.NodeGroupClient.swapAdd(this, hv, asset, h, c, cb(r)));
                return;
            }
            case "removeLiquidity": {
                String asset = code(a.optString("asset"));
                long shares = positive(a, "shares");
                if (asset == null || "HARV".equals(asset) || shares <= 0) { bad(id); return; }
                rows.add(new String[]{"Pool", sym(asset) + " / HARV"});
                rows.add(new String[]{"Shares", String.format(java.util.Locale.US, "%,d", shares)});
                reviewAsync("Remove liquidity", "", rows, id, r -> com.harvana.app.sync.NodeGroupClient.swapRemove(this, hv, asset, shares, cb(r)));
                return;
            }
            case "stake": {
                long amt = positive(a, "amountRaw");
                int days = a.optInt("lockDays", -1);
                if (amt <= 0 || !(days == 0 || days == 7 || days == 30 || days == 90)) { bad(id); return; }
                // The vault comes from the node, never from the page.
                com.harvana.app.sync.NodeGroupClient.getStakingInfo(new com.harvana.app.sync.NodeGroupClient.Callback() {
                    @Override public void onSuccess(JSONObject info) {
                        runOnUiThread(() -> {
                            String vault = info.optString("vaultAddress", "");
                            if (vault.isEmpty() || !origin.equals(pageOrigin)) { release(); error(id, -32603, "Staking is unavailable."); return; }
                            rows.add(new String[]{"Amount", fmt(amt, "HARV")});
                            rows.add(new String[]{"Term", days == 0 ? "Flexible" : days + " days"});
                            rows.add(new String[]{"To", vault});
                            reviewAsync("Stake HARV", fmt(amt, "HARV"), rows, id, r -> com.harvana.app.tx.TransactionRouter.routeTransaction(
                                    DappBrowserActivity.this, hv, vault, BigDecimal.valueOf(amt).movePointLeft(8), null, "STAKE_LOCK:" + days,
                                    new com.harvana.app.tx.TransactionRouter.RouterCallback() {
                                        @Override public void onSuccess(String txHash) { r.ok(txHash); }
                                        @Override public void onError(String code, String msg) { r.fail(msg != null ? msg : code); }
                                    }));
                        });
                    }
                    @Override public void onError(String e) { runOnUiThread(() -> { release(); error(id, -32603, com.harvana.app.utils.ErrorText.plain(e)); }); }
                });
                return;
            }
            case "unstake": {
                long amt = positive(a, "amountRaw");
                String lot = a.optString("lotId", "");
                if (amt <= 0 || !lot.matches("[A-Za-z0-9_:.-]{0,96}")) { bad(id); return; }
                rows.add(new String[]{"Amount", fmt(amt, "HARV")});
                if (!lot.isEmpty()) rows.add(new String[]{"Stake", lot.length() > 16 ? lot.substring(0, 16) + "…" : lot});
                reviewAsync("Unstake HARV", fmt(amt, "HARV"), rows, id,
                        r -> com.harvana.app.sync.NodeGroupClient.unstake(this, hv, lot.isEmpty() ? null : lot, amt, cb(r)));
                return;
            }
            case "claim":
                rows.add(new String[]{"Rewards", "All unclaimed"});
                reviewAsync("Claim staking rewards", "", rows, id, r -> com.harvana.app.sync.NodeGroupClient.claimStakingRewards(this, hv, cb(r)));
                return;
            default:
                release();
                error(id, 4200, "Action not supported.");
        }
    }

    private void bad(int id) { release(); error(id, -32602, "Invalid params."); }

    private com.harvana.app.sync.NodeGroupClient.Callback cb(Reply r) {
        return new com.harvana.app.sync.NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) { r.ok(json); }
            @Override public void onError(String error) { r.fail(error); }
        };
    }

    /** As {@link #review}, for work that answers through a callback (on any thread). */
    private void reviewAsync(String title, String amount, List<String[]> rows, int id, AsyncWork work) {
        final String originAtAsk = pageOrigin;
        com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(this, title, amount, rows,
                () -> {
                    if (!originAtAsk.equals(pageOrigin)) { release(); error(id, 4001, "User rejected the request."); return; }
                    work.run(new Reply() {
                        @Override public void ok(Object result) { runOnUiThread(() -> { release(); DappBrowserActivity.this.ok(id, result); }); }
                        @Override public void fail(String message) { runOnUiThread(() -> { release(); error(id, -32603, com.harvana.app.utils.ErrorText.plain(message)); }); }
                    });
                },
                () -> { if (busyId == id) { release(); error(id, 4001, "User rejected the request."); } });
    }

    private static String code(String c) {
        String u = c == null ? "" : c.trim().toUpperCase(java.util.Locale.ROOT);
        return u.matches("[A-Z]{3,6}") ? u : null;
    }

    private static long positive(JSONObject a, String key) {
        Object v = a.opt(key);
        try { long n = v instanceof Number ? ((Number) v).longValue() : Long.parseLong(String.valueOf(v)); return n > 0 ? n : 0; }
        catch (Exception e) { return 0; }
    }

    private static String sym(String code) { return "HARV".equals(code) ? "HARV" : "h" + code.substring(1); }

    private static String fmt(long raw, String code) {
        int dec = "HARV".equals(code) ? 8 : 6;
        return new BigDecimal(raw).movePointLeft(dec).stripTrailingZeros().toPlainString() + " " + sym(code);
    }

    private void personalSign(String origin, int id, JSONArray params) {
        String a = params.optString(0, ""), b = params.optString(1, "");
        String data = a.equalsIgnoreCase(account) ? b : a;                     // [data, address] or [address, data]
        byte[] bytes = data.startsWith("0x") ? Web3Signer.unhex(data) : data.getBytes(StandardCharsets.UTF_8);
        String shown = printable(bytes) ? new String(bytes, StandardCharsets.UTF_8) : "0x" + hex(bytes);
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Site", origin});
        rows.add(new String[]{"Account", shortAddr(account)});
        rows.add(new String[]{"Network", net().name});
        rows.add(new String[]{"Message", shown.length() > 1200 ? shown.substring(0, 1200) + "…" : shown});
        review("Sign message", "", rows, id, () -> Web3Signer.personalSign(this, account, bytes));
    }

    private void typedData(String origin, int id, JSONArray params) throws Exception {
        String a = params.optString(0, ""), b = params.optString(1, "");
        String json = a.equalsIgnoreCase(account) ? b : a;
        JSONObject typed = new JSONObject(json);
        JSONObject domain = typed.optJSONObject("domain");
        if (domain != null && domain.has("chainId")) {
            BigInteger want = new BigInteger(String.valueOf(domain.opt("chainId")).replaceFirst("^0x", ""), String.valueOf(domain.opt("chainId")).startsWith("0x") ? 16 : 10);
            if (want.longValue() != net().chainId) { release(); error(id, -32602, "Chain mismatch."); return; }
        }
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Site", origin});
        rows.add(new String[]{"Account", shortAddr(account)});
        rows.add(new String[]{"Network", net().name});
        if (domain != null && domain.has("name")) rows.add(new String[]{"App", domain.optString("name")});
        if (domain != null && domain.has("verifyingContract")) rows.add(new String[]{"Contract", domain.optString("verifyingContract")});
        rows.add(new String[]{"Type", typed.optString("primaryType")});
        JSONObject msg = typed.optJSONObject("message");
        String body = msg == null ? "" : msg.toString(2);
        rows.add(new String[]{"Data", body.length() > 1500 ? body.substring(0, 1500) + "…" : body});
        review("Sign data", "", rows, id, () -> Web3Signer.signTypedData(this, account, typed));
    }

    private void sendTx(String origin, int id, JSONArray params) {
        JSONObject tx = params.optJSONObject(0);
        if (tx == null) { release(); error(id, -32602, "Invalid params."); return; }
        String from = tx.optString("from", account);
        if (!from.equalsIgnoreCase(account)) { release(); error(id, 4100, "Unknown account."); return; }
        final Net.Evm n = net();
        final String sym = coin(chainKey);
        io.execute(() -> {
            try {
                long[] g = Web3Signer.estimate(n, account, tx);
                ui.post(() -> {
                    if (!origin.equals(pageOrigin)) { release(); error(id, 4001, "User rejected the request."); return; }
                    BigInteger value = tx.has("value") ? Web3Signer.qty(tx.optString("value")) : BigInteger.ZERO;
                    BigInteger fee = BigInteger.valueOf(g[0]).multiply(BigInteger.valueOf(g[1]));
                    String data = tx.optString("data", tx.optString("input", ""));
                    String to = tx.optString("to", "");
                    List<String[]> rows = new ArrayList<>();
                    rows.add(new String[]{"Site", origin});
                    rows.add(new String[]{"To", to.isEmpty() ? "New contract" : to});
                    rows.add(new String[]{"Network", n.name});
                    rows.add(new String[]{"Function", data.length() >= 10 ? data.substring(0, 10) : "Transfer"});
                    if (data.length() > 2) rows.add(new String[]{"Data", ((data.length() - 2) / 2) + " bytes"});
                    rows.add(new String[]{"Transport fare (max)", ether(fee) + " " + sym});
                    rows.add(new String[]{"Total (max)", ether(value.add(fee)) + " " + sym});
                    review("Send transaction", ether(value) + " " + sym, rows, id, () -> Web3Signer.send(this, n, account, tx, g[0], g[1]));
                });
            } catch (Web3Signer.RpcError e) {
                ui.post(() -> { release(); error(id, e.code, e.getMessage()); });
            } catch (Exception e) {
                ui.post(() -> { release(); error(id, -32603, "Could not estimate this transaction."); });
            }
        });
    }

    private interface Work { String run() throws Exception; }

    /** The review sheet, then PIN or fingerprint, then the work on a background thread; the answer goes to the page. */
    private void review(String title, String amount, List<String[]> rows, int id, Work work) {
        final String originAtAsk = pageOrigin;
        com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(this, title, amount, rows,
                () -> {
                    if (!originAtAsk.equals(pageOrigin)) { release(); error(id, 4001, "User rejected the request."); return; }
                    io.execute(() -> {
                        try {
                            String r = work.run();
                            ui.post(() -> { release(); ok(id, r); });
                        } catch (Web3Signer.RpcError e) {
                            ui.post(() -> { release(); error(id, e.code, e.getMessage()); });
                        } catch (Exception e) {
                            ui.post(() -> { release(); error(id, -32603, com.harvana.app.utils.ErrorText.plain(e.getMessage())); });
                        }
                    });
                },
                () -> { if (busyId == id) { release(); error(id, 4001, "User rejected the request."); } });
    }

    // ── replies ───────────────────────────────────────────────────────────────

    private void ok(int id, Object result) {
        try { post(new JSONObject().put("id", id).put("result", result == null ? JSONObject.NULL : result)); } catch (Exception ignored) { }
    }

    private void error(int id, int code, String message) {
        try { post(new JSONObject().put("id", id).put("error", new JSONObject().put("code", code).put("message", message))); } catch (Exception ignored) { }
    }

    private void emitHv(String event, Object data) {
        try { post(new JSONObject().put("event", event).put("data", data).put("ns", "harvana")); } catch (Exception ignored) { }
    }

    private void emit(String event, Object data) {
        try { post(new JSONObject().put("event", event).put("data", data)); } catch (Exception ignored) { }
    }

    private void post(JSONObject o) {
        JavaScriptReplyProxy p = proxy;
        if (p != null) try { p.postMessage(o.toString()); } catch (Exception ignored) { }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static boolean printable(byte[] b) {
        String s = new String(b, StandardCharsets.UTF_8);
        if (s.contains("�")) return false;
        for (char ch : s.toCharArray()) if (ch < 0x20 && ch != '\n' && ch != '\r' && ch != '\t') return false;
        return true;
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private static String ether(BigInteger wei) {
        return new BigDecimal(wei).movePointLeft(18).stripTrailingZeros().toPlainString();
    }

    private static String shortAddr(String a) { return a == null || a.length() < 14 ? String.valueOf(a) : a.substring(0, 8) + "…" + a.substring(a.length() - 6); }

    private View padded(View v) {
        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(16), dp(8), dp(16), 0);
        box.addView(v, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return box;
    }

    private ImageView icon(int res) {
        ImageView iv = new ImageView(this);
        iv.setImageResource(res);
        iv.setPadding(dp(8), dp(8), dp(8), dp(8));
        iv.setColorFilter(UiColors.textSecondary());
        return iv;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
