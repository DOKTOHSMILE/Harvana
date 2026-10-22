package com.harvana.app.share;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Every share button in the app goes through here: text, a picture, or a file.
 * Change the chooser or the receipt wording in this class only.
 */
public final class ShareCenter {
    private ShareCenter() {}

    public static void text(Context context, String chooserTitle, String body) {
        if (context == null || body == null) return;
        Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, body);
        launch(context, Intent.createChooser(send, chooserTitle == null ? "Share" : chooserTitle));
    }

    public static void file(Activity activity, Uri uri, String mime, String subject, String text, String chooserTitle) {
        if (activity == null || uri == null) return;
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType(mime == null || mime.isEmpty() ? "*/*" : mime);
        send.putExtra(Intent.EXTRA_STREAM, uri);
        if (subject != null) send.putExtra(Intent.EXTRA_SUBJECT, subject);
        if (text != null) send.putExtra(Intent.EXTRA_TEXT, text);
        send.setClipData(ClipData.newRawUri("share", uri));
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        launch(activity, Intent.createChooser(send, chooserTitle == null ? "Share" : chooserTitle));
    }

    private static void launch(Context context, Intent chooser) {
        try {
            if (!(context instanceof Activity)) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
        } catch (Exception ignored) { }
    }
}
