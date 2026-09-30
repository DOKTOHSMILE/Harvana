package com.harvanna.app.group;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the plain-language "how this group works" contract a new member reads and signs
 * before joining. Pure Java (no Android types) so it is unit-testable.
 *
 * The text is generated from the group's own settings (set by the creator) plus the rules the
 * node enforces for that group family. The SHA-256 of the canonical text is what the member's
 * wallet signs, so the signature proves exactly which terms were accepted.
 */
public final class GroupContract {

    public static final class Section {
        public final String title;
        public final List<String> points = new ArrayList<>();
        Section(String title) { this.title = title; }
        Section add(String p) { points.add(p); return this; }
    }

    private GroupContract() {}

    public static List<Section> sections(Map<String, Object> g, String ticker) {
        List<Section> out = new ArrayList<>();
        String type = str(g, "groupType", "");
        String t = type.toLowerCase(Locale.US);
        boolean rotating = t.contains("rotating");
        boolean fixed = t.contains("fixed");
        boolean accum = t.contains("accumul");
        String name = str(g, "groupName", "this group");

        Section about = new Section("About " + name);
        about.add("Type: " + (type.isEmpty() ? "Savings group" : type) + ".");
        String desc = str(g, "groupDescription", str(g, "description", ""));
        if (!desc.isEmpty()) about.add(desc);
        long slots = (long) num(g, "totalSlots");
        if (slots > 0) about.add("Size: up to " + slots + " members.");
        about.add("Set up by the group admin (the creator). The admin sets the rules below and controls the group wallet.");
        out.add(about);

        Section money = new Section("What you pay");
        double amount = num(g, "contributionAmount");
        String freq = str(g, "frequency", "");
        if (amount > 0) {
            money.add("Contribution: " + fmt(amount) + " " + ticker + (freq.isEmpty() ? "" : " (" + freq.toLowerCase(Locale.US) + ")") + ".");
        }
        if (rotating) {
            money.add("Each round one member receives the pool, in a fixed slot order. Your slot decides when you are paid.");
            money.add("Paying on time protects everyone: a missed payment delays the member whose turn it is.");
            double grace = num(g, "gracePeriodDays");
            if (grace > 0) money.add("Grace period: " + fmt(grace) + " day(s) after a due date before a payment counts as late.");
            String cons = str(g, "defaultConsequence", "");
            if (!cons.isEmpty()) money.add("If you default: " + cons + ".");
            double comm = num(g, "commissionPercent");
            if (comm > 0) money.add("Commission: " + fmt(comm) + "% of each payout.");
        } else if (fixed) {
            double goal = num(g, "targetGoalAmount");
            if (goal > 0) money.add("The group is saving toward " + fmt(goal) + " " + ticker + ".");
            String rule = str(g, "disbursementRule", "");
            if (!rule.isEmpty()) money.add("Money leaves the pool only by this rule: " + rule + ".");
            money.add("You cannot withdraw your deposit on your own. Disbursements, milestones and emergency grants each need a group vote.");
        } else if (accum) {
            String w = str(g, "withdrawalRule", "");
            money.add("You build your own savings line inside the group and can add to it regularly.");
            if (!w.isEmpty()) money.add("Withdrawal rule: " + w + ".");
        }
        out.add(money);

        if (allowsLoans(g)) {
            Section loans = new Section("Loans");
            double rate = num(g, "loanInterestRate");
            String period = str(g, "loanInterestPeriod", "MONTHLY").toLowerCase(Locale.US);
            loans.add("This group lets members borrow from the pool.");
            loans.add("Interest: " + fmt(rate) + "% per " + period.replace("ly", "") + " (as set by the admin).");
            loans.add("Borrowers must repay in full. Unpaid loans are taken into account against your share and your trust score.");
            out.add(loans);
        } else {
            out.add(new Section("Loans").add("This group does not offer loans."));
        }

        Section govern = new Section("Voting and decisions");
        if (fixed || accum) {
            govern.add("Group votes are weighted: the admin holds 40% and the other 60% is shared among the group's most active members (up to 5), or all eligible members while the group is young.");
            govern.add("A vote passes with at least 51% of the weight, a minimum number of voters (15% of the group, at least 2), and at least one yes from someone other than the requester.");
            govern.add("Only members who have paid in at least once and have been in the group for 48 hours can vote.");
            govern.add("If the admin loses their wallet, the group can vote to replace the admin (requests expire after 14 days).");
        } else {
            govern.add("Payout order and payments follow the schedule above. The admin manages joining, removals and notices.");
        }
        out.add(govern);

        Section node = new Section("How the network records this");
        node.add("Your money moves on the Harvanna network, not through the admin's account. Every payment is a transaction signed by your own wallet.");
        node.add("You need a wallet on this phone with a backed-up recovery phrase. Nobody, including Harvanna, can recover it for you.");
        if (rotating) {
            node.add("Joining a Circle registers your wallet with the network and locks a bond that covers missed payments (with a penalty).");
        } else {
            node.add("The network records you as a member with your first deposit or savings line.");
        }
        node.add("Signing below records your agreement to these terms with your wallet's signature. It is not a payment.");
        out.add(node);

        Section leaving = new Section("Leaving");
        leaving.add("You can ask to leave at any time. Amounts already contributed follow the rules above and may not be refundable mid-cycle.");
        leaving.add("The admin can remove members who break the rules.");
        out.add(leaving);
        return out;
    }

    public static boolean allowsLoans(Map<String, Object> g) {
        Object v = g.get("allowLoans");
        boolean flag = !(v instanceof Boolean) || (Boolean) v;
        String t = str(g, "groupType", "").toLowerCase(Locale.US);
        return flag && !t.contains("rotating");
    }

    /** Deterministic text the member signs the hash of. */
    public static String canonicalText(String groupId, Map<String, Object> g, String ticker) {
        StringBuilder sb = new StringBuilder("HARVANNA_GROUP_CONTRACT_V1\n").append(groupId).append('\n');
        for (Section s : sections(g, ticker)) {
            sb.append('#').append(s.title).append('\n');
            for (String p : s.points) sb.append('-').append(p).append('\n');
        }
        return sb.toString();
    }

    public static String hash(String groupId, Map<String, Object> g, String ticker) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256")
                .digest(canonicalText(groupId, g, ticker).getBytes(StandardCharsets.UTF_8));
            StringBuilder h = new StringBuilder();
            for (byte b : d) h.append(String.format("%02x", b));
            return h.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    /** The exact string the member's wallet signs. */
    public static String signedMessage(String groupId, String memberAddress, String contractHash, long signedAtMs) {
        return "HARVANNA_GROUP_JOIN_ACK_V1:" + groupId + ":" + memberAddress + ":" + contractHash + ":" + signedAtMs;
    }

    private static String fmt(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.format(Locale.US, "%.2f", d);
    }
    private static String str(Map<String, Object> m, String k, String fb) {
        Object o = m.get(k); return o == null || String.valueOf(o).trim().isEmpty() ? fb : String.valueOf(o).trim();
    }
    private static double num(Map<String, Object> m, String k) {
        Object o = m.get(k);
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return o == null ? 0 : Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0; }
    }
}
