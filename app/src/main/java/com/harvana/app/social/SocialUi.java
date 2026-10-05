package com.harvana.app.social;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.utils.UiColors;

/** Small view builders shared by the friends and contact-card screens (themed through UiColors). */
final class SocialUi {
    private SocialUi() {}

    static int dp(Context c, float v) { return (int) (v * c.getResources().getDisplayMetrics().density); }

    static TextView text(Context c, String s, float sp, boolean bold, int color) {
        TextView t = new TextView(c);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    static MaterialButton button(Context c, String label, boolean outlined) {
        MaterialButton b = outlined
            ? new MaterialButton(c, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
            : new MaterialButton(c);
        b.setText(label); b.setAllCaps(false);
        return b;
    }

    /** A short button for rows: 34dp high, small text, no letter spacing, so a row of two fits a small phone. */
    static MaterialButton compactButton(Context c, String label, boolean outlined) {
        MaterialButton b = button(c, label, outlined);
        b.setTextSize(12);
        b.setLetterSpacing(0f);
        b.setInsetTop(0); b.setInsetBottom(0);
        b.setMinHeight(0); b.setMinimumHeight(0); b.setMinWidth(0); b.setMinimumWidth(0);
        b.setPadding(dp(c, 12), 0, dp(c, 12), 0);
        b.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(c, 34)));
        return b;
    }

    static EditText edit(Context c, String hint) {
        EditText e = new EditText(c);
        e.setHint(hint); e.setSingleLine(true);
        e.setTextColor(UiColors.textPrimary());
        e.setHintTextColor(UiColors.textTertiary());
        return e;
    }

    /** A rounded surface block with padding, used as a card. */
    static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 14);
        l.setPadding(p, p, p, p);
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(UiColors.surface()); g.setCornerRadius(dp(c, 14));
        g.setStroke(dp(c, 1), (UiColors.textTertiary() & 0x00FFFFFF) | 0x33000000);
        l.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, 12);
        l.setLayoutParams(lp);
        return l;
    }

    /** name + @handle on the left, one action button on the right. */
    static LinearLayout personRow(Context c, Person p, String action, boolean outlined, View.OnClickListener onAction, View.OnClickListener onRow) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(c, 8), 0, dp(c, 8));
        TextView face = text(c, p.initials(), 14, true, 0xFF1A1208);
        face.setGravity(Gravity.CENTER);
        int size = dp(c, 40);
        GradientDrawable disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        disc.setColor(0xFFF0B45A);
        face.setBackground(disc);
        LinearLayout.LayoutParams faceLp = new LinearLayout.LayoutParams(size, size);
        faceLp.rightMargin = dp(c, 12);
        row.addView(face, faceLp);
        if (p.photo.startsWith("http")) {
            ImageView photo = new ImageView(c);
            photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
            try {
                com.bumptech.glide.Glide.with(c).load(p.photo).circleCrop().into(photo);
                row.removeView(face);
                row.addView(photo, 0, faceLp);
            } catch (Exception ignored) { }
        }
        LinearLayout names = new LinearLayout(c);
        names.setOrientation(LinearLayout.VERTICAL);
        names.addView(text(c, p.name, 15, true, UiColors.textPrimary()));
        if (!p.handle().isEmpty()) names.addView(text(c, p.handle(), 12, false, UiColors.textSecondary()));
        TextView trust = text(c, "", 11, true, UiColors.textSecondary());
        trust.setPadding(dp(c, 8), dp(c, 2), dp(c, 8), dp(c, 2));
        LinearLayout.LayoutParams trustLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        trustLp.topMargin = dp(c, 4);
        names.addView(trust, trustLp);
        com.harvana.app.trust.ProfileTrust.attach(trust, p.uid);
        row.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (action != null) {
            MaterialButton b = button(c, action, outlined);
            b.setOnClickListener(onAction);
            row.addView(b);
        }
        if (onRow != null) { row.setClickable(true); row.setOnClickListener(onRow); }
        return row;
    }
}
