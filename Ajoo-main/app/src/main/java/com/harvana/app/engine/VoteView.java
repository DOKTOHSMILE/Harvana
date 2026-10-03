package com.harvana.app.engine;

import static com.harvana.app.engine.Amounts.*;

import java.util.Map;

/** A vote on the node (disbursement, milestone, grant or admin recovery), summarised for display. Pure Java. */
public final class VoteView {
    public String title = "";
    public String status = "";
    public int yesPercent, thresholdPercent;
    public int votersYes, votersTotal, minVoters;
    public boolean eligible, alreadyVoted, open;
    public boolean myVoteYes;

    @SuppressWarnings("unchecked")
    public static VoteView of(String title, Map<String, Object> rec, String me) {
        VoteView v = new VoteView();
        v.title = title;
        v.status = str(rec, "status");
        Object weights = rec.get("voterWeights"), votes = rec.get("votes");
        long total = 0, yes = 0;
        if (weights instanceof Map) {
            Map<String, Object> w = (Map<String, Object>) weights;
            v.votersTotal = w.size();
            for (Map.Entry<String, Object> e : w.entrySet()) {
                long wt = e.getValue() instanceof Number ? ((Number) e.getValue()).longValue() : 0;
                total += wt;
                if (votes instanceof Map && Boolean.TRUE.equals(((Map<String, Object>) votes).get(e.getKey()))) { yes += wt; v.votersYes++; }
            }
            v.eligible = w.containsKey(me);
        }
        if (votes instanceof Map) {
            Object mine = ((Map<String, Object>) votes).get(me);
            v.alreadyVoted = mine != null;
            v.myVoteYes = Boolean.TRUE.equals(mine);
        }
        v.yesPercent = total > 0 ? (int) (yes * 100 / total) : 0;
        v.thresholdPercent = (int) (lng(rec, "thresholdBps") > 0 ? lng(rec, "thresholdBps") / 100 : 51);
        v.minVoters = (int) lng(rec, "minVoters");
        v.open = "PENDING".equals(v.status) || "PENDING_APPROVAL".equals(v.status);
        return v;
    }

    /** One sentence a member can understand. */
    public String explain() {
        if (!open) return "Closed: " + status.toLowerCase(java.util.Locale.US).replace('_', ' ') + ".";
        String s = yesPercent + "% yes so far. It passes at " + thresholdPercent + "% with at least "
            + (minVoters > 0 ? minVoters : 2) + " yes votes, one of them not from the person asking.";
        if (!eligible) s += " You can't vote on this one (only the admin and the most active members can).";
        else if (alreadyVoted) s += " You voted " + (myVoteYes ? "yes" : "no") + ". You can change it until it closes.";
        return s;
    }
}
