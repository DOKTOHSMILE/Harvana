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
import com.google.firebase.storage.StorageMetadata;
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
        upload(context, uri, folder, maxPx, null, cb);
    }

    /**
     * Same, but with a fixed name for the picture. Uploading again under the same name replaces the earlier file, so a person or a group
     * never has more than one picture stored: pass {@code "profile_icons/" + uid}, {@code "group_photos/" + groupId} and so on.
     * (Cloudinary replaces only if the upload preset has Overwrite switched on. If it does not, the picture still uploads, under a new name.)
     */
    public static void upload(Context context, final Uri uri, final String folder, final int maxPx, final String publicId, final Callback cb) {
        final Context app = context.getApplicationContext();
        BG.execute(() -> {
            try {
                final byte[] jpeg = shrink(app, uri, maxPx);
                if (cloudinaryConfigured(app)) {
                    String url = toCloudinary(app, jpeg, publicId);
                    MAIN.post(() -> cb.onSuccess(url));
                } else {
                    toFirebase(jpeg, folder, publicId != null, cb);
                }
            } catch (Exception e) {
                final String m = e.getMessage() == null ? "Couldn't upload the picture." : e.getMessage();
                MAIN.post(() -> cb.onError(m));
            }
        });
    }

    /**
     * A PDF for a trade chat. Stored in Firebase Storage under {@code folder}. Pictures still go through {@link #upload}.
     * The bucket must allow a signed-in user to write {@code p2p_chat/}.
     */
    public static void uploadPdf(Context context, final Uri uri, final String folder, final Callback cb) {
        final Context app = context.getApplicationContext();
        BG.execute(() -> {
            try {
                byte[] data;
                try (InputStream in = app.getContentResolver().openInputStream(uri)) {
                    if (in == null) throw new Exception("Couldn't read that file.");
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n, total = 0;
                    while ((n = in.read(buf)) >= 0) {
                        total += n;
                        if (total > 8 * 1024 * 1024) throw new Exception("That PDF is larger than 8 MB.");
                        out.write(buf, 0, n);
                    }
                    data = out.toByteArray();
                }
                if (data.length < 5 || data[0] != '%') throw new Exception("That file is not a PDF.");
                StorageReference ref = FirebaseStorage.getInstance().getReference().child(folder + "/" + System.currentTimeMillis() + ".pdf");
                StorageMetadata meta = new StorageMetadata.Builder().setContentType("application/pdf").build();
                ref.putBytes(data, meta)
                        .addOnSuccessListener(t -> ref.getDownloadUrl()
                                .addOnSuccessListener(u -> cb.onSuccess(u.toString()))
                                .addOnFailureListener(e -> cb.onError(com.harvana.app.utils.ErrorText.of(e))))
                        .addOnFailureListener(e -> cb.onError(com.harvana.app.utils.ErrorText.of(e)));
            } catch (Exception e) {
                final String m = e.getMessage() == null ? "Couldn't upload the PDF." : e.getMessage();
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

    private static String toCloudinary(Context c, byte[] jpeg, String publicId) throws Exception {
        String url = sendToCloudinary(c, jpeg, publicId);
        if (url == null) url = sendToCloudinary(c, jpeg, null);     // the host kept an older file under that name: store this one under a fresh name
        return url;
    }

    /** Returns the picture's address, or null when a fixed name was asked for but the host would not replace the earlier file. */
    private static String sendToCloudinary(Context c, byte[] jpeg, String publicId) throws Exception {
        String cloud = c.getString(R.string.cloudinary_cloud_name).trim();
        MultipartBody.Builder mb = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", c.getString(R.string.cloudinary_upload_preset).trim());
        // No "folder" field: an unsigned upload may only send a few extra fields, and the account may use dynamic folders.
        // The folder is set once in the preset (Asset folder) instead. public_id is one of the fields an unsigned upload may send.
        if (publicId != null && !publicId.isEmpty()) mb.addFormDataPart("public_id", publicId);
        RequestBody body = mb.addFormDataPart("file", "photo.jpg", RequestBody.create(jpeg, MediaType.parse("image/jpeg"))).build();
        Request req = new Request.Builder().url("https://api.cloudinary.com/v1_1/" + cloud + "/image/upload").post(body).build();
        try (Response r = HTTP.newCall(req).execute()) {
            String text = r.body() == null ? "" : r.body().string();
            if (!r.isSuccessful()) {
                String why = "";
                try { why = new JSONObject(text).getJSONObject("error").optString("message"); } catch (Exception ignored) { }
                if (publicId != null && why.toLowerCase(java.util.Locale.US).contains("already exists")) return null;
                throw new Exception("The picture host said no" + (why.isEmpty() ? " (" + r.code() + ")" : ": " + why));
            }
            JSONObject json = new JSONObject(text);
            if (publicId != null && json.optBoolean("existing", false)) return null;     // it returned the old file without replacing it
            String url = json.optString("secure_url", "");
            if (!url.startsWith("https://")) throw new Exception("The picture host sent no address.");
            return url;
        }
    }

    private static void toFirebase(byte[] jpeg, String folder, boolean fixedName, final Callback cb) {
        final StorageReference ref = FirebaseStorage.getInstance().getReference().child(folder + "/" + (fixedName ? "current" : String.valueOf(System.currentTimeMillis())) + ".jpg");
        ref.putBytes(jpeg)
                .addOnSuccessListener(t -> ref.getDownloadUrl()
                        // A replaced file keeps its address, so a version is added to make every screen load the new picture.
                        .addOnSuccessListener(u -> cb.onSuccess(fixedName ? u.toString() + (u.toString().contains("?") ? "&" : "?") + "v=" + System.currentTimeMillis() : u.toString()))
                        .addOnFailureListener(e -> cb.onError(com.harvana.app.utils.ErrorText.of(e))))
                .addOnFailureListener(e -> cb.onError(com.harvana.app.utils.ErrorText.of(e)));
    }
}
