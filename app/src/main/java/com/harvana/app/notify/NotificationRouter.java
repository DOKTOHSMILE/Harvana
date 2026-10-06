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

    /** Notification page code for "a vote is waiting": the group's Actions & votes screen. */
    public static final int VOTES = 99;

    private static final Pattern TX_HASH = Pattern.compile("^tx_([0-9a-fA-F]{16,})_");

    /** Marks the notification read and opens its page. Safe from any context (a service, an activity, a trampoline). */
    public static void open(Context ctx, NotificationCenter.Item it) {
        if (ctx == null || it == null) return;
        final Context c = ctx.getApplicationContext();
        NotificationCenter.markRead(c, it.id);

        Matcher m = TX_HASH.matcher(it.id == null ? "" : it.id);
        if (NotificationCenter.TX.equals(it.type) && m.find()) { openTransaction(c, m.group(1)); return; }
        if (it.tab == VOTES && it.groupId != null && !it.groupId.isEmpty()) { openVotes(c, it.groupId); return; }
        if (NotificationCenter.SYSTEM.equals(it.type) && it.id != null && it.id.startsWith("learn_") && it.id.endsWith("_approved")) {
            openLearnPost(c, it.id.substring("learn_".length(), it.id.length() - "_approved".length()));
            return;
        }
        start(c, intentFor(c, it));
    }

    /** The page for everything that does not need a lookup first. Never null: the worst case is the inbox itself. */
    static Intent intentFor(Context c, NotificationCenter.Item it) {
        String id = it.id == null ? "" : it.id;
        switch (it.type == null ? "" : it.type) {
            case NotificationCenter.INVITE:
                // The invitation itself (its group's details and the Join choice), not the list of invitations.
                if (it.groupId != null && !it.groupId.isEmpty() && id.startsWith("invite_")) {
                    return new Intent(c, com.harvana.app.GroupJoinRequestActivity.class)
                        .putExtra("groupId", it.groupId).putExtra("inviteId", id.substring("invite_".length()));
                }
                return new Intent(c, InvitesActivity.class);
            case NotificationCenter.SOCIAL:
                if (id.startsWith("p2pchat_")) {
                    Intent chat = chatIntent(c, id);
                    return chat != null ? chat : new Intent(c, P2pMyTradesActivity.class);
                }
                if (id.startsWith("pinch_")) {                    // the person who pinched: their profile
                    String[] parts = id.split("_");
                    if (parts.length >= 3) return new Intent(c, com.harvana.app.social.PublicProfileActivity.class)
                        .putExtra(com.harvana.app.social.PublicProfileActivity.EXTRA_UID, parts[1]);
                }
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

    /** "p2pchat_{orderId}_{uidA}_{uidB}_{time}": the trade chat with the other person. Null when the id cannot be read. */
    private static Intent chatIntent(Context c, String id) {
        try {
            String[] p = id.substring("p2pchat_".length()).split("_");
            if (p.length < 4) return null;
            String me = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null ? ""
                : com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
            String a = p[p.length - 3], b = p[p.length - 2];
            String other = me.equals(a) ? b : me.equals(b) ? a : "";
            StringBuilder order = new StringBuilder();
            for (int i = 0; i < p.length - 3; i++) order.append(i == 0 ? "" : "_").append(p[i]);
            if (other.isEmpty() || order.length() == 0) return null;
            return new Intent(c, com.harvana.app.P2pChatActivity.class)
                .putExtra(com.harvana.app.P2pChatActivity.EXTRA_ORDER, order.toString())
                .putExtra(com.harvana.app.P2pChatActivity.EXTRA_OTHER, other);
        } catch (Exception e) { return null; }
    }

    /** The published article itself; the Learn tab if it cannot be found. */
    private static void openLearnPost(final Context c, final String contentId) {
        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("LEARN_CONTENT").document(contentId).get()
                .addOnCompleteListener(task -> {
                    com.google.firebase.firestore.DocumentSnapshot d = task.isSuccessful() ? task.getResult() : null;
                    if (d == null || !d.exists()) { start(c, main(c, "learn")); return; }
                    Intent i = new Intent(c, com.harvana.app.LearnReadActivity.class);
                    i.putExtra(com.harvana.app.LearnReadActivity.EXTRA_ID, d.getId());
                    i.putExtra(com.harvana.app.LearnReadActivity.EXTRA_TITLE, str(d, "title"));
                    i.putExtra(com.harvana.app.LearnReadActivity.EXTRA_BODY, str(d, "description"));
                    i.putExtra(com.harvana.app.LearnReadActivity.EXTRA_CATEGORY, str(d, "category"));
                    i.putExtra(com.harvana.app.LearnReadActivity.EXTRA_IMAGE, str(d, "imageUrl"));
                    i.putExtra(com.harvana.app.LearnReadActivity.EXTRA_VIDEO, str(d, "videoUrl"));
                    i.putExtra(com.harvana.app.LearnReadActivity.EXTRA_AUTHOR, str(d, "authorUid"));
                    Long at = d.getLong("createdAt"), views = d.getLong("views");
                    i.putExtra(com.harvana.app.LearnReadActivity.EXTRA_CREATED, at == null ? 0L : at);
                    i.putExtra(com.harvana.app.LearnReadActivity.EXTRA_VIEWS, views == null ? 0L : views);
                    start(c, i);
                });
        } catch (Exception e) { start(c, main(c, "learn")); }
    }

    private static String str(com.google.firebase.firestore.DocumentSnapshot d, String key) {
        Object o = d.get(key);
        return o == null ? "" : String.valueOf(o);
    }

    /** A group on the network votes on its own screen (Actions & votes); any other group votes in the Members tab. */
    private static void openVotes(final Context c, final String groupId) {
        com.harvana.app.firestore.GroupFirestoreManager.getInstance(c).getGroup(groupId, new com.harvana.app.firestore.GroupFirestoreManager.DocCallback() {
            @Override public void onSuccess(java.util.Map<String, Object> data) {
                com.harvana.app.group.GroupIdentity identity = com.harvana.app.group.GroupIdentityResolver.resolve(groupId, data);
                if (identity != null && identity.onChain()) {
                    start(c, new Intent(c, com.harvana.app.group.NodeGroupActivity.class)
                        .putExtra("nodeGroupId", identity.protocolId()).putExtra("firestoreGroupId", groupId));
                } else {
                    start(c, group(c, groupId, 1));
                }
            }
            @Override public void onError(String error) { start(c, group(c, groupId, 4)); }
        });
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
