package com.harvanna.app.group;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.harvanna.app.firestore.GroupFirestoreManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Connects a group to its chat on another app (WhatsApp, Telegram...). The link is stored in the
 * members-only private area, so only the group's members can read it.
 */
public final class GroupChatConnector {
    private static final String DOC = "chatLink";

    private GroupChatConnector() {}

    /** Opens the group's chat, or explains how to get one. */
    public static void open(Activity a, String groupId, boolean isAdmin) {
        GroupFirestoreManager.getInstance(a).getPrivateDoc(groupId, DOC, new GroupFirestoreManager.DocCallback() {
            @Override public void onSuccess(Map<String, Object> d) {
                String url = ChatLink.normalize(String.valueOf(d.get("url")));
                a.runOnUiThread(() -> {
                    if (url == null) { none(a, groupId, isAdmin); return; }
                    confirmOpen(a, url);
                });
            }
            @Override public void onError(String e) {
                a.runOnUiThread(() -> none(a, groupId, isAdmin));
            }
        });
    }

    private static void confirmOpen(Activity a, String url) {
        String platform = ChatLink.platformOf(url);
        new AlertDialog.Builder(a)
            .setTitle("Open the group chat")
            .setMessage("This chat is on " + platform + ", outside Harvanna. The group admin runs it.\n\n"
                + "Never share your recovery words, passphrase or PIN there. Payments happen only inside the Harvanna app.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Open " + platform, (d, w) -> {
                try { a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
                catch (Exception e) { Toast.makeText(a, "No app found to open this link.", Toast.LENGTH_LONG).show(); }
            }).show();
    }

    private static void none(Activity a, String groupId, boolean isAdmin) {
        if (isAdmin) { edit(a, groupId); return; }
        new AlertDialog.Builder(a).setTitle("No group chat yet")
            .setMessage("The admin hasn't added a chat link yet.")
            .setPositiveButton("OK", null).show();
    }

    /** Admin: paste or change the chat link. */
    public static void edit(Activity a, String groupId) {
        EditText in = new EditText(a);
        in.setHint("Paste your group chat link");
        in.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        new AlertDialog.Builder(a)
            .setTitle("Group chat link")
            .setMessage("Add a WhatsApp, Telegram, Signal, Discord or Facebook group link. Only members of this group can see it.")
            .setView(in)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (d, w) -> {
                String url = ChatLink.normalize(in.getText().toString());
                if (url == null) {
                    Toast.makeText(a, "That isn't a supported chat link. Use an https link from WhatsApp, Telegram, Signal, Discord or Facebook.", Toast.LENGTH_LONG).show();
                    return;
                }
                Map<String, Object> f = new HashMap<>();
                f.put("url", url);
                f.put("platform", ChatLink.platformOf(url));
                f.put("updatedAt", System.currentTimeMillis());
                GroupFirestoreManager.getInstance(a).setPrivateDoc(groupId, DOC, f, new GroupFirestoreManager.SimpleCallback() {
                    @Override public void onSuccess() { a.runOnUiThread(() -> Toast.makeText(a, "Chat link saved", Toast.LENGTH_SHORT).show()); }
                    @Override public void onError(String e) { a.runOnUiThread(() -> Toast.makeText(a, "Couldn't save: " + e, Toast.LENGTH_LONG).show()); }
                });
            }).show();
    }
}
