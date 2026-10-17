package com.harvana.app.group;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.print.PrintHelper;

import com.harvana.app.wallet.CardTemplate;
import com.harvana.app.wallet.HarvanaCardRenderer;

import java.util.Map;

/**
 * Member view of the group's wallet Access Card. The card is stored encrypted;
 * only the admin knows the passphrase, so members can safely keep, print and
 * save it as a social backup without being able to spend from the wallet.
 */
public class GroupWalletBackupActivity extends AppCompatActivity {
    public static final String EXTRA_GROUP_ID = "groupId";
    public static final String EXTRA_GROUP_NAME = "groupName";

    private ProgressBar progress;
    private Bitmap front, back;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
		getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE); // no screenshots or recents thumbnails of secrets

        com.harvana.app.utils.AppBar.setContent(this, com.harvana.app.R.layout.activity_group_wallet_backup, "Group wallet backup", "Encrypted social backup card");
        progress = findViewById(com.harvana.app.R.id.backup_progress);
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
                runOnUiThread(() -> fail(error != null && (com.harvana.app.utils.ErrorText.is(error, "404") || com.harvana.app.utils.ErrorText.is(error, "NOT_FOUND"))
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
            Bitmap f = HarvanaCardRenderer.renderFront(this, t, addr, label, null);
            Bitmap k = HarvanaCardRenderer.renderBack(this, t, qr, digits);
            runOnUiThread(() -> show(f, k));
        } catch (Exception e) {
            runOnUiThread(() -> fail("Couldn't render the card: " + com.harvana.app.utils.ErrorText.of(e)));
        }
    }

    private void show(Bitmap f, Bitmap k) {
        front = f; back = k;
        progress.setVisibility(View.GONE);
        ((ImageView) findViewById(com.harvana.app.R.id.iv_backup_front)).setImageBitmap(f);
        ((ImageView) findViewById(com.harvana.app.R.id.iv_backup_back)).setImageBitmap(k);
        findViewById(com.harvana.app.R.id.backup_card).setVisibility(View.VISIBLE);
        findViewById(com.harvana.app.R.id.btn_backup_pdf).setOnClickListener(v -> HarvanaCardRenderer.saveAsPdf(this, front, back,
            "harvana-group-card-" + System.currentTimeMillis(), exportCb("Saved to Downloads/Harvana")));
        findViewById(com.harvana.app.R.id.btn_backup_images).setOnClickListener(v -> {
            long s = System.currentTimeMillis();
            HarvanaCardRenderer.saveAsPng(this, front, "group-card-front-" + s, exportCb("Front saved"));
            HarvanaCardRenderer.saveAsPng(this, back, "group-card-back-" + s, exportCb("Back saved"));
        });
        findViewById(com.harvana.app.R.id.btn_backup_print).setOnClickListener(v -> {
            PrintHelper ph = new PrintHelper(this);
            ph.setScaleMode(PrintHelper.SCALE_MODE_FIT);
            ph.printBitmap("Group wallet card - front", front);
            ph.printBitmap("Group wallet card - back", back);
        });
    }

    private HarvanaCardRenderer.ExportCallback exportCb(String ok) {
        return new HarvanaCardRenderer.ExportCallback() {
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
        TextView t = findViewById(com.harvana.app.R.id.tv_backup_error);
        t.setText(msg);
        t.setVisibility(View.VISIBLE);
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
}
