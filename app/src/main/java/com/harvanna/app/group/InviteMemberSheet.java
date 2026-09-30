package com.harvanna.app.group;

import android.app.Activity;
import android.content.Intent;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.harvanna.app.utils.UiColors;

import java.util.Map;

/** Admin (or any member, if the group allows it) invites someone by email. */
public final class InviteMemberSheet {
    private InviteMemberSheet() {}

    public static void show(Activity a, String groupId, Map<String, Object> group) {
        boolean creator = false;
        com.google.firebase.auth.FirebaseUser me = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (me != null) creator = me.getUid().equals(String.valueOf(group.get("creatorId")));
        boolean membersMayInvite = Boolean.TRUE.equals(group.get("membersCanInvite"));
        if (!creator && !membersMayInvite) {
            Toast.makeText(a, "Only the admin can invite people to this group.", Toast.LENGTH_LONG).show();
            return;
        }
        float d = a.getResources().getDisplayMetrics().density;
        int pad = (int) (16 * d);
        BottomSheetDialog dlg = new BottomSheetDialog(a);
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(UiColors.surface());

        TextView t = new TextView(a);
        t.setText("Invite someone");
        t.setTextSize(18);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setTextColor(UiColors.textPrimary());
        root.addView(t);
        TextView info = new TextView(a);
        info.setText("They will be notified, see the group's rules and details, and decide for themselves. "
            + "Invites last 7 days. They can join even if they haven't signed up yet.");
        info.setTextSize(12);
        info.setTextColor(UiColors.textSecondary());
        info.setPadding(0, (int) (4 * d), 0, (int) (8 * d));
        root.addView(info);

        final com.harvanna.app.social.PeoplePicker.Selection sel = new com.harvanna.app.social.PeoplePicker.Selection();
        root.addView(com.harvanna.app.social.PeoplePicker.section(a, sel));

        EditText email = new EditText(a);
        email.setHint("Their email address");
        email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        root.addView(email);
        EditText msg = new EditText(a);
        msg.setHint("Personal message (optional)");
        root.addView(msg);

        MaterialButton send = new MaterialButton(a);
        send.setText("Send invitations");
        send.setAllCaps(false);
        send.setOnClickListener(v -> {
            String typed = InviteManager.normalizeEmail(email.getText().toString());
            if (!typed.isEmpty() && !sel.emails.contains(typed)) sel.emails.add(typed);
            if (sel.count() == 0) { Toast.makeText(a, "Add a friend, a username, a contact or type an email.", Toast.LENGTH_LONG).show(); return; }
            send.setEnabled(false);
            com.harvanna.app.social.PeoplePicker.sendAll(a, groupId, group, sel, msg.getText().toString(), (sent, failed) ->
                a.runOnUiThread(() -> {
                    send.setEnabled(true);
                    if (failed > 0) { Toast.makeText(a, sent + " sent, " + failed + " couldn't be sent.", Toast.LENGTH_LONG).show(); return; }
                    Toast.makeText(a, sent == 1 ? "Invitation sent" : sent + " invitations sent", Toast.LENGTH_SHORT).show();
                    dlg.dismiss();
                }));
        });
        root.addView(send);

        MaterialButton share = new MaterialButton(a, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        share.setText("Also share a message");
        share.setAllCaps(false);
        share.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, "I invited you to join \"" + group.get("groupName")
                + "\" on Harvanna. Install the app, sign up with this email, and open Invitations to read the details.");
            a.startActivity(Intent.createChooser(i, "Share invitation"));
        });
        root.addView(share);

        android.widget.ScrollView scroller = new android.widget.ScrollView(a);
        scroller.setBackgroundColor(UiColors.surface());
        scroller.addView(root);
        dlg.setContentView(scroller);
        dlg.show();
    }
}
