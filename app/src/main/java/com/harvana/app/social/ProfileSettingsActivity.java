package com.harvana.app.social;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.harvana.app.BlockedUsersActivity;
import com.harvana.app.EditProfileActivity;
import com.harvana.app.NotificationSettingsActivity;
import com.harvana.app.PrivacySettingsActivity;
import com.harvana.app.SecurityPinSettingsActivity;
import com.harvana.app.SettingsActivity;
import com.harvana.app.utils.AppBar;
import com.harvana.app.utils.UiColors;

/**
 * Settings that belong to my profile, opened from the profile page's Settings tile: who I am, who can see what, who I have blocked,
 * and how I'm protected and notified. The rest of the app's settings (currency, theme, wallets, backup) are one tap further down.
 */
public class ProfileSettingsActivity extends AppCompatActivity {

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(UiColors.background());
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = SocialUi.dp(this, 16);
        root.setPadding(p, p, p, SocialUi.dp(this, 28));
        sv.addView(root);
        AppBar.setContent(this, sv, "Profile settings", "Your details, privacy and safety");

        LinearLayout you = section(root, "Your profile");
        row(you, "Edit profile", "Photo, name, occupation, address, city", EditProfileActivity.class);
        row(you, "Username and contact card", "Username, phone, chat links, let contacts find me", ContactCardActivity.class);
        row(you, "My QR code", "Show or save your code so people can follow you", ProfileQrActivity.class);

        LinearLayout privacy = section(root, "Privacy");
        row(privacy, "Profile privacy", "Bio, show or hide your age and city", ProfilePrivacyActivity.class);
        row(privacy, "Privacy and visibility", "What other members can see about you", PrivacySettingsActivity.class);
        row(privacy, "Blocked users", "People you have blocked", BlockedUsersActivity.class);

        LinearLayout safety = section(root, "Safety and alerts");
        row(safety, "PIN and security", "Change your PIN, biometrics, lock", SecurityPinSettingsActivity.class);
        row(safety, "Notifications", "Which alerts you receive", NotificationSettingsActivity.class);

        LinearLayout app = section(root, "More");
        row(app, "All app settings", "Currency, theme, wallets, backup and more", SettingsActivity.class);
    }

    private LinearLayout section(LinearLayout root, String title) {
        TextView t = SocialUi.text(this, title, 13, true, UiColors.textSecondary());
        t.setAllCaps(true);
        t.setLetterSpacing(0.06f);
        t.setPadding(SocialUi.dp(this, 4), SocialUi.dp(this, 6), 0, SocialUi.dp(this, 8));
        root.addView(t);
        LinearLayout card = SocialUi.card(this);
        card.setPadding(SocialUi.dp(this, 16), SocialUi.dp(this, 4), SocialUi.dp(this, 16), SocialUi.dp(this, 4));
        root.addView(card);
        return card;
    }

    private void row(LinearLayout card, String title, String caption, Class<?> target) {
        if (card.getChildCount() > 0) {
            View line = new View(this);
            line.setBackgroundColor((UiColors.textTertiary() & 0x00FFFFFF) | 0x33000000);
            card.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, SocialUi.dp(this, 14), 0, SocialUi.dp(this, 14));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.addView(SocialUi.text(this, title, 15, true, UiColors.textPrimary()));
        TextView sub = SocialUi.text(this, caption, 12.5f, false, UiColors.textSecondary());
        sub.setPadding(0, SocialUi.dp(this, 2), 0, 0);
        words.addView(sub);
        row.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(SocialUi.text(this, "›", 22, true, UiColors.accent()));
        row.setClickable(true);
        row.setOnClickListener(v -> startActivity(new Intent(this, target)));
        card.addView(row);
    }
}
