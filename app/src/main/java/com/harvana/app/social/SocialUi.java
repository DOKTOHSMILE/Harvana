package com.harvana.app.social;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.utils.UiColors;

/** Small view builders shared by the friends and contact-card screens (themed through UiColors). */
public final class SocialUi {
    private SocialUi() {}

    public static int dp(Context c, float v) { return (int) (v * c.getResources().getDisplayMetrics().density); }

    public static TextView text(Context c, String s, float sp, boolean bold, int color) {
        TextView t = new TextView(c);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    /** The row chevron (duotone icon, accent colour) used where a row opens another page. */
    public static android.widget.ImageView chevron(Context c) {
        android.widget.ImageView v = new android.widget.ImageView(c);
        v.setImageResource(com.harvana.app.R.drawable.ic_arrow_right);
        androidx.core.widget.ImageViewCompat.setImageTintList(v, android.content.res.ColorStateList.valueOf(com.harvana.app.utils.UiColors.accent()));
        int s = dp(c, 20);
        v.setLayoutParams(new android.widget.LinearLayout.LayoutParams(s, s));
        return v;
    }

    public static MaterialButton button(Context c, String label, boolean outlined) {
        MaterialButton b = outlined
            ? new MaterialButton(c, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
            : new MaterialButton(c);
        b.setText(label); b.setAllCaps(false);
        return b;
    }

    /** A short button for rows: 34dp high, small text, no letter spacing, so a row of two fits a small phone. */
    public static MaterialButton compactButton(Context c, String label, boolean outlined) {
        MaterialButton b = button(c, label, outlined);
        b.setTextSize(12);
        b.setLetterSpacing(0f);
        b.setInsetTop(0); b.setInsetBottom(0);
        b.setMinHeight(0); b.setMinimumHeight(0); b.setMinWidth(0); b.setMinimumWidth(0);
        b.setPadding(dp(c, 12), 0, dp(c, 12), 0);
        b.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(c, 34)));
        return b;
    }

    public static EditText edit(Context c, String hint) {
        EditText e = new com.google.android.material.textfield.TextInputEditText(c);
        e.setHint(hint); e.setSingleLine(true);
        e.setTextColor(UiColors.textPrimary());
        e.setHintTextColor(UiColors.textTertiary());
        return e;
    }

    /** A rounded surface block with padding, used as a card. */
    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(UiColors.surface()); g.setCornerRadius(dp(c, 14));
        g.setStroke(dp(c, 1), (UiColors.textTertiary() & 0x00FFFFFF) | 0x33000000);
        l.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, 14);
        l.setLayoutParams(lp);
        return l;
    }

    /** name + @handle on the left, one action button on the right. */
    public static LinearLayout personRow(Context c, Person p, String action, boolean outlined, View.OnClickListener onAction, View.OnClickListener onRow) {
        LinearLayout row = (LinearLayout) android.view.LayoutInflater.from(c).inflate(com.harvana.app.R.layout.item_person_row, null, false);
        TextView initials = row.findViewById(com.harvana.app.R.id.tv_person_initials);
        initials.setText(p.initials());
        if (p.photo.startsWith("http")) {
            try {
                com.bumptech.glide.Glide.with(c).load(p.photo).circleCrop().into((ImageView) row.findViewById(com.harvana.app.R.id.iv_person_photo));
                initials.setVisibility(View.INVISIBLE);
            } catch (Exception ignored) { }
        }
        ((TextView) row.findViewById(com.harvana.app.R.id.tv_person_name)).setText(p.name);
        TextView handle = row.findViewById(com.harvana.app.R.id.tv_person_handle);
        if (!p.handle().isEmpty()) { handle.setText(p.handle()); handle.setVisibility(View.VISIBLE); }
        com.harvana.app.trust.ProfileTrust.attach(row.findViewById(com.harvana.app.R.id.tv_person_trust), p.uid);
        if (action != null) {
            MaterialButton b = row.findViewById(com.harvana.app.R.id.btn_person_action);
            b.setText(action);
            if (outlined) {
                b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
                b.setStrokeColorResource(com.harvana.app.R.color.colorAccent);
                b.setStrokeWidth(dp(c, 1));
                b.setTextColor(androidx.core.content.ContextCompat.getColor(c, com.harvana.app.R.color.colorAccentStrong));
            }
            b.setVisibility(View.VISIBLE);
            b.setOnClickListener(onAction);
        }
        if (onRow != null) { row.setClickable(true); row.setOnClickListener(onRow); }
        return row;
    }
}
