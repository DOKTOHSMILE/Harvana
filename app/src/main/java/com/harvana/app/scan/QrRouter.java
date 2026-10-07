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
 *   profile code (harvana://user/<name>)        -> that member's profile
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

    /** Opens whatever the code is for. */
    public static void route(Activity a, String raw) {
        String t = raw == null ? "" : raw.trim();
        switch (kindOf(t)) {
            case ADDRESS:
                a.startActivity(new Intent(a, com.harvana.app.SendAssetActivity.class).putExtra("toAddress", t));
                return;
            case ACCESS_CARD:
                a.startActivity(new Intent(a, com.harvana.app.RestoreWalletActivity.class)
                        .putExtra(com.harvana.app.RestoreWalletActivity.EXTRA_SCANNED_CARD, t));
                return;
            case PROFILE: {
                String name = t.substring(PROFILE_PREFIX.length()).trim();
                com.harvana.app.social.SocialStore.findByUsername(name, (person, err) -> a.runOnUiThread(() -> {
                    if (person == null) { Toast.makeText(a, err == null ? "No member found for that code." : err, Toast.LENGTH_LONG).show(); return; }
                    a.startActivity(new Intent(a, com.harvana.app.social.PublicProfileActivity.class)
                            .putExtra(com.harvana.app.social.PublicProfileActivity.EXTRA_UID, person.uid));
                }));
                return;
            }
            case TRANSACTION:
                TxVerifySheet.show(a, txHash(t));
                return;
            default:
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
