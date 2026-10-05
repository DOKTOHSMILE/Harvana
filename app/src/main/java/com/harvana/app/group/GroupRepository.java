package com.harvana.app.group;

import android.content.Context;

import com.harvana.app.firestore.GroupFirestoreManager;
import com.harvana.app.sync.NodeGroupClient;
import com.harvana.app.wallet.BalanceFormatter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The only door from a group screen to protocol state. Presentation stays in Firestore.
 * Money, membership and votes are read and changed here, on the node id.
 */
public final class GroupRepository {
    private GroupRepository() {}

    public interface Classified { void onIdentity(GroupIdentity identity); void onError(String error); }

    public interface ProtocolView {
        void onView(String state, String creator, String escrow, long contributionRaw, int memberLimit, int round, boolean callerIsCreator);
    }

    /** Load the Firestore profile, then say which identity it is. Never guesses a node id. */
    public static void classify(Context context, String firestoreId, Classified cb) {
        GroupFirestoreManager.getInstance(context).getGroup(firestoreId, new GroupFirestoreManager.DocCallback() {
            @Override public void onSuccess(Map<String, Object> data) {
                cb.onIdentity(GroupIdentityResolver.resolve(firestoreId, data));
            }
            @Override public void onError(String error) {
                cb.onError("Could not read the group profile: " + error);
            }
        });
    }

    /** Live protocol fields. Does nothing for a legacy group. A read failure leaves the profile on screen. */
    public static void loadProtocol(GroupIdentity id, String wallet, ProtocolView cb) {
        if (id == null || !id.onChain() || cb == null) return;
        NodeGroupClient.Callback read = new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                String creator = first(json, "creatorAddress", "creator", "adminAddress");
                String escrow = first(json, "escrowAddress", "groupWalletAddress");
                long raw = json == null ? 0 : json.optLong("contributionAmountRaw", json.optLong("dailyAmountRaw", 0));
                int limit = json == null ? 0 : json.optInt("memberLimit", 0);
                int round = json == null ? 0 : json.optInt("currentRound", 0);
                String state = first(json, "state", "status");
                boolean mine = wallet != null && !wallet.isEmpty() && wallet.equalsIgnoreCase(creator);
                cb.onView(state, creator, escrow, raw, limit, round, mine);
            }
            @Override public void onError(String error) { }
        };
        if ("CIRCLE".equals(id.family)) NodeGroupClient.getCircle(id.protocolId(), read);
        else if ("ACCUM".equals(id.family)) NodeGroupClient.getAccumGroup(id.protocolId(), read);
        else NodeGroupClient.getFixedGroup(id.protocolId(), read);
    }

    public interface Members {
        void onMembers(List<Map<String, Object>> rows);
        void onError(String error);
    }

    /** Protocol roster. Circle rows are in payout order. Empty family does not call the node. */
    public static void loadMembers(GroupIdentity id, Members cb) {
        if (id == null || !id.onChain()) { cb.onError("Not a network group."); return; }
        NodeGroupClient.Callback read = new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) { cb.onMembers(rows(json)); }
            @Override public void onError(String error) { cb.onError(error); }
        };
        if ("CIRCLE".equals(id.family)) NodeGroupClient.getCircleMembers(id.protocolId(), read);
        else if ("ACCUM".equals(id.family)) NodeGroupClient.getAccumMembers(id.protocolId(), read);
        else NodeGroupClient.getFixedMembers(id.protocolId(), read);
    }

    private static List<Map<String, Object>> rows(JSONObject json) {
        List<Map<String, Object>> out = new ArrayList<>();
        JSONArray arr = json == null ? null : json.optJSONArray("members");
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject row = arr.optJSONObject(i);
            if (row == null) continue;
            String address = row.optString("address", row.optString("memberAddress", ""));
            Map<String, Object> m = new HashMap<>();
            m.put("walletAddress", address);
            m.put("address", address);
            m.put("state", row.optString("state", ""));
            m.put("status", "ACTIVE");
            m.put("slot", i + 1);
            m.put("source", "node");
            if (row.has("paidRaw")) m.put("paidRaw", row.optLong("paidRaw"));
            if (row.has("payoutRound")) m.put("payoutRound", row.optInt("payoutRound"));
            if (row.has("bondRemainingRaw")) m.put("bondRemainingRaw", row.optLong("bondRemainingRaw"));
            out.add(m);
        }
        return out;
    }
    public static void requestPayout(Context context, GroupIdentity id, String wallet, NodeGroupClient.Callback cb) {
        if (!GroupCapability.canRequestDisbursement(id)) {
            String msg = GroupCapability.payoutFollowsLockedOrder(id)
                    ? "This circle pays members in the order locked on the network. There is no separate payout to request."
                    : GroupCapability.canClaimLine(id)
                    ? "Claim a packed savings line. A payout request in the app would not pay you."
                    : "This group has no network payout to request.";
            cb.onError(msg);
            return;
        }
        NodeGroupClient.requestFixedDisbursement(context, wallet, id.protocolId(), cb);
    }

    /**
     * GenLoan request. The node accepts 24 to 720 hours
     * (HarvanaProtocolManager.GENLOAN_MIN_DURATION_HOURS / MAX).
     * Monthly maps to 720. Daily maps to 24. Anything else is refused.
     */
    public static void requestLoan(Context context, GroupIdentity id, String wallet, double principalHarv, String period, NodeGroupClient.Callback cb) {
        if (id == null || !id.onChain()) {
            cb.onError("This loan is not a network loan.");
            return;
        }
        String p = period == null ? "MONTHLY" : period.trim().toUpperCase();
        long hours = "DAILY".equals(p) ? 24L : "MONTHLY".equals(p) ? 720L : -1L;
        if (hours < 0) {
            cb.onError("The network allows a loan of 1 to 30 days. A longer period is not a protocol option.");
            return;
        }
        long raw = BalanceFormatter.harvToRaw(principalHarv);
        if (raw <= 0) { cb.onError("Enter an amount the network can record."); return; }
        NodeGroupClient.requestLoan(context, wallet, id.protocolId(), raw, hours, cb);
    }

    private static String first(JSONObject json, String... keys) {
        if (json == null) return "";
        for (String key : keys) {
            String s = json.optString(key, "").trim();
            if (!s.isEmpty()) return s;
        }
        return "";
    }
}
