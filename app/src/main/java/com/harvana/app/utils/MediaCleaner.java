package com.harvana.app.utils;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.harvana.app.sync.HarvanaNodeClient;

import org.json.JSONObject;

/**
 * Deletes a stored picture for good. Deleting needs the picture host's secret key, which cannot live in the app, so the node does it:
 * the app sends its Firebase sign-in proof and the KIND of picture, never a file name, and the node works out which file is the
 * caller's. Everything is best effort: a picture that cannot be deleted right now (offline, or the node has no host key set yet) is
 * simply left, and the app carries on. {@code done} runs in every case, so callers can continue with what they were doing.
 */
public final class MediaCleaner {
    private MediaCleaner() {}

    public static final String PROFILE_PHOTO = "PROFILE_PHOTO", PROFILE_COVER = "PROFILE_COVER", GROUP_PHOTO = "GROUP_PHOTO";

    public static void delete(Context context, String kind, String groupId, Runnable done) {
        final Runnable end = done == null ? () -> { } : done;
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) { end.run(); return; }
            user.getIdToken(false).addOnCompleteListener(task -> {
                try {
                    if (!task.isSuccessful() || task.getResult() == null || task.getResult().getToken() == null) { end.run(); return; }
                    JSONObject body = new JSONObject().put("idToken", task.getResult().getToken()).put("kind", kind);
                    if (groupId != null && !groupId.isEmpty()) body.put("groupId", groupId);
                    HarvanaNodeClient.nodePost("/api/v1/media/delete", body.toString(), new HarvanaNodeClient.JsonCallback() {
                        @Override public void onSuccess(JSONObject json) { end.run(); }
                        @Override public void onError(String error) { end.run(); }
                    });
                } catch (Exception e) { end.run(); }
            });
        } catch (Exception e) { end.run(); }
    }

    public static void delete(Context context, String kind, String groupId) { delete(context, kind, groupId, null); }
}
