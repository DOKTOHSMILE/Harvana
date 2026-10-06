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
        newCover = null;
        page.addView(coverBlock());
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

    private View coverBlock() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 0, 0, SocialUi.dp(this, 14));
        FrameLayout frame = new FrameLayout(this);
        GradientDrawable fallback = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[] {(UiColors.accent() & 0x00FFFFFF) | 0x66000000, UiColors.surface()});
        frame.setBackground(fallback);
        coverView = new ImageView(this);
        coverView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(coverView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        coverBusy = new ProgressBar(this);
        coverBusy.setVisibility(View.GONE);
        frame.addView(coverBusy, new FrameLayout.LayoutParams(SocialUi.dp(this, 36), SocialUi.dp(this, 36), Gravity.CENTER));
        showCover(s("cover"));
        int previewWidth = getResources().getDisplayMetrics().widthPixels - 2 * SocialUi.dp(this, 16);
        box.addView(frame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(previewWidth / COVER_ASPECT)));   // same shape as on the profile

        LinearLayout buttons = new LinearLayout(this);
        buttons.setPadding(0, SocialUi.dp(this, 10), 0, 0);
        MaterialButton change = SocialUi.compactButton(this, "Change cover", true);
        change.setOnClickListener(v -> startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), "Choose a cover"), RC_COVER));
        buttons.addView(change);
        if (!s("cover").isEmpty()) {
            MaterialButton remove = SocialUi.compactButton(this, "Remove", true);
            LinearLayout.LayoutParams rp = (LinearLayout.LayoutParams) remove.getLayoutParams();
            rp.setMarginStart(SocialUi.dp(this, 8));
            remove.setOnClickListener(v -> {
                newCover = ""; pendingCover = null;
                showCover("");
                coverStatus.setText("Cover will be removed when you press Save changes.");
                if (save != null) save.setEnabled(true);
            });
            buttons.addView(remove, rp);
        }
        box.addView(buttons);
        coverStatus = SocialUi.text(this, "", 12, false, UiColors.textSecondary());
        coverStatus.setPadding(0, SocialUi.dp(this, 6), 0, 0);
        box.addView(coverStatus);
        return box;
    }

    private void showCover(String url) {
        if (url != null && url.startsWith("http")) Glide.with(this).load(url).centerCrop().into(coverView);
        else coverView.setImageDrawable(null);
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
