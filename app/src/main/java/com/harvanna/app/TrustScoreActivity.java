package com.harvanna.app;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.harvanna.app.sync.NodeIdentityClient;
import com.harvanna.app.utils.UiColors;
import com.harvanna.app.wallet.WalletManager;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Trust as the node records it. The number is personhood: 10 once this device is bound to the
 * wallet, plus 2 for each active vouch, up to 8. Labels are live badges from circle history.
 * Neither is stored in Firebase, and neither is a 100-point profile score.
 */
public class TrustScoreActivity extends AppCompatActivity {
    private LinearLayout root;
    private String address = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(UiColors.background());
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        root.setPadding(p, p, p, dp(28));
        sv.addView(root);
        com.harvanna.app.utils.AppBar.setContent(this, sv, "Trust score", "Recorded on the network");
    }

    @Override protected void onResume() { super.onResume(); load(); }
    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    private void load() {
        root.removeAllViews();
        root.addView(tv("Reading the network…", 14, false, UiColors.textSecondary()));
        address = WalletManager.getActiveWalletAddress(this);
        if (address == null || address.isEmpty()) {
            root.removeAllViews();
            root.addView(tv("No wallet yet", 22, true, UiColors.textPrimary()));
            root.addView(tv("Create a wallet first. Trust is recorded against the wallet, not the profile.", 14, false, UiColors.textSecondary()));
            return;
        }
        final JSONObject[] person = new JSONObject[1];
        final JSONObject[] labels = new JSONObject[1];
        final int[] left = {2};
        Runnable paint = () -> { if (--left[0] == 0) runOnUiThread(() -> render(person[0], labels[0])); };
        NodeIdentityClient.lookup(address, new NodeIdentityClient.Callback() {
            @Override public void onSuccess(JSONObject j) { person[0] = j; paint.run(); }
            @Override public void onError(String e) { paint.run(); }
        });
        com.harvanna.app.sync.HarvannaNodeClient.nodeGet("/api/v1/wallet/labels/" + address, new com.harvanna.app.sync.HarvannaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject j) { labels[0] = j; paint.run(); }
            @Override public void onError(String e) { paint.run(); }
        });
    }

    private void render(JSONObject person, JSONObject labels) {
        root.removeAllViews();
        if (person == null) {
            root.addView(tv("The network didn't return a trust score.", 14, false, UiColors.error()));
            return;
        }
        boolean bound = person.optBoolean("bound");
        int score = person.optInt("trustScore", 0);
        int vouches = person.optInt("effectiveVouchCount", 0);
        root.addView(tv(score + " / 26", 36, true, UiColors.accent()));
        root.addView(tv(bound ? "This wallet is bound to a device." : "Not bound yet. The score stays 0 until it is.", 14, false, UiColors.textSecondary()));

        root.addView(head("How the number is made"));
        root.addView(line("Device", bound ? "10" : "0", "Binding this phone to the wallet is worth 10. It is not a government ID."));
        root.addView(line("Vouches", String.valueOf(Math.min(vouches, 8) * 2), vouches + " active " + (vouches == 1 ? "vouch" : "vouches") + ". Each one adds 2, and only the first 8 count."));
        if (person.optBoolean("established")) root.addView(tv("Established. You can vouch for other members.", 13, false, UiColors.textPrimary()));
        long cool = person.optLong("vouchCoolOffUntil", 0);
        if (cool > System.currentTimeMillis()) root.addView(tv("You cannot vouch again until the cool-off ends.", 13, false, UiColors.error()));

        if (!bound) {
            MaterialButton bind = new MaterialButton(this);
            bind.setAllCaps(false);
            bind.setText("Bind this device");
            bind.setOnClickListener(v -> bindDevice());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(12);
            root.addView(bind, lp);
        }

        root.addView(head("Badges from your circles"));
        root.addView(tv("These are computed when you open this screen, from rotating circles you have actually finished. A badge does not add points, and a group uses them only to mark the circle New, Established, or High trust.", 13, false, UiColors.textSecondary()));
        JSONArray rows = labels == null ? null : labels.optJSONArray("labels");
        if (rows == null || rows.length() == 0) {
            root.addView(card("No badges yet", "Finish a rotating circle. If you never default, Zero default appears. Ten payments met on time can add Early payer."));
        } else {
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row == null) continue;
                String name = row.optString("label", "");
                JSONObject detail = row.optJSONObject("detail");
                root.addView(card(titleOf(name), bodyOf(name, row.optString("type", ""), detail)));
            }
        }
    }

    private void bindDevice() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Bind this device?")
            .setMessage("This ties the wallet to this phone and sets the base score to 10. It does not upload an ID. One device hash can be bound to one wallet.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Bind", (d, w) -> NodeIdentityClient.bindPersonhood(this, address, new NodeIdentityClient.Callback() {
                @Override public void onSuccess(JSONObject j) { runOnUiThread(TrustScoreActivity.this::load); }
                @Override public void onError(String e) {
                    runOnUiThread(() -> android.widget.Toast.makeText(TrustScoreActivity.this, e, android.widget.Toast.LENGTH_LONG).show());
                }
            }))
            .show();
    }

    private static String titleOf(String name) {
        if ("CIRCLE_VETERAN".equals(name)) return "Circle veteran";
        if ("FOUNDING_MEMBER".equals(name)) return "Founding member";
        if ("ZERO_DEFAULT".equals(name)) return "Zero default";
        if ("EARLY_PAYER".equals(name)) return "Early payer";
        if ("VOUCHED".equals(name)) return "Vouched";
        return name;
    }

    private static String bodyOf(String name, String type, JSONObject detail) {
        String kind = "PERMANENT".equals(type) ? "Stays on the record." : "Recalculated every time. It can go away.";
        if ("CIRCLE_VETERAN".equals(name)) {
            int n = detail == null ? 0 : detail.optInt("circlesCompleted", 0);
            return "Finished " + n + " rotating " + (n == 1 ? "circle" : "circles") + ". " + kind;
        }
        if ("FOUNDING_MEMBER".equals(name)) return "You started a rotating circle that ran to the end. " + kind;
        if ("ZERO_DEFAULT".equals(name)) return "You have finished a circle and have never defaulted. One default removes this. " + kind;
        if ("EARLY_PAYER".equals(name)) {
            int pct = detail == null ? 0 : detail.optInt("onTimeRatePercent", 0);
            return "At least 10 payments, and " + pct + "% met the obligation. " + kind;
        }
        if ("VOUCHED".equals(name)) {
            int n = detail == null ? 0 : detail.optInt("activeVouches", 0);
            return n + " active " + (n == 1 ? "vouch" : "vouches") + " from members the network already trusts. " + kind;
        }
        return kind;
    }

    private ViewGroup line(String name, String value, String note) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(8), 0, dp(4));
        TextView top = tv(name + "  ·  " + value, 15, true, UiColors.textPrimary());
        row.addView(top);
        row.addView(tv(note, 12, false, UiColors.textSecondary()));
        return row;
    }

    private ViewGroup card(String title, String body) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        int p = dp(14);
        c.setPadding(p, p, p, p);
        GradientDrawable g = new GradientDrawable();
        g.setColor(UiColors.surface());
        g.setCornerRadius(dp(14));
        c.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        c.setLayoutParams(lp);
        c.addView(tv(title, 15, true, UiColors.textPrimary()));
        c.addView(tv(body, 13, false, UiColors.textSecondary()));
        return c;
    }

    private TextView head(String t) {
        TextView v = tv(t, 16, true, UiColors.textPrimary());
        v.setPadding(0, dp(22), 0, dp(4));
        return v;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private TextView tv(String t, int sp, boolean bold, int color) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(color);
        if (bold) v.setTypeface(null, Typeface.BOLD);
        v.setPadding(0, dp(2), 0, dp(2));
        return v;
    }
}
