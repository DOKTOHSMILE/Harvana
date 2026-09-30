package com.harvanna.app.social;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;

/** "Who in my contacts is on Harvanna?": permission, read, scramble, ask. Results come back on the main thread. */
public final class ContactsFinder {
    private ContactsFinder() {}

    public interface Cb { void onMatches(List<DiscoveryStore.Match> matches); }

    public static void find(Activity a, Cb cb) {
        ContactsAccessActivity.ensure(a, granted -> a.runOnUiThread(() -> {
            if (!granted) { Toast.makeText(a, "Contacts access is off. You can still find friends by username.", Toast.LENGTH_LONG).show(); return; }
            SocialStore.myCountryCode((cc, err) -> a.runOnUiThread(() -> withCountryCode(a, cc, code -> scan(a, code, cb))));
        }));
    }

    private interface Code { void use(String cc); }

    private static void withCountryCode(Activity a, String detected, Code next) {
        SharedPreferences sp = a.getSharedPreferences("contact_discovery", Activity.MODE_PRIVATE);
        String cc = detected != null && !detected.isEmpty() ? detected : sp.getString("cc", "");
        if (!cc.isEmpty()) { next.use(cc); return; }
        final EditText et = new EditText(a);
        et.setHint("e.g. 234");
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(a).setTitle("Your country code")
            .setMessage("Needed to read local numbers like 0801… correctly. Nigeria is 234, Ghana 233, Kenya 254, UK 44.")
            .setView(et)
            .setPositiveButton("Continue", (d, w) -> {
                String v = et.getText().toString().replaceAll("[^0-9]", "");
                if (v.isEmpty() || v.length() > 3) { Toast.makeText(a, "Enter 1 to 3 digits.", Toast.LENGTH_LONG).show(); return; }
                sp.edit().putString("cc", v).apply();
                next.use(v);
            }).setNegativeButton("Cancel", null).show();
    }

    private static void scan(Activity a, String cc, Cb cb) {
        Toast.makeText(a, "Checking your contacts…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            List<PhoneContacts.Entry> entries = PhoneContacts.read(a);
            DiscoveryStore.lookup(entries, cc, (matches, err) -> a.runOnUiThread(() -> {
                if (err != null) Toast.makeText(a, err, Toast.LENGTH_LONG).show();
                else cb.onMatches(matches);
            }));
        }).start();
    }
}
