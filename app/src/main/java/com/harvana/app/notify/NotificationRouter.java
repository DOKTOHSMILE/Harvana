package com.harvana.app.notify;

import android.content.Context;
import android.content.Intent;

import com.harvana.app.GroupDetailsActivity;
import com.harvana.app.InvitesActivity;
import com.harvana.app.MainActivity;
import com.harvana.app.P2pMyTradesActivity;
import com.harvana.app.TransactionDetailActivity;
import com.harvana.app.sync.HarvanaNodeClient;

import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Where a notification goes when it is tapped, from the inbox list or from the phone's notification shade. One place, so both
 * behave the same: a payment goes to that transaction, a group notice to that group's own page, an invitation to Invitations.
 */
public final class NotificationRouter {
    private NotificationRouter() {}

    private static final Pattern TX_HASH = Pattern.compile("^tx_([0-9a-fA-F]{16,})_");

    /** Marks the notification read and opens its page. Safe from any context (a service, an activity, a trampoline). */
    public static void open(Context ctx, NotificationCenter.Item it) {
        if (ctx == null || it == null) return;
        final Context c = ctx.getApplicationContext();
        NotificationCenter.markRead(c, it.id);

        Matcher m = TX_HASH.matcher(it.id == null ? "" : it.id);
        if (NotificationCenter.TX.equals(it.type) && m.find()) { openTransaction(c, m.group(1)); return; }
        start(c, intentFor(c, it));
    }

    /** The page for everything that does not need a lookup first. Never null: the worst case is the inbox itself. */
    static Intent intentFor(Context c, NotificationCenter.Item it) {
        String id = it.id == null ? "" : it.id;
        switch (it.type == null ? "" : it.type) {
            case NotificationCenter.INVITE:
                return new Intent(c, InvitesActivity.class);
            case NotificationCenter.SOCIAL:
                if (id.startsWith("p2pchat_")) return new Intent(c, P2pMyTradesActivity.class);
                return new Intent(c, com.harvana.app.social.FriendsActivity.class);
            case NotificationCenter.TX:
                return main(c, "asset");
            case NotificationCenter.MEMBERSHIP:
                if (id.endsWith("_rejected")) return main(c, "discover");
                if (!it.groupId.isEmpty()) return group(c, it.groupId, 0);
                return main(c, "groups");
            case NotificationCenter.SYSTEM:
                if (id.startsWith("learn_")) return main(c, "learn");
                break;
            default:
                break;
        }
        if (it.groupId != null && !it.groupId.isEmpty()) {
            int page = it.tab >= 0 ? it.tab : (NotificationCenter.PAYMENT.equals(it.type) ? 2 : NotificationCenter.LOAN.equals(it.type) ? 3 : 4);
            return group(c, it.groupId, page);
        }
        return new Intent(c, com.harvana.app.NotificationsActivity.class);
    }

    private static Intent group(Context c, String groupId, int page) {
        Intent i = new Intent(c, GroupDetailsActivity.class).putExtra("groupId", groupId);
        if (page >= 0) i.putExtra("tab", page);
        return i;
    }

    private static Intent main(Context c, String fragment) {
        return new Intent(c, MainActivity.class).putExtra("open_fragment", fragment)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    private static void start(Context c, Intent i) {
        if (i == null) return;
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { c.startActivity(i); } catch (Exception ignored) { }
    }

    /** Reads the transaction (and how many blocks confirm it) from the network and opens its detail page; the wallet tab if it cannot be read. */
    private static void openTransaction(final Context c, final String hash) {
        HarvanaNodeClient.nodeGet("/api/v1/tx/" + hash, new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(final JSONObject tx) {
                // The detail lookup does not carry the confirmation count; the status lookup does.
                HarvanaNodeClient.nodeGet("/api/v1/tx/status/" + hash, new HarvanaNodeClient.JsonCallback() {
                    @Override public void onSuccess(JSONObject st) { startDetail(c, hash, tx, st.optLong("confirmations", 0L)); }
                    @Override public void onError(String error) { startDetail(c, hash, tx, 0L); }
                });
            }
            @Override public void onError(String error) { start(c, main(c, "asset")); }
        });
    }

    private static void startDetail(Context c, String hash, JSONObject tx, long confirmations) {
        long block = tx.optLong("blockHeight", tx.optLong("block", 0L));
        long conf = confirmations > 0 ? confirmations : (block >= 0 && tx.has("blockHeight") ? 1L : 0L);
        Intent i = new Intent(c, TransactionDetailActivity.class);
        i.putExtra("txHash", hash);
        i.putExtra("status", conf > 0 ? "confirmed" : "pending");
        i.putExtra("senderAddress", tx.optString("from", ""));
        i.putExtra("recipientAddress", tx.optString("to", ""));
        i.putExtra("tokenSymbol", com.harvana.app.CoinInfo.ticker());
        i.putExtra("value", HarvanaNodeClient.toHarvDisplay(tx.optLong("amount", 0L)));
        i.putExtra("harvFee", HarvanaNodeClient.toHarvDisplay(tx.optLong("gas", tx.optLong("fee", 0L))));
        i.putExtra("burnAmount", tx.optDouble("burnAmount", 0.0));
        i.putExtra("timestamp", tx.optLong("timestamp", System.currentTimeMillis()));
        i.putExtra("memo", tx.optString("memo", ""));
        i.putExtra("confirmations", conf);
        i.putExtra("blockNumber", block);
        i.putExtra("nonce", tx.optLong("nonce", 0L));
        start(c, i);
    }
}
