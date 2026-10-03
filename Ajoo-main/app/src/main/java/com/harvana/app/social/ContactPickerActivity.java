package com.harvana.app.social;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.Toast;

/**
 * Lets the person choose ONE entry from their phone contacts through the system contact picker. Because the system
 * picker returns only the chosen entry, the app never gets (or asks for) permission to read the whole address book.
 */
public class ContactPickerActivity extends Activity {
    public interface Result { void picked(String email, String name); }
    private static Result pending;

    /** Opens the picker; {@code r} receives the chosen contact's email address. */
    public static void pickEmail(Activity from, Result r) {
        pending = r;
        from.startActivity(new Intent(from, ContactPickerActivity.class));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (b == null) {
            try { startActivityForResult(new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Email.CONTENT_URI), 7); }
            catch (Exception e) { Toast.makeText(this, "No contacts app available.", Toast.LENGTH_SHORT).show(); finish(); }
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        Result r = pending;
        pending = null;
        if (res == RESULT_OK && data != null && data.getData() != null && r != null) {
            Uri uri = data.getData();
            try (Cursor c = getContentResolver().query(uri,
                new String[]{ContactsContract.CommonDataKinds.Email.ADDRESS, ContactsContract.Contacts.DISPLAY_NAME}, null, null, null)) {
                if (c != null && c.moveToFirst()) r.picked(c.getString(0), c.getString(1));
            } catch (Exception e) {
                Toast.makeText(this, "Couldn't read that contact.", Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }
}
