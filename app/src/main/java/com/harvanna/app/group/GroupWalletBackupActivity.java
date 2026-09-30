package com.harvanna.app.group;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.print.PrintHelper;

import com.harvanna.app.utils.StatusBarUtil;
import com.harvanna.app.utils.UiColors;
import com.harvanna.app.wallet.CardTemplate;
import com.harvanna.app.wallet.HarvannaCardRenderer;

import java.util.Map;

/**
 * Member view of the group's wallet Access Card. The card is stored encrypted;
 * only the admin knows the passphrase, so members can safely keep, print and
 * save it as a social backup without being able to spend from the wallet.
 */
public class GroupWalletBackupActivity extends AppCompatActivity {
    public static final String EXTRA_GROUP_ID = "groupId";
    public static final String EXTRA_GROUP_NAME = "groupName";

    private LinearLayout content;
    private ProgressBar progress;
    private Bitmap front, back;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
		getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE); // no screenshots or recents thumbnails of secrets

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(UiColors.background());
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        content.setPadding(p, p, p, p);
        sv.addView(content);
        com.harvanna.app.utils.AppBar.setContent(this, sv, "Group wallet backup", "Encrypted social backup card");

        addText("Social backup card", 20, true, UiColors.textPrimary());
        addText("This card is encrypted with a passphrase only the group admin holds. "
            + "Keep a copy safe: if the admin loses access, the admin's passphrase plus any "
            + "member's copy of this card can restore the group wallet. On its own, this card "
            + "cannot move funds.", 14, false, UiColors.textSecondary());

        progress = new ProgressBar(this);
        content.addView(progress);
        load();
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }

    private void load() {
        String gid = getIntent().getStringExtra(EXTRA_GROUP_ID);
        if (gid == null) { fail("Missing group."); return; }
        GroupCardStore.retryPending(this, gid);
        GroupCardStore.fetch(this, gid, new GroupCardStore.Callback() {
            @Override public void onSuccess(Map<String, Object> card) {
                new Thread(() -> render(card)).start();
            }
            @Override public void onError(String error) {
                runOnUiThread(() -> fail(error != null && (error.contains("404") || error.contains("NOT_FOUND"))
                    ? "The admin hasn't published a wallet card for this group yet."
                    : "Couldn't load the card: " + error));
            }
        });
    }

    private void render(Map<String, Object> card) {
        try {
            String addr = str(card.get("walletAddress"));
            String label = str(card.get("label"));
            String qr = str(card.get("qrPayload"));
            String digits = str(card.get("digitCode"));
            CardTemplate t;
            try { t = CardTemplate.valueOf(str(card.get("template"))); }
            catch (Exception e) { t = CardTemplate.values()[0]; }
            Bitmap f = HarvannaCardRenderer.renderFront(this, t, addr, label);
            Bitmap k = HarvannaCardRenderer.renderBack(this, t, qr, digits);
            runOnUiThread(() -> show(f, k));
        } catch (Exception e) {
            runOnUiThread(() -> fail("Couldn't render the card: " + e.getMessage()));
        }
    }

    private void show(Bitmap f, Bitmap k) {
        front = f; back = k;
        progress.setVisibility(View.GONE);
        addImage(f);
        addImage(k);
        addButton("Save as PDF", v -> HarvannaCardRenderer.saveAsPdf(this, front, back,
            "harvanna-group-card-" + System.currentTimeMillis(), exportCb("Saved to Downloads/Harvanna")));
        addButton("Save images", v -> {
            long s = System.currentTimeMillis();
            HarvannaCardRenderer.saveAsPng(this, front, "group-card-front-" + s, exportCb("Front saved"));
            HarvannaCardRenderer.saveAsPng(this, back, "group-card-back-" + s, exportCb("Back saved"));
        });
        addButton("Print", v -> {
            PrintHelper ph = new PrintHelper(this);
            ph.setScaleMode(PrintHelper.SCALE_MODE_FIT);
            ph.printBitmap("Group wallet card - front", front);
            ph.printBitmap("Group wallet card - back", back);
        });
    }

    private HarvannaCardRenderer.ExportCallback exportCb(String ok) {
        return new HarvannaCardRenderer.ExportCallback() {
            @Override public void onSuccess(android.net.Uri u) {
                runOnUiThread(() -> Toast.makeText(GroupWalletBackupActivity.this, ok, Toast.LENGTH_SHORT).show());
            }
            @Override public void onError(String e) {
                runOnUiThread(() -> Toast.makeText(GroupWalletBackupActivity.this, "Failed: " + e, Toast.LENGTH_LONG).show());
            }
        };
    }

    private void fail(String msg) {
        progress.setVisibility(View.GONE);
        addText(msg, 14, false, UiColors.error());
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private void addText(String s, int sp, boolean bold, int color) {
        TextView tv = new TextView(this);
        tv.setText(s); tv.setTextSize(sp); tv.setTextColor(color);
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(0, 0, 0, dp(10));
        content.addView(tv);
    }

    private void addImage(Bitmap bmp) {
        ImageView iv = new ImageView(this);
        iv.setImageBitmap(bmp);
        iv.setAdjustViewBounds(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        content.addView(iv, lp);
    }

    private void addButton(String label, View.OnClickListener l) {
        Button btn = new Button(this);
        btn.setText(label); btn.setAllCaps(false); btn.setOnClickListener(l);
        btn.setGravity(Gravity.CENTER);
        content.addView(btn);
    }
}
