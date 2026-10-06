package com.harvana.app.utils;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * One step for a group picture, used by Create group and Group settings: choose a picture and crop it to a square. It returns the
 * cropped picture as a local file and uploads nothing; the caller uploads it with {@link #upload} only when the person saves, so a
 * failed save never leaves a stray picture behind.
 */
public class GroupPhotoPickerActivity extends AppCompatActivity {
    private static final int RC_PICK = 701, RC_CROP = 702;

    /** Choose a picture and crop it square. The result is the cropped picture as the activity's data (a local file); nothing is uploaded. */
    public static Intent intent(Context c) {
        return new Intent(c, GroupPhotoPickerActivity.class);
    }

    private com.harvana.app.ui.ProgressSheet busy;

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
        // Nothing is uploaded here. The cropped picture stays on the phone and goes up when the caller saves (see upload()).
        setResult(RESULT_OK, new Intent().setData(data.getData()));
        finish();
    }

    /** Uploads a cropped group picture under the group's own name, so a new picture replaces the old one and only one is ever stored. */
    public static void upload(Context c, Uri cropped, String groupId, ImageHost.Callback cb) {
        ImageHost.upload(c, cropped, "group_photos/" + groupId, 512, "group_photos/" + groupId, cb);
    }

    @Override
    protected void onDestroy() {
        if (busy != null && busy.isShowing()) busy.dismiss();
        super.onDestroy();
    }
}
