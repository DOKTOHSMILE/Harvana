package com.harvana.app.social;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.harvana.app.auth.SessionGuard;
import com.harvana.app.utils.AppBar;
import com.harvana.app.utils.UiColors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/** The phones this account has been signed in on: this one on top, then the others with when they were signed out. */
public class DevicesActivity extends AppCompatActivity {
    private LinearLayout root;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(UiColors.background());
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = SocialUi.dp(this, 14);
        root.setPadding(p, SocialUi.dp(this, 10), p, p);
        sv.addView(root);
        AppBar.setContent(this, sv, "Devices", "Phones signed in to your account");
        load();
    }

    private void load() {
        root.removeAllViews();
        LinearLayout me = SocialUi.card(this);
        me.addView(SocialUi.text(this, "This phone", 13, true, UiColors.accent()));
        me.addView(SocialUi.text(this, SessionGuard.deviceName(), 16, true, UiColors.textPrimary()));
        me.addView(SocialUi.text(this, "Signed in now", 12, false, UiColors.textSecondary()));
        root.addView(me);
        final LinearLayout others = SocialUi.card(this);
        others.addView(SocialUi.text(this, "Other phones", 13, true, UiColors.accent()));
        others.addView(SocialUi.text(this, "Loading…", 13, false, UiColors.textSecondary()));
        root.addView(others);
        TextView note = SocialUi.text(this, "An account can be signed in on one phone at a time. Signing in on a new phone signs the old one out; "
                + "your wallet stays on the phone where you created or imported it.", 12, false, UiColors.textTertiary());
        note.setPadding(SocialUi.dp(this, 4), SocialUi.dp(this, 10), SocialUi.dp(this, 4), 0);
        root.addView(note);

        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return;
        FirebaseFirestore.getInstance().collection("USERS").document(u.getUid()).collection("private").document("devices").get()
            .addOnSuccessListener(d -> {
                others.removeAllViews();
                others.addView(SocialUi.text(this, "Other phones", 13, true, UiColors.accent()));
                java.util.List<Map<String, Object>> list = new ArrayList<>();
                if (d.get("devices") instanceof Map) for (Map.Entry<?, ?> e : ((Map<?, ?>) d.get("devices")).entrySet()) {
                    if (String.valueOf(e.getKey()).equals(SessionGuard.deviceId()) || !(e.getValue() instanceof Map)) continue;
                    Map<String, Object> m = new java.util.HashMap<>();
                    for (Map.Entry<?, ?> f : ((Map<?, ?>) e.getValue()).entrySet()) m.put(String.valueOf(f.getKey()), f.getValue());
                    list.add(m);
                }
                Collections.sort(list, (x, y) -> Long.compare(num(y.get("lastAt")), num(x.get("lastAt"))));
                if (list.isEmpty()) {
                    others.addView(SocialUi.text(this, "No other phone has signed in to this account.", 13, false, UiColors.textSecondary()));
                    return;
                }
                for (Map<String, Object> m : list) {
                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.VERTICAL);
                    row.setPadding(0, SocialUi.dp(this, 12), 0, 0);
                    row.addView(SocialUi.text(this, m.get("name") == null ? "Phone" : String.valueOf(m.get("name")), 15, true, UiColors.textPrimary()));
                    String state = m.get("signedOutAt") != null
                            ? "Signed out " + when(num(m.get("signedOutAt")))
                            : "Last seen " + when(num(m.get("lastAt")));
                    row.addView(SocialUi.text(this, state + (m.get("signedInAt") != null ? "  ·  signed in " + when(num(m.get("signedInAt"))) : ""),
                            12, false, UiColors.textSecondary()));
                    others.addView(row);
                }
            })
            .addOnFailureListener(e -> {
                others.removeAllViews();
                others.addView(SocialUi.text(this, "Couldn't load the list. Check your connection.", 13, false, UiColors.textSecondary()));
            });
    }

    private static long num(Object o) { return o instanceof Number ? ((Number) o).longValue() : 0L; }

    private static String when(long t) {
        return t <= 0 ? "unknown" : new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.US).format(new Date(t));
    }
}
