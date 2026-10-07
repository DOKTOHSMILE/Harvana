package com.harvana.app.social;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.HARVQRCodeGenerator;
import com.harvana.app.ScanQrCodeActivity;
import com.harvana.app.utils.UiColors;

import java.io.File;
import java.io.FileOutputStream;

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

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        FrameLayout screen = new FrameLayout(this);
        screen.setBackgroundColor(UiColors.background());
        com.harvana.app.utils.AppBar.setContent(this, screen, "My QR code", "Share or scan to connect");

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        screen.addView(column, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final FrameLayout cardHost = new FrameLayout(this);
        LinearLayout.LayoutParams hostLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        column.addView(cardHost, hostLp);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        int p = SocialUi.dp(this, 20);
        panel.setPadding(p, p, p, p);
        float r = SocialUi.dp(this, 24);
        GradientDrawable pbg = new GradientDrawable();
        pbg.setColor(UiColors.surface());
        pbg.setCornerRadii(new float[] {r, r, r, r, 0, 0, 0, 0});
        panel.setBackground(pbg);
        panel.addView(SocialUi.text(this, "QR code", 20, true, UiColors.textPrimary()));
        TextView hint = SocialUi.text(this, "Anyone who scans this opens your public profile.", 13, false, UiColors.textSecondary());
        hint.setPadding(0, SocialUi.dp(this, 4), 0, SocialUi.dp(this, 14));
        panel.addView(hint);
        MaterialButton share = SocialUi.button(this, "Share QR code", false);
        share.setOnClickListener(v -> shareQr());
        panel.addView(share, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SocialUi.dp(this, 52)));
        MaterialButton scan = SocialUi.button(this, "Scan a QR code", true);
        scan.setOnClickListener(v -> startActivityForResult(new Intent(this, ScanQrCodeActivity.class)
                .putExtra(ScanQrCodeActivity.EXTRA_TITLE, "Scan a Harvana profile code")
                .putExtra(ScanQrCodeActivity.EXTRA_MODE, ScanQrCodeActivity.MODE_ANY), RC_SCAN));
        LinearLayout.LayoutParams scanLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SocialUi.dp(this, 52));
        scanLp.topMargin = SocialUi.dp(this, 10);
        panel.addView(scan, scanLp);
        column.addView(panel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        PublicProfileStore.loadMine((card, err) -> runOnUiThread(() -> {
            if (card == null || card.username.isEmpty()) {
                TextView none = SocialUi.text(this, "Choose a username first (menu on your profile > Change username). Your QR code is made from it.",
                        14, false, UiColors.textSecondary());
                none.setGravity(Gravity.CENTER);
                none.setPadding(SocialUi.dp(this, 32), SocialUi.dp(this, 48), SocialUi.dp(this, 32), 0);
                cardHost.addView(none);
                share.setEnabled(false);
                return;
            }
            username = card.username;
            icon = card.icon;
            qr = HARVQRCodeGenerator.generateQRCode(LINK_PREFIX + username, 720, null, true);
            cardHost.addView(qrCard());
        }));
    }

    /** A white card with the QR, the @username under it and the photo overlapping the top edge. */
    private View qrCard() {
        FrameLayout wrap = new FrameLayout(this);
        int cardW = SocialUi.dp(this, 280);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(SocialUi.dp(this, 20), SocialUi.dp(this, 56), SocialUi.dp(this, 20), SocialUi.dp(this, 20));
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFFFFFFFF);
        g.setCornerRadius(SocialUi.dp(this, 26));
        card.setBackground(g);
        ImageView img = new ImageView(this);
        img.setImageBitmap(qr);
        card.addView(img, new LinearLayout.LayoutParams(SocialUi.dp(this, 220), SocialUi.dp(this, 220)));
        TextView name = SocialUi.text(this, "@" + username.toUpperCase(java.util.Locale.US), 22, true, 0xFF1A1A1A);
        name.setTypeface(null, Typeface.BOLD);
        name.setPadding(0, SocialUi.dp(this, 12), 0, 0);
        card.addView(name);
        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(cardW, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        cardLp.topMargin = SocialUi.dp(this, 30);
        wrap.addView(card, cardLp);

        int s = SocialUi.dp(this, 40);
        FrameLayout face = new FrameLayout(this);
        TextView initial = SocialUi.text(this, username.substring(0, 1).toUpperCase(java.util.Locale.US), 15, true, 0xFFFFFFFF);
        initial.setGravity(Gravity.CENTER);
        GradientDrawable disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        disc.setColors(new int[] {0xFF2BA597, 0xFF0B6B61});
        disc.setOrientation(GradientDrawable.Orientation.TL_BR);
        disc.setStroke(SocialUi.dp(this, 2), 0xFFFFFFFF);
        initial.setBackground(disc);
        face.addView(initial, new FrameLayout.LayoutParams(s, s));
        if (icon.startsWith("http")) {
            ImageView photo = new ImageView(this);
            photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
            face.addView(photo, new FrameLayout.LayoutParams(s, s));
            try { com.bumptech.glide.Glide.with(this).load(icon).circleCrop().into(photo); initial.setVisibility(View.INVISIBLE); } catch (Exception ignored) { }
        }
        // the photo sits centred on the card's top edge: the card has a 30dp margin and the photo is 72dp, so it overlaps by 36dp
        FrameLayout.LayoutParams faceLp = new FrameLayout.LayoutParams(s, s, Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        wrap.addView(face, faceLp);
        wrap.setMinimumHeight(SocialUi.dp(this, 380));
        return wrap;
    }

    private void shareQr() {
        if (qr == null) return;
        try {
            File dir = new File(getCacheDir(), "images");
            dir.mkdirs();
            File f = new File(dir, "harvana-qr.png");
            try (FileOutputStream out = new FileOutputStream(f)) { qr.compress(Bitmap.CompressFormat.PNG, 100, out); }
            Uri uri = FileProvider.getUriForFile(this, "com.harvana.app.fileprovider", f);
            Intent send = new Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
                    .putExtra(Intent.EXTRA_TEXT, "Add me on Harvana: @" + username).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, "Share QR code"));
        } catch (Exception e) {
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, "Add me on Harvana: @" + username), "Share"));
        }
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
