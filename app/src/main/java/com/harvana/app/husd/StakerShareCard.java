package com.harvana.app.husd;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.harvana.app.utils.UiColors;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * On the Staking screen: what HARV stakers have earned from stable-coin fees (half of every HARV-less hUSD, hNGN, …
 * transfer fee, shared daily by stake weight; node docs/STABLE_ASSETS.md). One line per coin with a Claim button.
 * Hidden until a stable coin is switched on.
 */
public final class StakerShareCard {
    private StakerShareCard() {}

    private static final String TAG = "stakerShareCard";

    public static void refresh(Activity a, LinearLayout page) {
        if (a == null || page == null) return;
        String address = WalletManager.getActiveWalletAddress(a);
        if (address == null || address.isEmpty()) return;
        HusdClient.shares(address, new HusdClient.Cb() {
            @Override public void ok(JSONObject r) { a.runOnUiThread(() -> draw(a, page, address, r)); }
            @Override public void fail(String e) { }
        });
    }

    private static void draw(Activity a, LinearLayout page, String address, JSONObject r) {
        if (a.isFinishing()) return;
        View old = page.findViewWithTag(TAG);
        if (old != null) page.removeView(old);
        JSONArray rows = r.optJSONArray("shares");
        if (rows == null || rows.length() == 0) return;
        long nonce = r.optLong("nextRequestNonce", 1);

        LinearLayout card = new LinearLayout(a);
        card.setTag(TAG);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(a, 16);
        card.setPadding(pad, pad, pad, pad);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(a, 18));
        bg.setColor(UiColors.surface());
        card.setBackground(bg);
        TextView title = text(a, "Your share of stable-coin fees", 16, true, UiColors.textPrimary());
        card.addView(title);
        card.addView(text(a, "Staking HARV earns half of every fee paid in hUSD, hNGN and the other stable coins by people "
                + "who hold no HARV. It is shared daily by stake size and lock, for stakes held the whole day.", 12, false, UiColors.textSecondary()));

        for (int i = 0; i < rows.length(); i++) {
            JSONObject s = rows.optJSONObject(i);
            if (s == null) continue;
            String code = s.optString("asset");
            long raw = s.optLong("claimableRaw");
            // One slim row: coin on the left; amount and a small Claim pill together on the right.
            LinearLayout row = new LinearLayout(a);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(dp(a, 44));
            row.setPadding(0, dp(a, 4), 0, dp(a, 4));
            ImageView icon = new ImageView(a);
            icon.setImageDrawable(StableIcon.drawable(a, code));
            row.addView(icon, new LinearLayout.LayoutParams(dp(a, 24), dp(a, 24)));
            TextView sym = text(a, Stable.symbol(code), 14, true, UiColors.textPrimary());
            sym.setPadding(dp(a, 10), 0, 0, 0);
            row.addView(sym, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView amt = text(a, HusdClient.format(raw), 14, raw > 0, raw > 0 ? UiColors.textPrimary() : UiColors.textTertiary());
            amt.setGravity(Gravity.END);
            amt.setPadding(0, 0, dp(a, 10), 0);
            com.harvana.app.utils.BalancePrivacy.guard(amt);
            row.addView(amt);
            TextView claim = pill(a, raw > 0);
            claim.setOnClickListener(v -> {
                if (raw <= 0) { Toast.makeText(a, "Nothing to claim yet.", Toast.LENGTH_SHORT).show(); return; }
                claim.setEnabled(false);
                java.util.List<String[]> confirm = new java.util.ArrayList<>();
                confirm.add(new String[]{"From", "Harvana's " + Stable.symbol(code) + " staker pool"});
                confirm.add(new String[]{"To", "This wallet"});
                confirm.add(new String[]{"Network fee", "Normal HARV gas, paid from this wallet first (like every claim)"});
                com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(a, "Claim staking share",
                        HusdClient.format(raw) + " " + Stable.symbol(code), confirm, () ->
                        com.harvana.app.sync.NodeGroupClient.claimStableShare(a, code, address, new com.harvana.app.sync.NodeGroupClient.Callback() {
                            @Override public void onSuccess(JSONObject res) {
                                a.runOnUiThread(() -> {
                                    Toast.makeText(a, "Claimed. It arrives with the next block.", Toast.LENGTH_LONG).show();
                                    refresh(a, page);
                                });
                            }
                            @Override public void onError(String e) {
                                a.runOnUiThread(() -> {
                                    claim.setEnabled(true);
                                    Toast.makeText(a, com.harvana.app.utils.ErrorText.is(e, "NOTHING_TO_CLAIM") ? "Nothing to claim yet." : com.harvana.app.utils.ErrorText.plain(e),
                                            Toast.LENGTH_LONG).show();
                                });
                            }
                        }));
            });
            row.addView(claim);
            card.addView(row);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(a, 12);
        page.addView(card, Math.min(1, page.getChildCount()), lp);
    }

    /** A small rounded "Claim" pill: filled when there is something to claim, outlined and quiet when not. */
    private static TextView pill(Activity a, boolean active) {
        TextView t = new TextView(a);
        t.setText("Claim");
        t.setTextSize(12);
        t.setTypeface(null, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(a, 14), dp(a, 6), dp(a, 14), dp(a, 6));
        t.setMinWidth(dp(a, 64));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(a, 14));
        if (active) {
            g.setColor(UiColors.accent());
            t.setTextColor(UiColors.onAccent());
        } else {
            g.setColor(0x00000000);
            g.setStroke(dp(a, 1), UiColors.textTertiary());
            t.setTextColor(UiColors.textTertiary());
        }
        t.setBackground(g);
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }

    private static TextView text(Activity a, String s, float sp, boolean bold, int color) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setPadding(0, dp(a, 2), 0, dp(a, 2));
        return t;
    }

    private static int dp(Activity a, int v) { return Math.round(v * a.getResources().getDisplayMetrics().density); }
}
