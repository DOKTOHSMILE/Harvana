package com.harvana.app;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.harvana.app.firestore.UserProfileManager;
import com.harvana.app.offline.OfflineCacheManager;
import com.harvana.app.social.ContactCardActivity;
import com.harvana.app.social.ProfilePrivacyActivity;
import com.harvana.app.social.PublicProfileStore;
import com.harvana.app.social.SocialUi;
import com.harvana.app.utils.AppBar;
import com.harvana.app.utils.Shimmer;
import com.harvana.app.utils.UiColors;

import java.util.HashMap;
import java.util.Map;

/**
 * Edit my profile: photo, name, occupation, address and city. Things that other parts of the app depend on (country, state, date of
 * birth, email, ID, and the name once an identity check has passed) are shown but not editable here, with the reason. The phone number
 * and username live on the contact card, bio and age/city visibility on profile privacy; both are one tap away.
 */
public class EditProfileActivity extends AppCompatActivity {
    private static final int RC_PHOTO = 612;

    private LinearLayout page;
    private String uid = "";
    private Map<String, Object> data = new HashMap<>();
    private boolean nameLocked;
    private EditText first, last, job, address, city;
    private MaterialButton save;
    private ImageView photo;
    private TextView initials, photoStatus;
    private ProgressBar photoBusy;
    private String newIcon;      // set once a picked photo has been uploaded

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        com.google.firebase.auth.FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me == null) { finish(); return; }
        uid = me.getUid();
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(UiColors.background());
        sv.setFillViewport(true);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        int p = SocialUi.dp(this, 16);
        page.setPadding(p, p, p, SocialUi.dp(this, 28));
        sv.addView(page);
        AppBar.setContent(this, sv, "Edit profile", "Your photo and details");
        page.addView(Shimmer.cards(this, 3));

        Map<String, Object> cached = OfflineCacheManager.load(getApplicationContext(), "profile_" + uid);
        if (cached != null && !cached.isEmpty()) { data = cached; build(); }
        UserProfileManager.getInstance(getApplicationContext()).getProfile(uid, new UserProfileManager.DocCallback() {
            @Override public void onSuccess(Map<String, Object> d) {
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    boolean editing = first != null && dirty();
                    data = d;
                    if (!editing) build();
                });
            }
            @Override public void onError(String error) {
                runOnUiThread(() -> {
                    if (isFinishing() || first != null) return;
                    page.removeAllViews();
                    page.addView(SocialUi.text(EditProfileActivity.this, "Couldn't load your details: " + error, 14, false, UiColors.error()));
                });
            }
        });
    }

    private String s(String k) {
        Object v = data.get(k);
        return v == null || "null".equals(String.valueOf(v)) ? "" : String.valueOf(v).trim();
    }

    // ── Page ─────────────────────────────────────────────────────────────

    private void build() {
        page.removeAllViews();
        nameLocked = !s("v_type").isEmpty();
        newIcon = null;
        page.addView(photoBlock());

        LinearLayout about = SocialUi.card(this);
        about.addView(SocialUi.text(this, "About you", 16, true, UiColors.textPrimary()));
        first = field(about, "First name", s("first_name"), InputType.TYPE_TEXT_FLAG_CAP_WORDS, nameLocked);
        last = field(about, "Last name", s("last_name"), InputType.TYPE_TEXT_FLAG_CAP_WORDS, nameLocked);
        if (nameLocked) about.addView(note("Your name was confirmed by an identity check, so it can't be changed here."));
        job = field(about, "Occupation", s("occupation"), InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, false);
        address = field(about, "Home address", s("address"), InputType.TYPE_TEXT_FLAG_CAP_WORDS, false);
        city = field(about, "City", s("city"), InputType.TYPE_TEXT_FLAG_CAP_WORDS, false);
        about.addView(note("Your address stays private. Your city is shown on your profile only if you turn that on in Profile privacy."));
        page.addView(about);

        LinearLayout fixed = SocialUi.card(this);
        fixed.addView(SocialUi.text(this, "Fixed details", 16, true, UiColors.textPrimary()));
        fixed.addView(readOnly("Country", s("country")));
        fixed.addView(readOnly("State", s("state")));
        fixed.addView(readOnly("Email", s("email")));
        fixed.addView(readOnly("Date of birth", s("dob")));
        fixed.addView(readOnly("Harvana ID", s("pid")));
        fixed.addView(note("These feed your trust score and your account's safety, so they are set when you sign up and can't be edited."));
        page.addView(fixed);

        LinearLayout more = SocialUi.card(this);
        more.addView(SocialUi.text(this, "Also on your profile", 16, true, UiColors.textPrimary()));
        more.addView(link("Username, phone and chat links", "My contact card", ContactCardActivity.class));
        more.addView(link("Bio, show or hide age and city", "Profile privacy", ProfilePrivacyActivity.class));
        page.addView(more);

        save = SocialUi.button(this, "Save changes", false);
        save.setEnabled(false);
        save.setOnClickListener(v -> save());
        page.addView(save, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SocialUi.dp(this, 52)));

        TextWatcher w = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence a, int b, int c, int d) { }
            @Override public void onTextChanged(CharSequence a, int b, int c, int d) { }
            @Override public void afterTextChanged(Editable e) { save.setEnabled(dirty()); }
        };
        for (EditText e : new EditText[] {first, last, job, address, city}) e.addTextChangedListener(w);
    }

    private View photoBlock() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(0, SocialUi.dp(this, 4), 0, SocialUi.dp(this, 16));
        int size = SocialUi.dp(this, 104);
        FrameLayout face = new FrameLayout(this);
        initials = SocialUi.text(this, initialsOf(), 34, true, UiColors.onAccent());
        initials.setGravity(Gravity.CENTER);
        GradientDrawable disc = new GradientDrawable();
        disc.setShape(GradientDrawable.OVAL);
        disc.setColor(UiColors.accent());
        initials.setBackground(disc);
        face.addView(initials, new FrameLayout.LayoutParams(size, size));
        photo = new ImageView(this);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        face.addView(photo, new FrameLayout.LayoutParams(size, size));
        photoBusy = new ProgressBar(this);
        photoBusy.setVisibility(View.GONE);
        face.addView(photoBusy, new FrameLayout.LayoutParams(SocialUi.dp(this, 36), SocialUi.dp(this, 36), Gravity.CENTER));
        showPhoto(s("icon"));
        box.addView(face, new LinearLayout.LayoutParams(size, size));

        MaterialButton change = SocialUi.compactButton(this, "Change photo", true);
        change.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
            startActivityForResult(Intent.createChooser(pick, "Choose a photo"), RC_PHOTO);
        });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, SocialUi.dp(this, 36));
        cp.topMargin = SocialUi.dp(this, 12);
        box.addView(change, cp);
        photoStatus = SocialUi.text(this, "", 12, false, UiColors.textSecondary());
        photoStatus.setPadding(0, SocialUi.dp(this, 6), 0, 0);
        box.addView(photoStatus);
        return box;
    }

    private void showPhoto(String url) {
        if (url != null && url.startsWith("http")) {
            initials.setVisibility(View.INVISIBLE);
            Glide.with(this).load(url).circleCrop().into(photo);
        } else {
            initials.setVisibility(View.VISIBLE);
            photo.setImageDrawable(null);
        }
    }

    private String initialsOf() {
        String f = s("first_name"), l = s("last_name");
        String out = (f.isEmpty() ? "" : f.substring(0, 1)) + (l.isEmpty() ? "" : l.substring(0, 1));
        return out.isEmpty() ? "?" : out.toUpperCase(java.util.Locale.US);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent result) {
        super.onActivityResult(req, res, result);
        if (req != RC_PHOTO || res != RESULT_OK || result == null || result.getData() == null) return;
        final Uri picked = result.getData();
        Glide.with(this).load(picked).circleCrop().into(photo);
        initials.setVisibility(View.INVISIBLE);
        photoBusy.setVisibility(View.VISIBLE);
        photoStatus.setText("Uploading…");
        if (save != null) save.setEnabled(false);
        StorageReference ref = FirebaseStorage.getInstance().getReference().child("profile_icons/" + uid + "/" + System.currentTimeMillis() + ".jpg");
        ref.putFile(picked)
            .addOnSuccessListener(t -> ref.getDownloadUrl().addOnSuccessListener(url -> {
                photoBusy.setVisibility(View.GONE);
                photoStatus.setText("Photo ready. Press Save changes.");
                newIcon = url.toString();
                if (save != null) save.setEnabled(true);
            }).addOnFailureListener(this::photoFailed))
            .addOnFailureListener(this::photoFailed);
    }

    private void photoFailed(Exception e) {
        photoBusy.setVisibility(View.GONE);
        photoStatus.setText("Couldn't upload the photo: " + (e.getMessage() == null ? "try again" : e.getMessage()));
        newIcon = null;
        showPhoto(s("icon"));
        if (save != null) save.setEnabled(dirty());
    }

    // ── Fields ───────────────────────────────────────────────────────────

    private EditText field(LinearLayout into, String label, String value, int caps, boolean locked) {
        TextView l = SocialUi.text(this, label, 13, false, UiColors.textSecondary());
        l.setPadding(0, SocialUi.dp(this, 14), 0, 0);
        into.addView(l);
        EditText e = SocialUi.edit(this, label);
        e.setInputType(InputType.TYPE_CLASS_TEXT | caps);
        e.setText(value);
        e.setEnabled(!locked);
        e.setAlpha(locked ? 0.6f : 1f);
        into.addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return e;
    }

    private View readOnly(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, SocialUi.dp(this, 12), 0, 0);
        row.addView(SocialUi.text(this, label, 13, false, UiColors.textSecondary()));
        TextView v = SocialUi.text(this, value.isEmpty() ? "Not provided" : value, 15, false, value.isEmpty() ? UiColors.textTertiary() : UiColors.textPrimary());
        v.setPadding(0, SocialUi.dp(this, 2), 0, 0);
        row.addView(v);
        return row;
    }

    private TextView note(String text) {
        TextView t = SocialUi.text(this, text, 12, false, UiColors.textTertiary());
        t.setPadding(0, SocialUi.dp(this, 10), 0, 0);
        return t;
    }

    private View link(String caption, String title, Class<?> target) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, SocialUi.dp(this, 12), 0, 0);
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.addView(SocialUi.text(this, title, 15, true, UiColors.textPrimary()));
        words.addView(SocialUi.text(this, caption, 12, false, UiColors.textSecondary()));
        row.addView(words, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(SocialUi.text(this, "›", 22, true, UiColors.accent()));
        row.setClickable(true);
        row.setOnClickListener(v -> startActivity(new Intent(this, target)));
        return row;
    }

    // ── Saving ───────────────────────────────────────────────────────────

    private boolean dirty() {
        if (first == null) return false;
        return newIcon != null
            || !first.getText().toString().trim().equals(s("first_name")) || !last.getText().toString().trim().equals(s("last_name"))
            || !job.getText().toString().trim().equals(s("occupation")) || !address.getText().toString().trim().equals(s("address"))
            || !city.getText().toString().trim().equals(s("city"));
    }

    private void save() {
        String f = first.getText().toString().trim(), l = last.getText().toString().trim();
        String j = job.getText().toString().trim(), a = address.getText().toString().trim(), c = city.getText().toString().trim();
        if (!nameLocked) {
            if (f.isEmpty() || l.isEmpty()) { toast("Enter your first and last name."); return; }
            if (f.length() > 40 || l.length() > 40) { toast("Names can be up to 40 characters."); return; }
            if (!f.matches("^[\\p{L}][\\p{L} .'-]*$") || !l.matches("^[\\p{L}][\\p{L} .'-]*$")) { toast("Names can have letters, spaces, hyphens and apostrophes."); return; }
        }
        if (j.length() > 60) { toast("Occupation can be up to 60 characters."); return; }
        if (a.length() > 120) { toast("Address can be up to 120 characters."); return; }
        if (c.length() > 60) { toast("City can be up to 60 characters."); return; }

        final Map<String, Object> changes = new HashMap<>();
        if (!nameLocked && !f.equals(s("first_name"))) changes.put("first_name", f);
        if (!nameLocked && !l.equals(s("last_name"))) changes.put("last_name", l);
        if (!j.equals(s("occupation"))) changes.put("occupation", j);
        if (!a.equals(s("address"))) changes.put("address", a);
        if (!c.equals(s("city"))) changes.put("city", c);
        if (newIcon != null) changes.put("icon", newIcon);
        if (changes.isEmpty()) { finish(); return; }

        save.setEnabled(false);
        save.setText("Saving…");
        UserProfileManager.getInstance(getApplicationContext()).updateProfileFields(uid, changes, new UserProfileManager.SimpleCallback() {
            @Override public void onSuccess() {
                Map<String, Object> merged = new HashMap<>(data);
                merged.putAll(changes);
                OfflineCacheManager.save(getApplicationContext(), "profile_" + uid, merged);
                PublicProfileStore.publishMine(getApplicationContext());
                runOnUiThread(() -> { toast("Profile saved."); finish(); });
            }
            @Override public void onError(String error) {
                runOnUiThread(() -> {
                    save.setText("Save changes");
                    save.setEnabled(true);
                    toast("Couldn't save: " + error);
                });
            }
        });
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
