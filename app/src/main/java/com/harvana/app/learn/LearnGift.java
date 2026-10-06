package com.harvana.app.learn;

import android.app.Activity;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.harvana.app.chain.EngagementCanonical;
import com.harvana.app.trust.ProfileTrust;
import com.harvana.app.tx.TransactionRouter;
import com.harvana.app.wallet.PinPromptHelper;
import com.harvana.app.wallet.WalletManager;

import java.math.BigDecimal;

/**
 * A reader gives HARV to the person who posted. It is a normal signed transfer from the reader's wallet to the poster's
 * verified wallet (the address the node confirmed for that profile), with a LEARN_GIFT memo so the node can count it as
 * real engagement. The reader must confirm with their PIN or biometric first, like any payment.
 */
public final class LearnGift {
    private LearnGift() {}

    private static final String[] LABELS = {"0.1 HARV", "0.5 HARV", "1 HARV", "5 HARV"};
    private static final String[] AMOUNTS = {"0.1", "0.5", "1", "5"};

    public static void ask(Activity host, String contentId, ProfileTrust.Card poster) {
        if (host == null || contentId == null) return;
        String me = WalletManager.getActiveWalletAddress(host);
        if (me == null || me.isEmpty()) { toast(host, "Create or import a wallet first to send a gift."); return; }
        if (poster == null || poster.wallet.isEmpty()) { toast(host, "This person has not linked a wallet yet, so they cannot receive gifts."); return; }
        if (poster.wallet.equalsIgnoreCase(me)) { toast(host, "That is your own post."); return; }
        String who = poster.name.isEmpty() ? "this author" : poster.name;

        new com.harvana.app.ui.SheetDialog.Builder(host)
                .setTitle("Gift " + who)
                .setItems(LABELS, (d, which) -> confirm(host, contentId, poster, me, who, AMOUNTS[which]))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void confirm(Activity host, String contentId, ProfileTrust.Card poster, String me, String who, String amount) {
        PinPromptHelper.authorize(host, "Send a gift of " + amount + " HARV to " + who + ".", () ->
                TransactionRouter.routeTransaction(host, me, poster.wallet, new BigDecimal(amount), null,
                        EngagementCanonical.giftMemo(contentId), new TransactionRouter.RouterCallback() {
                            @Override public void onSuccess(String txHash) { toast(host, "Gift sent to " + who + ". Thank you."); }
                            @Override public void onError(String code, String msg) { toast(host, msg == null || msg.isEmpty() ? "The gift could not be sent." : msg); }
                        }));
    }

    private static void toast(Activity host, String message) {
        host.runOnUiThread(() -> Toast.makeText(host, message, Toast.LENGTH_LONG).show());
    }
}
