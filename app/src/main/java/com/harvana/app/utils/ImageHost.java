package com.harvana.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.harvana.app.R;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Uploads a picked photo and returns an https address for it. The photo is first shrunk on the phone (so a 4 MB camera picture
 * becomes a 40 to 250 KB file, which is what keeps any free plan going a long time), then sent to Cloudinary if it is set up in
 * res/values/image_host.xml, otherwise to Firebase Storage as before. Callers keep storing the returned address, so nothing that
 * shows a picture needs to know where it lives.
 */
public final class ImageHost {
    private ImageHost() {}

    public interface Callback {
        void onSuccess(String url);
        void onError(String message);
    }

    private static final Executor BG = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build();

    public static boolean cloudinaryConfigured(Context c) {
        return !c.getString(R.string.cloudinary_cloud_name).trim().isEmpty() && !c.getString(R.string.cloudinary_upload_preset).trim().isEmpty();
    }

    /**
     * @param folder a path such as "profile_icons/{uid}" (Cloudinary folder, or Firebase Storage path)
     * @param maxPx  longest side after shrinking: 512 for a profile picture, 1280 for a cover
     */
    public static void upload(Context context, final Uri uri, final String folder, final int maxPx, final Callback cb) {
        final Context app = context.getApplicationContext();
        BG.execute(() -> {
            try {
                final byte[] jpeg = shrink(app, uri, maxPx);
                if (cloudinaryConfigured(app)) {
                    String url = toCloudinary(app, jpeg, folder);
                    MAIN.post(() -> cb.onSuccess(url));
                } else {
                    toFirebase(jpeg, folder, cb);
                }
            } catch (Exception e) {
                final String m = e.getMessage() == null ? "Couldn't upload the picture." : e.getMessage();
                MAIN.post(() -> cb.onError(m));
            }
        });
    }

    // ── Shrinking ────────────────────────────────────────────────────────

    private static byte[] shrink(Context c, Uri uri, int maxPx) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = c.getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(in, null, bounds); }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new Exception("That file isn't a picture.");
        int sample = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inSampleSize = sample;
        Bitmap bmp;
        try (InputStream in = c.getContentResolver().openInputStream(uri)) { bmp = BitmapFactory.decodeStream(in, null, o); }
        if (bmp == null) throw new Exception("Couldn't read that picture.");

        // Cameras store the picture sideways and say so in the EXIF tag; turn it upright (the tag is read from API 24).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try (InputStream in = c.getContentResolver().openInputStream(uri)) {
                int orientation = new android.media.ExifInterface(in).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1);
                int degrees = orientation == 6 ? 90 : orientation == 3 ? 180 : orientation == 8 ? 270 : 0;
                if (degrees != 0) {
                    Matrix m = new Matrix();
                    m.postRotate(degrees);
                    bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
                }
            } catch (Exception ignored) { }
        }
        float scale = Math.min(1f, maxPx / (float) Math.max(bmp.getWidth(), bmp.getHeight()));
        if (scale < 1f) bmp = Bitmap.createScaledBitmap(bmp, Math.round(bmp.getWidth() * scale), Math.round(bmp.getHeight() * scale), true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
        return out.toByteArray();
    }

    // ── Destinations ─────────────────────────────────────────────────────

    private static String toCloudinary(Context c, byte[] jpeg, String folder) throws Exception {
        String cloud = c.getString(R.string.cloudinary_cloud_name).trim();
        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", c.getString(R.string.cloudinary_upload_preset).trim())
                .addFormDataPart("folder", folder)
                .addFormDataPart("file", "photo.jpg", RequestBody.create(jpeg, MediaType.parse("image/jpeg")))
                .build();
        Request req = new Request.Builder().url("https://api.cloudinary.com/v1_1/" + cloud + "/image/upload").post(body).build();
        try (Response r = HTTP.newCall(req).execute()) {
            String text = r.body() == null ? "" : r.body().string();
            if (!r.isSuccessful()) {
                String why = "";
                try { why = new JSONObject(text).getJSONObject("error").optString("message"); } catch (Exception ignored) { }
                throw new Exception("The picture host said no" + (why.isEmpty() ? " (" + r.code() + ")" : ": " + why));
            }
            String url = new JSONObject(text).optString("secure_url", "");
            if (!url.startsWith("https://")) throw new Exception("The picture host sent no address.");
            return url;
        }
    }

    private static void toFirebase(byte[] jpeg, String folder, final Callback cb) {
        final StorageReference ref = FirebaseStorage.getInstance().getReference().child(folder + "/" + System.currentTimeMillis() + ".jpg");
        ref.putBytes(jpeg)
                .addOnSuccessListener(t -> ref.getDownloadUrl()
                        .addOnSuccessListener(u -> cb.onSuccess(u.toString()))
                        .addOnFailureListener(e -> cb.onError(e.getMessage() == null ? "Couldn't upload the picture." : e.getMessage())))
                .addOnFailureListener(e -> cb.onError(e.getMessage() == null ? "Couldn't upload the picture." : e.getMessage()));
    }
}
