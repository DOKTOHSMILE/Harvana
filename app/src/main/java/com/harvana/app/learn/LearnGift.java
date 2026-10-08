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
 * A reader gives HARV to the person who posted. The gift goes to the wallet that was signed in when the article was
 * posted. Older articles, which have no wallet of their own, still pay the wallet linked on that person's profile.
 * It is a normal signed transfer, with a LEARN_GIFT memo so the node can count it as real engagement. The reader must
 * confirm with their PIN or biometric first, like any payment.
 */
public final class LearnGift {
    private LearnGift() {}

    private static final String[] LABELS = {"0.1 HARV", "0.5 HARV", "1 HARV", "5 HARV"};
    private static final String[] AMOUNTS = {"0.1", "0.5", "1", "5"};

    public static void ask(Activity host, String contentId, ProfileTrust.Card poster) { ask(host, contentId, poster, null, null); }

    public static void ask(Activity host, String contentId, ProfileTrust.Card poster, Runnable onGifted) {
        ask(host, contentId, poster, null, onGifted);
    }

    /** {@code authorWallet} is the wallet signed in when the article was posted. Empty falls back to the profile wallet. */
    public static void ask(Activity host, String contentId, ProfileTrust.Card poster, String authorWallet, Runnable onGifted) {
        if (host == null || contentId == null) return;
        String me = WalletManager.getActiveWalletAddress(host);
        if (me == null || me.isEmpty()) { toast(host, "Create or import a wallet first to send a gift."); return; }
        String dest = authorWallet != null && !authorWallet.trim().isEmpty() ? authorWallet.trim()
                : (poster == null ? "" : poster.wallet);
        if (dest.isEmpty()) { toast(host, "This article has no wallet to receive a gift yet."); return; }
        if (dest.equalsIgnoreCase(me)) { toast(host, "That is your own post."); return; }
        String who = poster != null && !poster.name.isEmpty() ? poster.name : "this author";

        new com.harvana.app.ui.SheetDialog.Builder(host)
                .setTitle("Gift " + who)
                .setItems(LABELS, (d, which) -> confirm(host, contentId, dest, me, who, AMOUNTS[which], onGifted))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void confirm(Activity host, String contentId, String dest, String me, String who, String amount, Runnable onGifted) {
        PinPromptHelper.authorize(host, "Send a gift of " + amount + " HARV to " + who + ".", () ->
                TransactionRouter.routeTransaction(host, me, dest, new BigDecimal(amount), null,
                        EngagementCanonical.giftMemo(contentId), new TransactionRouter.RouterCallback() {
                            @Override public void onSuccess(String txHash) {
                                toast(host, "Gift sent to " + who + ". Thank you.");
                                LearnSocial.recordGift(contentId, txHash, amount, dest, (ok, err) -> { if (ok && onGifted != null) host.runOnUiThread(onGifted); });
                            }
                            @Override public void onError(String code, String msg) { toast(host, msg == null || msg.isEmpty() ? "The gift could not be sent." : msg); }
                        }));
    }

    private static void toast(Activity host, String message) {
        host.runOnUiThread(() -> Toast.makeText(host, message, Toast.LENGTH_LONG).show());
    }
}
