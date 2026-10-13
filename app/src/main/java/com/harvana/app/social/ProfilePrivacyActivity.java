package com.harvana.app.social;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.utils.AppBar;

/**
 * What other members see on your profile. Two things you can turn on or off (age and city), a short bio, and a plain list of what
 * is always public or never shown, so nobody has to guess. Nothing is shown until you turn it on.
 */
public class ProfilePrivacyActivity extends AppCompatActivity {
    private SwitchCompat swAge, swCity;
    private EditText bio;
    private TextView status;
    private MaterialButton save;
    private boolean loaded;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        AppBar.setContent(this, com.harvana.app.R.layout.activity_profile_privacy, "Profile privacy", "Choose what others can see");
        swAge = findViewById(com.harvana.app.R.id.sw_show_age);
        swCity = findViewById(com.harvana.app.R.id.sw_show_city);
        bio = findViewById(com.harvana.app.R.id.et_profile_bio);
        save = findViewById(com.harvana.app.R.id.btn_privacy_save);
        status = findViewById(com.harvana.app.R.id.tv_privacy_status);
        bio.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(PublicProfileStore.BIO_MAX)});

        // The whole line toggles the switch, so the tap target is not the small thumb.
        findViewById(com.harvana.app.R.id.row_show_age).setOnClickListener(v -> { swAge.toggle(); dirty(); });
        findViewById(com.harvana.app.R.id.row_show_city).setOnClickListener(v -> { swCity.toggle(); dirty(); });

        findViewById(com.harvana.app.R.id.btn_privacy_preview).setOnClickListener(v -> {
            com.google.firebase.auth.FirebaseUser u = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (u == null) return;
            startActivity(new android.content.Intent(this, PublicProfileActivity.class).putExtra(PublicProfileActivity.EXTRA_UID, u.getUid()));
        });

        save.setOnClickListener(v -> {
            save.setEnabled(false);
            status.setText("Saving…");
            PublicProfileStore.savePrefs(this, swAge.isChecked(), swCity.isChecked(), bio.getText().toString(), (ok, err) -> runOnUiThread(() -> {
                if (ok) { status.setText("Saved"); Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show(); }
                else { status.setText("Couldn't save: " + err); save.setEnabled(true); }
            }));
        });
        bio.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { dirty(); }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void dirty() {
        if (loaded) { save.setEnabled(true); status.setText(""); }
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
}
