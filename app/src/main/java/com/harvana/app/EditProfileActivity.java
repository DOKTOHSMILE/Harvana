package com.harvana.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
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

    private String uid = "";
    private Map<String, Object> data = new HashMap<>();
    private boolean nameLocked;
    private EditText first, last, job, address, city;
    private MaterialButton save;
    private ImageView photo;
    private TextView initials, photoStatus;
    private ProgressBar photoBusy;
    private String newIcon;      // an address to save as the photo (set only by a change that needs no upload)
    private Uri pendingIcon, pendingCover;     // cropped pictures waiting on the phone: they upload only when Save changes is pressed
    private static final int RC_COVER = 613, RC_CROP_PHOTO = 614, RC_CROP_COVER = 615;
    /** The cover is shown 5 wide to 2 high on every profile, so that is the shape the person crops to. */
    static final float COVER_ASPECT = 2.5f;
    private String newCover;     // set once a picked cover has been uploaded; "" means remove it
    private ImageView coverView;
    private TextView coverStatus;
    private ProgressBar coverBusy;

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        com.google.firebase.auth.FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        if (me == null) { finish(); return; }
        uid = me.getUid();
        AppBar.setContent(this, R.layout.activity_edit_profile, "Edit profile", "Your photo and details");
        ((LinearLayout) findViewById(R.id.edit_profile_loading)).addView(Shimmer.cards(this, 3));

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
                    findViewById(R.id.edit_profile_loading).setVisibility(View.GONE);
                    TextView err = findViewById(R.id.tv_edit_profile_error);
                    err.setText("Couldn't load your details: " + error);
                    err.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private String s(String k) {
        Object v = data.get(k);
        return v == null || "null".equals(String.valueOf(v)) ? "" : String.valueOf(v).trim();
    }

    // ── Page ─────────────────────────────────────────────────────────────

    /** Fills res/layout/activity_edit_profile.xml from the profile. */
    private void build() {
        findViewById(R.id.edit_profile_loading).setVisibility(View.GONE);
        findViewById(R.id.edit_profile_content).setVisibility(View.VISIBLE);
        nameLocked = !s("v_type").isEmpty();
        newIcon = null;
        newCover = null;
        coverBlock();
        photoBlock();

        first = field(R.id.et_profile_first, s("first_name"), nameLocked);
        last = field(R.id.et_profile_last, s("last_name"), nameLocked);
        findViewById(R.id.tv_profile_name_locked).setVisibility(nameLocked ? View.VISIBLE : View.GONE);
        job = field(R.id.et_profile_job, s("occupation"), false);
        address = field(R.id.et_profile_address, s("address"), false);
        city = field(R.id.et_profile_city, s("city"), false);

        readOnly(R.id.tv_profile_country, s("country"));
        readOnly(R.id.tv_profile_state, s("state"));
        readOnly(R.id.tv_profile_email, s("email"));
        readOnly(R.id.tv_profile_dob, s("dob"));
        readOnly(R.id.tv_profile_pid, s("pid"));

        findViewById(R.id.row_profile_contact_card).setOnClickListener(v -> startActivity(new Intent(this, ContactCardActivity.class)));
        findViewById(R.id.row_profile_privacy).setOnClickListener(v -> startActivity(new Intent(this, ProfilePrivacyActivity.class)));

        save = findViewById(R.id.btn_profile_save);
        save.setText("Save changes");
        save.setEnabled(false);
        save.setOnClickListener(v -> save());

        TextWatcher w = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence a, int b, int c, int d) { }
            @Override public void onTextChanged(CharSequence a, int b, int c, int d) { }
            @Override public void afterTextChanged(Editable e) { save.setEnabled(dirty()); }
        };
        for (EditText e : new EditText[] {first, last, job, address, city}) { e.removeTextChangedListener(w); e.addTextChangedListener(w); }
    }

    private void coverBlock() {
        View frame = findViewById(R.id.profile_cover_frame);
        int previewWidth = getResources().getDisplayMetrics().widthPixels - 2 * SocialUi.dp(this, 16);
        ViewGroup.LayoutParams lp = frame.getLayoutParams();
        lp.height = Math.round(previewWidth / COVER_ASPECT);                       // same shape as on the profile
        frame.setLayoutParams(lp);
        coverView = findViewById(R.id.iv_profile_cover);
        coverBusy = findViewById(R.id.profile_cover_busy);
        coverStatus = findViewById(R.id.tv_profile_cover_status);
        showCover(s("cover"));
        findViewById(R.id.btn_profile_cover_change).setOnClickListener(v ->
                startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), "Choose a cover"), RC_COVER));
        View remove = findViewById(R.id.btn_profile_cover_remove);
        remove.setVisibility(s("cover").isEmpty() ? View.GONE : View.VISIBLE);
        remove.setOnClickListener(v -> {
            newCover = ""; pendingCover = null;
            showCover("");
            coverStatus.setText("Cover will be removed when you press Save changes.");
            if (save != null) save.setEnabled(true);
        });
    }

    private void showCover(String url) {
        if (url != null && url.startsWith("http")) Glide.with(this).load(url).centerCrop().into(coverView);
        else coverView.setImageDrawable(null);
    }

    private void photoBlock() {
        initials = findViewById(R.id.tv_profile_initials);
        initials.setText(initialsOf());
        photo = findViewById(R.id.iv_profile_photo);
        photoBusy = findViewById(R.id.profile_photo_busy);
        photoStatus = findViewById(R.id.tv_profile_photo_status);
        showPhoto(s("icon"));
        findViewById(R.id.btn_profile_photo_change).setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
            startActivityForResult(Intent.createChooser(pick, "Choose a photo"), RC_PHOTO);
        });
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
        if (res != RESULT_OK || result == null || result.getData() == null) return;
        // A picked picture is cropped by hand first (square for the profile picture, wide for the cover); the crop is what gets uploaded.
        if (req == RC_COVER) { startActivityForResult(com.harvana.app.utils.CropImageActivity.intent(this, result.getData(), COVER_ASPECT, 1280, "Crop cover", false), RC_CROP_COVER); return; }
        if (req == RC_PHOTO) { startActivityForResult(com.harvana.app.utils.CropImageActivity.intent(this, result.getData(), 1f, 512, "Crop profile photo", true), RC_CROP_PHOTO); return; }
        if (req == RC_CROP_COVER) {
            // Nothing is uploaded yet. The crop waits on the phone and goes up when Save changes is pressed, so a failed save leaves nothing behind.
            pendingCover = result.getData();
            newCover = null;
            Glide.with(this).load(pendingCover).centerCrop().into(coverView);
            coverStatus.setText("New cover chosen. It uploads when you press Save changes.");
            if (save != null) save.setEnabled(true);
            return;
        }
        if (req != RC_CROP_PHOTO) return;
        pendingIcon = result.getData();
        newIcon = null;
        Glide.with(this).load(pendingIcon).circleCrop().into(photo);
        initials.setVisibility(View.INVISIBLE);
        photoStatus.setText("New photo chosen. It uploads when you press Save changes.");
        if (save != null) save.setEnabled(true);
    }

    /**
     * Uploads the waiting pictures, one after the other, and only then calls {@code next}. A picture is stored under a fixed name per person
     * ("profile_icons/uid"), so each new one replaces the last and only one photo is ever kept. If an upload fails nothing else is saved.
     */
    private void uploadPending(final Map<String, Object> changes, final Runnable next) {
        if (pendingIcon != null) {
            photoBusy.setVisibility(View.VISIBLE);
            photoStatus.setText("Uploading photo…");
            com.harvana.app.utils.ImageHost.upload(this, pendingIcon, "profile_icons/" + uid, 512, "profile_icons/" + uid, new com.harvana.app.utils.ImageHost.Callback() {
                @Override public void onSuccess(String url) {
                    photoBusy.setVisibility(View.GONE);
                    photoStatus.setText("");
                    changes.put("icon", url);
                    pendingIcon = null;
                    uploadPending(changes, next);
                }
                @Override public void onError(String message) { uploadFailed("photo", message, photoBusy, photoStatus); }
            });
            return;
        }
        if (pendingCover != null) {
            coverBusy.setVisibility(View.VISIBLE);
            coverStatus.setText("Uploading cover…");
            com.harvana.app.utils.ImageHost.upload(this, pendingCover, "profile_covers/" + uid, 1280, "profile_covers/" + uid, new com.harvana.app.utils.ImageHost.Callback() {
                @Override public void onSuccess(String url) {
                    coverBusy.setVisibility(View.GONE);
                    coverStatus.setText("");
                    changes.put("cover", url);
                    pendingCover = null;
                    uploadPending(changes, next);
                }
                @Override public void onError(String message) { uploadFailed("cover", message, coverBusy, coverStatus); }
            });
            return;
        }
        next.run();
    }

    private void uploadFailed(String what, String message, ProgressBar busy, TextView status) {
        busy.setVisibility(View.GONE);
        status.setText("Couldn't upload the " + what + ": " + message + " Nothing was saved. Press Save changes to try again.");
        save.setText("Save changes");
        save.setEnabled(true);
    }

    // ── Fields ───────────────────────────────────────────────────────────

    private EditText field(int id, String value, boolean locked) {
        EditText e = findViewById(id);
        e.setText(value);
        e.setEnabled(!locked);
        e.setAlpha(locked ? 0.6f : 1f);
        return e;
    }

    private void readOnly(int id, String value) {
        TextView v = findViewById(id);
        v.setText(value.isEmpty() ? "—" : value);
        v.setTextColor(value.isEmpty() ? UiColors.textTertiary() : UiColors.textPrimary());
    }

    // ── Saving ───────────────────────────────────────────────────────────

    private boolean dirty() {
        if (first == null) return false;
        return newIcon != null || newCover != null || pendingIcon != null || pendingCover != null
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
        if (newCover != null) changes.put("cover", newCover);
        if (changes.isEmpty() && pendingIcon == null && pendingCover == null) { finish(); return; }

        save.setEnabled(false);
        save.setText("Saving…");
        // Pictures upload first; the rest is saved only if they succeed.
        uploadPending(changes, () -> commit(changes));
    }

    private void commit(final Map<String, Object> changes) {
        UserProfileManager.getInstance(getApplicationContext()).updateProfileFields(uid, changes, new UserProfileManager.SimpleCallback() {
            @Override public void onSuccess() {
                Map<String, Object> merged = new HashMap<>(data);
                merged.putAll(changes);
                OfflineCacheManager.save(getApplicationContext(), "profile_" + uid, merged);
                com.harvana.app.trust.ProfileTrust.forget(uid);          // every post and row reads the new photo and name now
                PublicProfileStore.publishMine(getApplicationContext());
                // A cover that was removed is deleted from the picture host too, so nothing is left stored.
                if ("".equals(changes.get("cover"))) com.harvana.app.utils.MediaCleaner.delete(getApplicationContext(), com.harvana.app.utils.MediaCleaner.PROFILE_COVER, null);
                if (changes.get("icon") instanceof String) {            // the sign-in profile photo, which the Home header reads
                    com.google.firebase.auth.FirebaseUser au = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                    if (au != null) au.updateProfile(new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setPhotoUri(android.net.Uri.parse((String) changes.get("icon"))).build());
                }
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
