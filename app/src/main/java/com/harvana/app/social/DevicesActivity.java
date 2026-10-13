package com.harvana.app.social;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.harvana.app.auth.SessionGuard;
import com.harvana.app.utils.AppBar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/** The phones this account has been signed in on: this one on top, then the others with when they were signed out. */
public class DevicesActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        AppBar.setContent(this, com.harvana.app.R.layout.activity_devices, "Devices", "Phones signed in to your account");
        load();
    }

    private void load() {
        ((TextView) findViewById(com.harvana.app.R.id.tv_device_this_name)).setText(SessionGuard.deviceName());
        final LinearLayout others = findViewById(com.harvana.app.R.id.devices_others);
        final TextView status = findViewById(com.harvana.app.R.id.tv_devices_status);
        others.removeAllViews();
        status.setText("Loading…");
        status.setVisibility(View.VISIBLE);

        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return;
        FirebaseFirestore.getInstance().collection("USERS").document(u.getUid()).collection("private").document("devices").get()
            .addOnSuccessListener(d -> {
                others.removeAllViews();
                java.util.List<Map<String, Object>> list = new ArrayList<>();
                if (d.get("devices") instanceof Map) for (Map.Entry<?, ?> e : ((Map<?, ?>) d.get("devices")).entrySet()) {
                    if (String.valueOf(e.getKey()).equals(SessionGuard.deviceId()) || !(e.getValue() instanceof Map)) continue;
                    Map<String, Object> m = new java.util.HashMap<>();
                    for (Map.Entry<?, ?> f : ((Map<?, ?>) e.getValue()).entrySet()) m.put(String.valueOf(f.getKey()), f.getValue());
                    list.add(m);
                }
                Collections.sort(list, (x, y) -> Long.compare(num(y.get("lastAt")), num(x.get("lastAt"))));
                if (list.isEmpty()) { status.setText("None"); return; }
                status.setVisibility(View.GONE);
                for (Map<String, Object> m : list) {
                    View row = getLayoutInflater().inflate(com.harvana.app.R.layout.item_device, others, false);
                    ((TextView) row.findViewById(com.harvana.app.R.id.tv_device_name)).setText(m.get("name") == null ? "Phone" : String.valueOf(m.get("name")));
                    String state = m.get("signedOutAt") != null
                            ? "Signed out " + when(num(m.get("signedOutAt")))
                            : "Last seen " + when(num(m.get("lastAt")));
                    ((TextView) row.findViewById(com.harvana.app.R.id.tv_device_state)).setText(
                            state + (m.get("signedInAt") != null ? "  ·  signed in " + when(num(m.get("signedInAt"))) : ""));
                    others.addView(row);
                }
            })
            .addOnFailureListener(e -> status.setText("Couldn't load the list. Check your connection."));
    }

    private static long num(Object o) { return o instanceof Number ? ((Number) o).longValue() : 0L; }

    private static String when(long t) {
        return t <= 0 ? "unknown" : new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.US).format(new Date(t));
    }
}
