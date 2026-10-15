package com.harvana.app.scan;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Every QR code the app makes, recognised and opened in the right place:
 *   wallet address (Receive, coin page)       -> Send, recipient filled in
 *   wallet access card (HARVANACARD2:...)       -> add the wallet: restore asks for the card password
 *   profile code (harvana://user/<name>)        -> add that member as a friend, then their profile
 *   receipt (harvana://tx/<hash>, or a bare hash on older receipts) -> checked on the network
 * Anything else is shown as text the user can copy.
 */
public final class QrRouter {
    private QrRouter() {}

    public enum Kind { ADDRESS, ACCESS_CARD, PROFILE, TRANSACTION, UNKNOWN }

    public static final String TX_PREFIX = "harvana://tx/";
    public static final String PROFILE_PREFIX = "harvana://user/";
    private static final Pattern HASH = Pattern.compile("^[0-9a-fA-F]{64}$");

    /** What a scanned text is. */
    public static Kind kindOf(String raw) {
        String t = raw == null ? "" : raw.trim();
        if (t.startsWith(com.harvana.app.wallet.HarvanaCardCrypto.FORMAT_PREFIX)) return Kind.ACCESS_CARD;
        if (t.startsWith(PROFILE_PREFIX)) return Kind.PROFILE;
        if (t.toLowerCase(Locale.US).startsWith(TX_PREFIX) || HASH.matcher(t).matches()) return Kind.TRANSACTION;
        if (com.harvana.app.AddressUtil.isValidAddress(t)) return Kind.ADDRESS;
        return Kind.UNKNOWN;
    }

    /** The transaction hash in a receipt code, or "" when it is not one. */
    public static String txHash(String raw) {
        String t = raw == null ? "" : raw.trim();
        if (t.toLowerCase(Locale.US).startsWith(TX_PREFIX)) t = t.substring(TX_PREFIX.length());
        int q = t.indexOf('?');
        if (q >= 0) t = t.substring(0, q);
        return HASH.matcher(t).matches() ? t.toLowerCase(Locale.US) : "";
    }

    /** The QR payload a receipt carries for a transaction. */
    public static String receiptPayload(String txHash) {
        return txHash == null || txHash.isEmpty() ? "" : TX_PREFIX + txHash;
    }

    /**
     * A scanned username code: offers to add that member as a friend straight away, then opens their profile.
     * Your own code just opens your profile.
     */
    static void addFriend(Activity a, com.harvana.app.social.Person p) {
        final Intent profile = new Intent(a, com.harvana.app.social.PublicProfileActivity.class)
                .putExtra(com.harvana.app.social.PublicProfileActivity.EXTRA_UID, p.uid);
        String me = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null ? ""
                : com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (p.uid.equals(me)) { a.startActivity(profile); return; }
        String who = p.name != null && !p.name.isEmpty() ? p.name : "@" + p.username;
        new com.harvana.app.ui.SheetDialog.Builder(a).setTitle("Add " + who + "?")
                .setMessage((p.username == null || p.username.isEmpty() ? "" : "@" + p.username + "\n\n")
                        + "Add them as a friend to see their posts and send to them quickly.")
                .setPositiveButton("Add friend", (d, w) -> com.harvana.app.social.SocialStore.follow(p, (ok, err) -> a.runOnUiThread(() -> {
                    Toast.makeText(a, Boolean.TRUE.equals(ok) ? who + " added as a friend" : (err == null ? "Couldn't add them. Try again." : err),
                            Toast.LENGTH_LONG).show();
                    a.startActivity(profile);
                })))
                .setNegativeButton("View profile", (d, w) -> a.startActivity(profile))
                .show();
    }

    /** Opens whatever the code is for. */
    public static void route(Activity a, String raw) {
        String t = raw == null ? "" : raw.trim();
        WalletQr.Hit wallet = WalletQr.parse(t);
        switch (kindOf(t)) {
            case ACCESS_CARD:
                a.startActivity(new Intent(a, com.harvana.app.RestoreWalletActivity.class)
                        .putExtra(com.harvana.app.RestoreWalletActivity.EXTRA_SCANNED_CARD, t));
                return;
            case PROFILE: {
                String name = t.substring(PROFILE_PREFIX.length()).trim();
                com.harvana.app.social.SocialStore.findByUsername(name, (person, err) -> a.runOnUiThread(() -> {
                    if (person == null) { Toast.makeText(a, err == null ? "No member found for that code." : err, Toast.LENGTH_LONG).show(); return; }
                    addFriend(a, person);
                }));
                return;
            }
            case TRANSACTION:
                TxVerifySheet.show(a, txHash(t));
                return;
            case ADDRESS:
            default:
                if (wallet != null) {
                    a.startActivity(new Intent(a, com.harvana.app.SendAssetActivity.class)
                            .putExtra("toAddress", wallet.address)
                            .putExtra("amount", wallet.amount)
                            .putExtra("memo", wallet.memo)
                            .putExtra(com.harvana.app.wallet.CoinCatalog.EXTRA, wallet.key));
                    return;
                }
                new com.harvana.app.ui.SheetDialog.Builder(a).setTitle("Scanned code")
                        .setMessage(t.isEmpty() ? "The code was empty." : t + "\n\nThis is not a Harvana code.")
                        .setPositiveButton("Copy", (d, w) -> {
                            android.content.ClipboardManager cm = (android.content.ClipboardManager) a.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                            if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText("Scanned code", t));
                            Toast.makeText(a, "Copied", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Close", null).show();
        }
    }
}
