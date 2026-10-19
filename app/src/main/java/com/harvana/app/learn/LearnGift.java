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
 *
 * <p>Harvana's share (owner, 19 Oct 2026): 5% of the gift goes to the company treasury in a second transfer
 * ({@code HARVANA_SERVICE:LEARN_GIFT:<contentId>}); the author gets 95%. The reader pays the gift amount.
 */
public final class LearnGift {
    private LearnGift() {}

    private static final String[] LABELS = {"0.1 HARV", "0.5 HARV", "1 HARV", "5 HARV"};
    private static final String[] AMOUNTS = {"0.1", "0.5", "1", "5"};

    /** The node's company treasury: "HV0x" + doubleSha256("PROTOCOL_COMPANY_TREASURY")[0..40]. */
    public static final String TREASURY = "HV0x5ce3ef914c96f22e630c6b4b73aaceef1ec5f49a";
    /** Harvana's share of a gift, in basis points (5%). */
    static final int SHARE_BPS = 500;

    /** {harvana share, author part} of a gift, 8 decimals, the share rounded down. */
    static BigDecimal[] split(BigDecimal gift) {
        BigDecimal share = gift.multiply(BigDecimal.valueOf(SHARE_BPS)).divide(BigDecimal.valueOf(10_000), 8, java.math.RoundingMode.DOWN);
        return new BigDecimal[]{share, gift.subtract(share)};
    }

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
        BigDecimal[] parts = split(new BigDecimal(amount));
        String authorPart = parts[1].stripTrailingZeros().toPlainString();
        PinPromptHelper.authorize(host, "Send a gift of " + amount + " HARV to " + who + ".", () ->
                TransactionRouter.routeTransaction(host, me, dest, parts[1], null,
                        EngagementCanonical.giftMemo(contentId), new TransactionRouter.RouterCallback() {
                            @Override public void onSuccess(String txHash) {
                                toast(host, "Gift sent to " + who + ". Thank you.");
                                LearnSocial.recordGift(contentId, txHash, authorPart, dest, (ok, err) -> { if (ok && onGifted != null) host.runOnUiThread(onGifted); });
                                if (parts[0].signum() > 0) {
                                    TransactionRouter.routeTransaction(host, me, TREASURY, parts[0], null,
                                            "HARVANA_SERVICE:LEARN_GIFT:" + contentId, new TransactionRouter.RouterCallback() {
                                                @Override public void onSuccess(String h) { }
                                                @Override public void onError(String code, String msg) {
                                                    android.util.Log.w("LearnGift", "Harvana share not sent: " + code);
                                                }
                                            });
                                }
                            }
                            @Override public void onError(String code, String msg) { toast(host, msg == null || msg.isEmpty() ? "The gift could not be sent." : msg); }
                        }));
    }

    private static void toast(Activity host, String message) {
        host.runOnUiThread(() -> Toast.makeText(host, message, Toast.LENGTH_LONG).show());
    }
}
