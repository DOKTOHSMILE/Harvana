package com.harvana.app.social;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.HARVQRCodeGenerator;
import com.harvana.app.ScanQrCodeActivity;


/**
 * Your QR code: a card with your photo and @username that another member scans to open your profile. Share it as a picture, or scan
 * someone else's. The code holds only {@code harvana://user/<username>}; scanning it opens the same public profile anyone can see.
 */
public class ProfileQrActivity extends AppCompatActivity {
    public static final String LINK_PREFIX = "harvana://user/";
    private static final int RC_SCAN = 5101;

    private Bitmap qr;
    private String username = "";
    private String icon = "";
    private String displayName = "", trustTier = "";
    private int trustPercent = -1;

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        com.harvana.app.utils.AppBar.setContent(this, com.harvana.app.R.layout.activity_profile_qr, "My QR code", "Share or scan to connect");
        MaterialButton share = findViewById(com.harvana.app.R.id.btn_qr_share);
        share.setOnClickListener(v -> shareQr());
        findViewById(com.harvana.app.R.id.btn_qr_scan).setOnClickListener(v -> startActivityForResult(new Intent(this, ScanQrCodeActivity.class)
                .putExtra(ScanQrCodeActivity.EXTRA_TITLE, "Scan a Harvana profile code")
                .putExtra(ScanQrCodeActivity.EXTRA_MODE, ScanQrCodeActivity.MODE_ANY), RC_SCAN));

        PublicProfileStore.loadMine((card, err) -> runOnUiThread(() -> {
            if (card == null || card.username.isEmpty()) {
                findViewById(com.harvana.app.R.id.tv_qr_no_username).setVisibility(View.VISIBLE);
                share.setEnabled(false);
                return;
            }
            username = card.username;
            displayName = card.name == null ? "" : card.name;
            trustPercent = card.scored() ? card.trustPercent : -1;
            trustTier = card.trustTier == null ? "" : card.trustTier;
            icon = card.icon;
            qr = HARVQRCodeGenerator.generateQRCode(LINK_PREFIX + username, 720, null, true);
            showQrCard();
        }));
    }

    /** Fills the white card: the QR, the @username under it and the photo (or initial) on the top edge. */
    private void showQrCard() {
        ((ImageView) findViewById(com.harvana.app.R.id.iv_qr_code)).setImageBitmap(qr);
        ((TextView) findViewById(com.harvana.app.R.id.tv_qr_username)).setText("@" + username.toUpperCase(java.util.Locale.US));
        TextView initial = findViewById(com.harvana.app.R.id.tv_qr_initial);
        initial.setText(username.substring(0, 1).toUpperCase(java.util.Locale.US));
        if (icon != null && icon.startsWith("http")) {
            ImageView photo = findViewById(com.harvana.app.R.id.iv_qr_photo);
            photo.setVisibility(View.VISIBLE);
            try { com.bumptech.glide.Glide.with(this).load(icon).circleCrop().into(photo); initial.setVisibility(View.INVISIBLE); } catch (Exception ignored) { }
        }
        findViewById(com.harvana.app.R.id.qr_card_wrap).setVisibility(View.VISIBLE);
    }

    /** Shares the profile card (photo-free picture with the QR) and a message together, like Share on the profile. */
    private void shareQr() {
        if (qr == null) return;
        ProfileShare.share(this, displayName, username, trustPercent, trustTier);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != RC_SCAN || resultCode != Activity.RESULT_OK || data == null) return;
        String text = data.getStringExtra(ScanQrCodeActivity.EXTRA_RESULT_TEXT);
        if (text == null || !text.startsWith(LINK_PREFIX)) { com.harvana.app.scan.QrRouter.route(this, text); return; }   // any other Harvana code opens where it belongs
        String name = text.substring(LINK_PREFIX.length()).trim();
        SocialStore.findByUsername(name, (person, err) -> runOnUiThread(() -> {
            if (person == null) { Toast.makeText(this, err == null ? "No member found." : err, Toast.LENGTH_LONG).show(); return; }
            startActivity(new Intent(this, PublicProfileActivity.class).putExtra(PublicProfileActivity.EXTRA_UID, person.uid));
        }));
    }
}
