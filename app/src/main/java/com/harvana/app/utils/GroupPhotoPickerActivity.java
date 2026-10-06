package com.harvana.app.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * One step for a group picture, used by Create group and Group settings: choose a picture, crop it to a square (the crop screen),
 * upload it through {@link ImageHost}, and hand back the https address. It shows nothing of its own except the system picker, the
 * crop screen and a short "Uploading" box, so a screen that wants a group picture only starts it and reads {@link #RESULT_URL}.
 * Nothing is written to the group here; the caller decides when to save the address.
 */
public class GroupPhotoPickerActivity extends AppCompatActivity {
    public static final String RESULT_URL = "url";
    private static final String X_KEY = "key";
    private static final int RC_PICK = 701, RC_CROP = 702;

    /** @param key a short folder name for the upload, such as the group id or your uid */
    public static Intent intent(Context c, String key) {
        return new Intent(c, GroupPhotoPickerActivity.class).putExtra(X_KEY, key == null ? "" : key);
    }

    private ProgressDialog busy;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (b != null) return;                       // rotating or returning: the flow already runs through results
        Intent pick = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
        try { startActivityForResult(Intent.createChooser(pick, "Choose a group picture"), RC_PICK); }
        catch (Exception e) { Toast.makeText(this, "No app to pick a picture.", Toast.LENGTH_LONG).show(); setResult(RESULT_CANCELED); finish(); }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != Activity.RESULT_OK || data == null || data.getData() == null) { setResult(RESULT_CANCELED); finish(); return; }
        if (req == RC_PICK) {
            // 1:1, shown with a round guide because groups appear as circles in lists.
            startActivityForResult(CropImageActivity.intent(this, data.getData(), 1f, 512, "Crop group picture", true), RC_CROP);
            return;
        }
        if (req != RC_CROP) return;
        busy = ProgressDialog.show(this, null, "Uploading…", true, false);
        String key = getIntent().getStringExtra(X_KEY);
        ImageHost.upload(this, data.getData(), "group_photos/" + (key == null || key.isEmpty() ? "new" : key), 512, new ImageHost.Callback() {
            @Override public void onSuccess(String url) {
                if (busy != null && busy.isShowing()) busy.dismiss();
                setResult(RESULT_OK, new Intent().putExtra(RESULT_URL, url));
                finish();
            }
            @Override public void onError(String message) {
                if (busy != null && busy.isShowing()) busy.dismiss();
                Toast.makeText(GroupPhotoPickerActivity.this, "Couldn't upload the picture: " + message, Toast.LENGTH_LONG).show();
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (busy != null && busy.isShowing()) busy.dismiss();
        super.onDestroy();
    }
}
