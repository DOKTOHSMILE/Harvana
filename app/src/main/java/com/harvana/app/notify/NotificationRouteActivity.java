package com.harvana.app.notify;

import android.app.Activity;
import android.os.Bundle;

/**
 * Invisible stop for a tap on a notification in the phone's shade. It finds the notification in the inbox, opens the page it is
 * about (see {@link NotificationRouter}) and closes itself, so the user lands on that page and not on the inbox.
 */
public class NotificationRouteActivity extends Activity {
    public static final String EXTRA_ID = "notification_id";

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        String id = getIntent() == null ? null : getIntent().getStringExtra(EXTRA_ID);
        NotificationCenter.Item found = null;
        if (id != null) for (NotificationCenter.Item it : NotificationCenter.list(this)) if (id.equals(it.id)) { found = it; break; }
        if (found != null) NotificationRouter.open(this, found);
        else startActivity(new android.content.Intent(this, com.harvana.app.NotificationsActivity.class));
        finish();
    }
}
