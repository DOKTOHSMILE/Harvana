package com.harvana.app.social;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.utils.AppBar;
import com.harvana.app.utils.UiColors;

/**
 * What other members see on your profile. Two things you can turn on or off (age and city), a short bio, and a plain list of what
 * is always public or never shown, so nobody has to guess. Nothing is shown until you turn it on.
 */
public class ProfilePrivacyActivity extends AppCompatActivity {
    private SwitchCompat swAge, swCity;
    private EditText bio;
    private TextView counter, status;
    private MaterialButton save;
    private boolean loaded;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(UiColors.background());
        sv.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = SocialUi.dp(this, 16);
        root.setPadding(p, p, p, SocialUi.dp(this, 28));
        sv.addView(root);
        AppBar.setContent(this, sv, "Profile privacy", "Choose what others can see");

        LinearLayout choose = SocialUi.card(this);
        choose.addView(SocialUi.text(this, "You choose", 16, true, UiColors.textPrimary()));
        swAge = new SwitchCompat(this);
        swCity = new SwitchCompat(this);
        choose.addView(toggleRow("Show my age", "Others see your age in years, never your date of birth.", swAge));
        choose.addView(toggleRow("Show my city", "Others see your city next to your state and country.", swCity));
        TextView bioLabel = SocialUi.text(this, "Short bio (public)", 14, true, UiColors.textPrimary());
        bioLabel.setPadding(0, SocialUi.dp(this, 18), 0, SocialUi.dp(this, 4));
        choose.addView(bioLabel);
        bio = new EditText(this);
        bio.setHint("Tell people a little about you");
        bio.setTextColor(UiColors.textPrimary());
        bio.setHintTextColor(UiColors.textTertiary());
        bio.setMinLines(2);
        bio.setGravity(Gravity.TOP | Gravity.START);
        bio.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(PublicProfileStore.BIO_MAX)});
        choose.addView(bio);
        counter = SocialUi.text(this, "0 / " + PublicProfileStore.BIO_MAX, 11.5f, false, UiColors.textTertiary());
        counter.setGravity(Gravity.END);
        choose.addView(counter);
        bio.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { counter.setText(s.length() + " / " + PublicProfileStore.BIO_MAX); }
            @Override public void afterTextChanged(Editable s) { }
        });
        save = SocialUi.button(this, "Save", false);
        save.setEnabled(false);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SocialUi.dp(this, 48));
        slp.topMargin = SocialUi.dp(this, 16);
        choose.addView(save, slp);
        status = SocialUi.text(this, "", 12.5f, false, UiColors.textSecondary());
        status.setPadding(0, SocialUi.dp(this, 8), 0, 0);
        choose.addView(status);
        root.addView(choose);

        LinearLayout always = SocialUi.card(this);
        always.addView(SocialUi.text(this, "Always public", 16, true, UiColors.textPrimary()));
        always.addView(fixedRow("Trust score and its parts", "The network works it out. It can't be hidden or changed."));
        always.addView(fixedRow("Country and state", "Shown on every profile."));
        always.addView(fixedRow("Number of groups and loans", "Counts only: groups, completed cycles, loans and pending loans."));
        always.addView(fixedRow("Badges", "The ones the network awards."));
        root.addView(always);

        LinearLayout never = SocialUi.card(this);
        never.addView(SocialUi.text(this, "Never shown", 16, true, UiColors.textPrimary()));
        never.addView(fixedRow("Amounts", "Money you owe, saved or borrowed is never on your profile."));
        never.addView(fixedRow("Group names", "People only see groups you both belong to."));
        never.addView(fixedRow("Date of birth, phone, email, address", "These stay private to you."));
        root.addView(never);

        MaterialButton see = SocialUi.button(this, "See how my profile looks to others", true);
        see.setOnClickListener(v -> {
            com.google.firebase.auth.FirebaseUser u = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (u == null) return;
            startActivity(new android.content.Intent(this, PublicProfileActivity.class).putExtra(PublicProfileActivity.EXTRA_UID, u.getUid()));
        });
        root.addView(see, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SocialUi.dp(this, 48)));

        save.setOnClickListener(v -> {
            save.setEnabled(false);
            status.setText("Saving…");
            PublicProfileStore.savePrefs(this, swAge.isChecked(), swCity.isChecked(), bio.getText().toString(), (ok, err) -> runOnUiThread(() -> {
                if (ok) { status.setText("Saved. Others see the change straight away."); Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show(); }
                else { status.setText("Couldn't save: " + err); save.setEnabled(true); }
            }));
        });
        View.OnClickListener dirty = v -> { if (loaded) { save.setEnabled(true); status.setText(""); } };
        swAge.setOnClickListener(dirty);
        swCity.setOnClickListener(dirty);
        bio.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { if (loaded) { save.setEnabled(true); status.setText(""); } }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        PublicProfileStore.loadMine((card, err) -> runOnUiThread(() -> {
            if (card == null) { status.setText("Couldn't load your settings: " + err); return; }
            loaded = false;
            swAge.setChecked(card.showAge);
            swCity.setChecked(card.showCity);
            bio.setText(card.bio);
            loaded = true;
            save.setEnabled(false);
        }));
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    /** A row where the whole line toggles the switch, so the tap target is not the small thumb. */
    private View toggleRow(String title, String hint, SwitchCompat sw) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, SocialUi.dp(this, 14), 0, SocialUi.dp(this, 4));
        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(SocialUi.text(this, title, 15, true, UiColors.textPrimary()));
        TextView h = SocialUi.text(this, hint, 12.5f, false, UiColors.textSecondary());
        h.setPadding(0, SocialUi.dp(this, 2), SocialUi.dp(this, 12), 0);
        text.addView(h);
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        sw.setClickable(false);
        row.addView(sw);
        row.setMinimumHeight(SocialUi.dp(this, 56));
        row.setClickable(true);
        row.setOnClickListener(v -> { sw.toggle(); if (loaded) { save.setEnabled(true); status.setText(""); } });
        return row;
    }

    private View fixedRow(String title, String hint) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, SocialUi.dp(this, 12), 0, 0);
        row.addView(SocialUi.text(this, title, 14, true, UiColors.textPrimary()));
        row.addView(SocialUi.text(this, hint, 12.5f, false, UiColors.textSecondary()));
        return row;
    }
}
