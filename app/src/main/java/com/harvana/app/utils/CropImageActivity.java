package com.harvana.app.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Lets the person choose exactly which part of a picked photo to keep: drag and pinch under a frame of the right shape, then Use photo.
 * The result is a new upright JPEG in the app's cache, returned as the activity's data; nothing is uploaded here.
 */
public class CropImageActivity extends AppCompatActivity {
    private static final String X_ASPECT = "aspect", X_MAX = "maxWidth", X_TITLE = "title", X_CIRCLE = "circle";
    private static final int LOAD_MAX_PX = 2400;

    public static Intent intent(Context c, Uri photo, float aspect, int maxWidthPx, String title, boolean circleGuide) {
        return new Intent(c, CropImageActivity.class).setData(photo).putExtra(X_ASPECT, aspect).putExtra(X_MAX, maxWidthPx)
                .putExtra(X_TITLE, title).putExtra(X_CIRCLE, circleGuide);
    }

    private CropView crop;
    private ProgressBar busy;
    private MaterialButton use;
    private float aspect;
    private int maxWidth;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Uri photo = getIntent().getData();
        aspect = getIntent().getFloatExtra(X_ASPECT, 1f);
        maxWidth = getIntent().getIntExtra(X_MAX, 1024);
        String title = getIntent().getStringExtra(X_TITLE);
        boolean circle = getIntent().getBooleanExtra(X_CIRCLE, false);
        if (photo == null) { finish(); return; }

        setContentView(com.harvana.app.R.layout.activity_crop_image);
        crop = findViewById(com.harvana.app.R.id.crop_view);
        busy = findViewById(com.harvana.app.R.id.crop_busy);
        use = findViewById(com.harvana.app.R.id.crop_use);
        findViewById(com.harvana.app.R.id.crop_cancel).setOnClickListener(v -> { setResult(RESULT_CANCELED); finish(); });
        if (title != null) ((TextView) findViewById(com.harvana.app.R.id.crop_title)).setText(title);
        ((TextView) findViewById(com.harvana.app.R.id.crop_hint)).setText(aspect <= 0f
                ? "Drag the corners or edges to choose the area. Drag inside to move it." : "Drag to move. Pinch to zoom.");
        use.setOnClickListener(v -> finishCrop());
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        final boolean round = circle;
        new Thread(() -> {
            final Bitmap bmp = load(photo);
            runOnUiThread(() -> {
                busy.setVisibility(View.GONE);
                if (bmp == null) { Toast.makeText(this, "Couldn't open that picture.", Toast.LENGTH_LONG).show(); setResult(RESULT_CANCELED); finish(); return; }
                crop.setup(bmp, aspect, round);
                use.setEnabled(true);
            });
        }).start();
    }

    /** Decodes the picture at a size the phone can hold, turned upright using its EXIF tag. */
    private Bitmap load(Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(in, null, bounds); }
            if (bounds.outWidth <= 0) return null;
            int sample = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= LOAD_MAX_PX) sample *= 2;
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = sample;
            Bitmap bmp;
            try (InputStream in = getContentResolver().openInputStream(uri)) { bmp = BitmapFactory.decodeStream(in, null, o); }
            if (bmp == null) return null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    int orientation = new android.media.ExifInterface(in).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1);
                    int deg = orientation == 6 ? 90 : orientation == 3 ? 180 : orientation == 8 ? 270 : 0;
                    if (deg != 0) {
                        Matrix m = new Matrix();
                        m.postRotate(deg);
                        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
                    }
                } catch (Exception ignored) { }
            }
            return bmp;
        } catch (Throwable t) {
            return null;
        }
    }

    private void finishCrop() {
        use.setEnabled(false);
        busy.setVisibility(View.VISIBLE);
        new Thread(() -> {
            Uri result = null;
            try {
                Bitmap out = crop.crop(maxWidth);
                if (out != null) {
                    File f = new File(getCacheDir(), "crop_" + System.currentTimeMillis() + ".jpg");
                    try (FileOutputStream fos = new FileOutputStream(f)) { out.compress(Bitmap.CompressFormat.JPEG, 90, fos); }
                    result = Uri.fromFile(f);
                }
            } catch (Throwable ignored) { }
            final Uri done = result;
            runOnUiThread(() -> {
                if (done == null) {
                    busy.setVisibility(View.GONE);
                    use.setEnabled(true);
                    Toast.makeText(this, "Couldn't crop that picture. Try again.", Toast.LENGTH_LONG).show();
                    return;
                }
                setResult(RESULT_OK, new Intent().setData(done));
                finish();
            });
        }).start();
    }
}
