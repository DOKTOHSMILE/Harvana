package com.harvana.app.social;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/** Explains and asks for permission to read the phone's contacts, then reports the answer. Nothing is read here. */
public class ContactsAccessActivity extends Activity {
    public interface Result { void onResult(boolean granted); }
    private static Result pending;

    /** Runs {@code r.onResult(true)} straight away if contacts access is already allowed, otherwise asks first. */
    public static void ensure(Activity from, Result r) {
        if (ContextCompat.checkSelfPermission(from, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) { r.onResult(true); return; }
        pending = r;
        from.startActivity(new Intent(from, ContactsAccessActivity.class));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (b != null) return;
        new AlertDialog.Builder(this)
            .setTitle("Find friends in your contacts")
            .setMessage("Harvana can show which of your contacts already use the app.\n\n"
                + "Your contacts stay on your phone: numbers and emails are scrambled before they are compared, "
                + "and nothing from your address book is uploaded or stored. You only see people who chose to be found.")
            .setPositiveButton("Continue", (d, w) -> ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 21))
            .setNegativeButton("Not now", (d, w) -> done(false))
            .setOnCancelListener(d -> done(false))
            .show();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        done(results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED);
    }

    private void done(boolean granted) {
        Result r = pending;
        pending = null;
        finish();
        if (r != null) r.onResult(granted);
    }
}
