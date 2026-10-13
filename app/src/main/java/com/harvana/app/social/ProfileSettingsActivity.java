package com.harvana.app.social;

import android.content.Intent;
import android.os.Bundle;

import com.harvana.app.R;

import androidx.appcompat.app.AppCompatActivity;

import com.harvana.app.BlockedUsersActivity;
import com.harvana.app.EditProfileActivity;
import com.harvana.app.NotificationSettingsActivity;
import com.harvana.app.PrivacySettingsActivity;
import com.harvana.app.SecurityPinSettingsActivity;
import com.harvana.app.SettingsActivity;
import com.harvana.app.utils.AppBar;

/**
 * Settings that belong to my profile, opened from the profile page's Settings tile: who I am, who can see what, who I have blocked,
 * and how I'm protected and notified. The rest of the app's settings (currency, theme, wallets, backup) are one tap further down.
 */
public class ProfileSettingsActivity extends AppCompatActivity {

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        AppBar.setContent(this, R.layout.activity_profile_settings, "Profile settings", "Your details, privacy and safety");
        open(R.id.row_edit_profile, EditProfileActivity.class);
        open(R.id.row_contact_card, ContactCardActivity.class);
        open(R.id.row_my_qr, ProfileQrActivity.class);
        open(R.id.row_profile_privacy, ProfilePrivacyActivity.class);
        open(R.id.row_privacy_visibility, PrivacySettingsActivity.class);
        open(R.id.row_blocked, BlockedUsersActivity.class);
        open(R.id.row_pin_security, SecurityPinSettingsActivity.class);
        open(R.id.row_notifications, NotificationSettingsActivity.class);
        open(R.id.row_all_settings, SettingsActivity.class);
    }

    private void open(int rowId, Class<?> target) {
        findViewById(rowId).setOnClickListener(v -> startActivity(new Intent(this, target)));
    }
}
