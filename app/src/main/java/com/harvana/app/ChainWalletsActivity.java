package com.harvana.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.harvana.app.utils.UiColors;
import com.harvana.app.wallet.ChainBalances;
import com.harvana.app.wallet.ChainKeys;
import com.harvana.app.wallet.ChainWalletStore;
import com.harvana.app.wallet.PiWalletStore;
import com.harvana.app.wallet.WalletManager;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Crypto wallets: the Pi wallet and the Bitcoin, Ethereum (and EVM chains), Tron, Solana, Litecoin and Dogecoin wallets that
 * the active wallet's recovery phrase also opens. Each card shows the address with Copy and a QR code to receive, and the
 * balance read from a free public service. Sending from these wallets comes in a later step; receiving works now, and the
 * same words open them in any standard wallet app.
 */
public class ChainWalletsActivity extends AppCompatActivity {

    private final ExecutorService bg = Executors.newFixedThreadPool(3);
    private LinearLayout list;
    private SwipeRefreshLayout refresh;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        refresh = new SwipeRefreshLayout(this);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(UiColors.background());
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(16), dp(8), dp(16), dp(28));
        sv.addView(list);
        refresh.addView(sv);
        refresh.setOnRefreshListener(this::load);
        com.harvana.app.utils.AppBar.setContent(this, refresh, "Crypto wallets", "One recovery phrase, every chain");
        load();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bg.shutdownNow();
    }

    private void load() {
        refresh.setRefreshing(true);
        list.removeAllViews();
        list.addView(com.harvana.app.utils.Shimmer.cards(this, 3));
        final String harv = WalletManager.getActiveWalletAddress(this);
        bg.execute(() -> {
            Map<ChainKeys.Chain, String> addrs = ChainWalletStore.ensure(getApplicationContext(), harv);
            String pi = PiWalletStore.get(getApplicationContext(), harv);
            runOnUiThread(() -> render(addrs, pi));
        });
    }

    private void render(Map<ChainKeys.Chain, String> addrs, String pi) {
        if (isFinishing()) return;
        refresh.setRefreshing(false);
        list.removeAllViews();
        TextView intro = text("These wallets come from your Harvana recovery phrase. The same words open them in Pi's app, "
                + "MetaMask, Trust Wallet, Phantom and other standard wallets.", 13, false, UiColors.textSecondary());
        intro.setPadding(dp(4), dp(4), dp(4), dp(8));
        list.addView(intro);
        if (addrs.isEmpty()) {
            list.addView(card("No recovery phrase on this phone",
                    "This wallet was added from a private key, so it has no phrase to make other wallets from. Create or restore a wallet with its recovery words.", null, null));
            return;
        }
        if (!pi.isEmpty()) {
            final TextView bal = text("Loading…", 15, true, UiColors.textPrimary());
            LinearLayout piCard = card("Pi Network", pi, bal, "PI");
            LinearLayout piActions = new LinearLayout(this);
            TextView send = text("Send Pi", 14, true, UiColors.accent());
            send.setPadding(0, dp(6), dp(24), dp(6));
            send.setOnClickListener(v -> sendPi(pi));
            TextView hist = text("History", 14, true, UiColors.accent());
            hist.setPadding(0, dp(6), 0, dp(6));
            hist.setOnClickListener(v -> piHistory(pi));
            piActions.addView(send);
            piActions.addView(hist);
            piCard.addView(piActions);
            list.addView(piCard);
            bg.execute(() -> { String s = ChainBalances.pi(pi); runOnUiThread(() -> bal.setText(s)); });
        } else {
            list.addView(card("Pi Network", "Pi wallets use 24 words. This wallet has 12, so it has no Pi wallet. "
                    + "Create a new wallet (24 words) to get one.", null, null));
        }
        for (Map.Entry<ChainKeys.Chain, String> e : addrs.entrySet()) {
            final TextView bal = text("Loading…", 15, true, UiColors.textPrimary());
            final ChainKeys.Chain chain = e.getKey();
            final String a = e.getValue();
            list.addView(card(chain.title, a, bal, chain.ticker));
            bg.execute(() -> { String s = ChainBalances.describe(chain, a); runOnUiThread(() -> bal.setText(s)); });
        }
        TextView note = text("Balances come from free public services and can lag a little. Sending from these wallets comes next; "
                + "until then, the same words open them in any standard wallet app.", 12, false, UiColors.textTertiary());
        note.setPadding(dp(4), dp(12), dp(4), 0);
        list.addView(note);
    }

    /** Send Pi: address, amount and an optional note; reviewed, then the PIN or fingerprint, then signed on this phone. */
    private void sendPi(String from) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        final android.widget.EditText to = field("Recipient's Pi address (G...)", false);
        final android.widget.EditText amount = field("Amount in Pi", true);
        final android.widget.EditText memo = field("Note (optional, up to 28 letters)", false);
        memo.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(28)});
        box.addView(to); box.addView(amount); box.addView(memo);
        TextView fee = text("Network fee: 0.01 Pi, paid by you. A new Pi wallet must receive at least the network's minimum balance.", 12, false, UiColors.textTertiary());
        fee.setPadding(0, dp(8), 0, 0);
        box.addView(fee);
        new com.harvana.app.ui.SheetDialog.Builder(this).setTitle("Send Pi").setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Review", (d, w) -> {
                    final String dest = to.getText().toString().trim();
                    final String amt = amount.getText().toString().trim();
                    if (!com.harvana.app.wallet.PiKeys.isValidAddress(dest)) { Toast.makeText(this, "That is not a Pi address.", Toast.LENGTH_LONG).show(); return; }
                    try { com.harvana.app.wallet.PiTx.toStroops(amt); } catch (Exception e) { Toast.makeText(this, "Enter an amount above 0, up to 7 decimals.", Toast.LENGTH_LONG).show(); return; }
                    java.util.List<String[]> rows = new java.util.ArrayList<>();
                    rows.add(new String[]{"To", dest.substring(0, 6) + "\u2026" + dest.substring(dest.length() - 6)});
                    rows.add(new String[]{"Network", "Pi Network"});
                    rows.add(new String[]{"Fee", "0.01 Pi"});
                    if (!memo.getText().toString().trim().isEmpty()) rows.add(new String[]{"Note", memo.getText().toString().trim()});
                    com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(this, "Send Pi", amt + " Pi", rows, () -> {
                        Toast.makeText(this, "Sending…", Toast.LENGTH_SHORT).show();
                        final String harv = WalletManager.getActiveWalletAddress(this);
                        bg.execute(() -> {
                            String msg;
                            try {
                                String phrase = com.harvana.app.wallet.SecureWalletStorage.loadMnemonic(getApplicationContext(), harv);
                                if (phrase == null) throw new Exception("No recovery phrase on this phone for this wallet.");
                                String hash = com.harvana.app.wallet.PiClient.send(phrase, dest, amt, memo.getText().toString().trim());
                                msg = "Sent " + amt + " Pi. Transaction " + (hash.length() > 10 ? hash.substring(0, 10) + "\u2026" : hash);
                            } catch (Exception e) {
                                msg = e.getMessage() == null ? "Could not send." : e.getMessage();
                            }
                            final String m = msg;
                            runOnUiThread(() -> { Toast.makeText(this, m, Toast.LENGTH_LONG).show(); load(); });
                        });
                    });
                }).show();
    }

    private void piHistory(String address) {
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(4), dp(20), 0);
        box.addView(text("Loading…", 14, false, UiColors.textSecondary()));
        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        new com.harvana.app.ui.SheetDialog.Builder(this).setTitle("Pi history").setView(sv).setPositiveButton("Done", null).show();
        bg.execute(() -> {
            java.util.List<com.harvana.app.wallet.PiClient.Payment> ps;
            String err = null;
            try { ps = com.harvana.app.wallet.PiClient.history(address); } catch (Exception e) { ps = new java.util.ArrayList<>(); err = e.getMessage(); }
            final java.util.List<com.harvana.app.wallet.PiClient.Payment> list2 = ps;
            final String e2 = err;
            runOnUiThread(() -> {
                box.removeAllViews();
                if (list2.isEmpty()) { box.addView(text(e2 != null ? "Couldn't load: " + e2 : "No Pi payments yet.", 14, false, UiColors.textSecondary())); return; }
                for (com.harvana.app.wallet.PiClient.Payment p : list2) {
                    TextView t = text(p.direction + "  " + p.amount + " Pi", 15, true, "Received".equals(p.direction) ? UiColors.success() : UiColors.textPrimary());
                    t.setPadding(0, dp(10), 0, 0);
                    box.addView(t);
                    String who = p.counterparty.length() > 12 ? p.counterparty.substring(0, 6) + "\u2026" + p.counterparty.substring(p.counterparty.length() - 6) : p.counterparty;
                    box.addView(text(("Received".equals(p.direction) ? "From " : "To ") + who + "  \u00b7  " + p.when, 12, false, UiColors.textTertiary()));
                }
            });
        });
    }

    private android.widget.EditText field(String hint, boolean number) {
        android.widget.EditText e = new android.widget.EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        if (number) e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return e;
    }

    /** One wallet: name, balance, the address, and Copy / QR. With balance null it is a plain message card. */
    private LinearLayout card(String title, String address, TextView balance, String ticker) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        bg.setColor(UiColors.surface());
        c.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        c.setLayoutParams(lp);
        c.addView(text(title, 16, true, UiColors.textPrimary()));
        if (balance == null) {
            TextView m = text(address, 13, false, UiColors.textSecondary());
            m.setPadding(0, dp(4), 0, 0);
            c.addView(m);
            return c;
        }
        balance.setPadding(0, dp(4), 0, dp(6));
        c.addView(balance);
        TextView addr = text(address, 12, false, UiColors.textSecondary());
        addr.setTypeface(Typeface.MONOSPACE);
        addr.setTextIsSelectable(true);
        c.addView(addr);
        LinearLayout actions = new LinearLayout(this);
        actions.setPadding(0, dp(8), 0, 0);
        TextView copy = text("Copy", 14, true, UiColors.accent());
        copy.setPadding(0, dp(6), dp(24), dp(6));
        copy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText(title + " address", address));
            Toast.makeText(this, title + " address copied", Toast.LENGTH_SHORT).show();
        });
        TextView qr = text("Show QR to receive", 14, true, UiColors.accent());
        qr.setPadding(0, dp(6), 0, dp(6));
        qr.setOnClickListener(v -> showQr(title, ticker, address));
        actions.addView(copy);
        actions.addView(qr);
        c.addView(actions);
        return c;
    }

    private void showQr(String title, String ticker, String address) {
        Bitmap bmp = HARVQRCodeGenerator.generateQRCode(address, 720);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(20), dp(20), dp(20), dp(8));
        ImageView iv = new ImageView(this);
        iv.setImageBitmap(bmp);
        box.addView(iv, new LinearLayout.LayoutParams(dp(240), dp(240)));
        TextView a = text(address, 12, false, UiColors.textSecondary());
        a.setTypeface(Typeface.MONOSPACE);
        a.setGravity(Gravity.CENTER);
        a.setPadding(0, dp(10), 0, 0);
        box.addView(a);
        TextView warn = text("Send only " + ticker + (ticker.equals("ETH") ? " or tokens on Ethereum, BNB Chain, Base or Polygon" : "")
                + " to this address. Coins from another network can be lost.", 12, false, UiColors.textTertiary());
        warn.setGravity(Gravity.CENTER);
        warn.setPadding(0, dp(8), 0, 0);
        box.addView(warn);
        new com.harvana.app.ui.SheetDialog.Builder(this).setTitle("Receive on " + title).setView(box)
                .setPositiveButton("Done", null).show();
    }

    private TextView text(String s, float sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
