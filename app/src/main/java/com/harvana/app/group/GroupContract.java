package com.harvana.app.group;

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
        double amount = num(g, "contributionAmount");
        String freq = str(g, "frequency", "").toLowerCase(Locale.US);
        long slots = (long) num(g, "totalSlots");
        boolean loans = allowsLoans(g);

        // 1. In short: the four things anyone must know.
        Section brief = new Section("In short");
        if (rotating) {
            brief.add("What it is: a rotating savings circle. Everyone pays the same amount each round. Each round, one member takes home everything the others paid that round. You do not pay in the round it is your turn.");
            if (amount > 0) brief.add("You pay: " + fmt(amount) + " " + ticker + (freq.isEmpty() ? "" : " every " + everyWord(freq)) + ".");
            if (amount > 0 && slots > 0) brief.add("You get: one payout of about " + fmt(amount * (slots - 1)) + " " + ticker + " when it is your turn.");
            brief.add("The risk: if you stop paying, the group is hurt and you can lose part of your bond (a deposit you lock when you join).");
        } else if (fixed) {
            brief.add("What it is: the group saves together toward one goal, such as a project or an emergency fund.");
            if (amount > 0) brief.add("You pay: " + fmt(amount) + " " + ticker + (freq.isEmpty() ? "" : " " + freq) + ".");
            double goal = num(g, "targetGoalAmount");
            if (goal > 0) brief.add("The goal: " + fmt(goal) + " " + ticker + " in total.");
            brief.add("The risk: you cannot take your money out alone. The group votes before any money leaves.");
        } else if (accum) {
            brief.add("What it is: you save at your own pace inside the group and earn interest that the group sets.");
            if (amount > 0) brief.add("You pay: at least " + fmt(amount) + " " + ticker + (freq.isEmpty() ? "" : " " + freq) + ".");
            brief.add("You get: your savings back, plus interest, when your saving period ends.");
            brief.add("The risk: taking money out early may not be allowed. See the withdrawal rule below.");
        } else {
            brief.add("What it is: a savings group run by its admin.");
        }
        brief.add("Your money stays in your control: every payment is signed by your own wallet. The admin cannot take money from your wallet.");
        out.add(brief);

        // 1b. A goal group: what the goal is, who set it, and exactly where the money goes.
        if (fixed) out.add(goalSection(g, name, ticker));
        else if (rotating) out.add(circleSection(g, name, ticker));
        else if (accum) out.add(savingsSection(g, name, ticker));

        // 2. A worked example with the group's real numbers.
        Section ex = exampleSection(rotating, fixed, accum, amount, slots, freq, ticker, g);
        if (ex != null) out.add(ex);

        // 3. About the group and the admin.
        Section about = new Section("About " + name);
        about.add("Type: " + (type.isEmpty() ? "Savings group" : type) + ".");
        String desc = str(g, "groupDescription", str(g, "description", ""));
        if (!desc.isEmpty()) about.add(desc);
        if (slots > 0) about.add("Size: up to " + slots + " members.");
        // For a group on the network the section above already says who started it and where the money sits; do not say it twice.
        if (!GroupKinds.isOnChain(g)) about.add("The admin is the person who created the group. The admin sets the rules below, accepts or removes members, and looks after the group wallet.");
        out.add(about);

        // 4. Money rules: only rules that no earlier page states (contribution, slot, late payment and the payout rule are already covered).
        Section money = new Section("The money rules");
        if (rotating) {
            double grace = num(g, "gracePeriodDays");
            if (grace > 0) money.add("Grace period: you have " + fmt(grace) + " extra day(s) after the due date before your payment counts as late.");
            String cons = str(g, "defaultConsequence", "");
            if (!cons.isEmpty()) money.add("If you fail to pay: " + cons + ".");
            double comm = num(g, "commissionPercent");
            if (comm > 0) money.add("Commission: " + fmt(comm) + "% is taken from each payout.");
        }
        if (!money.points.isEmpty()) out.add(money);

        // 5. Loans.
        if (loans) {
            Section ls = new Section("Loans");
            double rate = LoanInterest.dailyPercent(num(g, "loanInterestRate"), str(g, "loanInterestPeriod", ""), GroupKinds.isOnChain(g));
            ls.add("Members of this group can borrow from the group's money, for 1 to 30 days.");
            ls.add("Interest is charged per day: " + fmt4(rate) + "% of the amount borrowed for each day the loan is held, counted in whole and part days. It is not a monthly charge.");
            ls.add("You pay only for the days you hold the loan. Example: borrow 100 " + ticker + " for 30 days and repay on day 15, and you pay interest for 15 days, "
                + fmt4(LoanInterest.interest(100, rate, 15)) + " " + ticker + ", not for 30. If you keep it the full 30 days you pay " + fmt4(LoanInterest.interest(100, rate, 30)) + " " + ticker + ".");
            ls.add("You must pay a loan back in full. Unpaid loans lower your trust score and can be taken from your savings in the group.");
            out.add(ls);
        } else {
            out.add(new Section("Loans").add("This group does not offer loans."));
        }

        // 6. Voting.
        Section govern = new Section("How decisions are made");
        if (fixed || accum) {
            govern.add("Big decisions are voted on. The admin's vote counts 40%. If the group has a treasurer, those 40% are shared so that the admin counts most, then the treasurer, then each member (for example about 23% and 17% in a group with five voting members). The other 60% is shared among the group's most active members: 20% of the members, at least 5 and at most 15, so more members vote as the group grows. While the group is new, all eligible members vote.");
            govern.add("A vote passes when at least 51% of the total vote weight says yes, enough people voted (at least 2, or 15% of the group), and someone other than the person asking also said yes.");
            govern.add("You can vote once you have paid at least once and been in the group for 48 hours.");
            govern.add("The admin seat is not a vote. Only the current admin can hand it to another member, from the group settings. Members cannot vote an admin out.");
        } else {
            govern.add("Payments and payouts follow the schedule above. The admin manages who joins, removals and notices.");
        }
        out.add(govern);

        // 7. How the network records it.
        Section node = new Section("How Harvana keeps it safe");
        node.add("Your payments are transactions on the Harvana network.");
        node.add("You need a wallet on this phone and you must have written down its 12 recovery words. Nobody, not even Harvana, can recover them for you.");
        if (!rotating) node.add("The network records you as a member when you make your first payment or start your savings line.");
        node.add("When you sign below, your wallet records that you read and agreed to this page. It is not a payment. No money moves.");
        out.add(node);

        Section chat = new Section("Group chat");
        chat.add("If the admin adds a group chat, it is on another app such as WhatsApp or Telegram. Harvana does not run it.");
        chat.add("Never share your recovery words, your passphrase or your PIN in any chat. Payments happen only in the Harvana app.");
        out.add(chat);

        // 8. Leaving.
        Section leaving = new Section("If you want to leave");
        leaving.add("You can ask to leave at any time. Money you already paid follows the rules above, and may not come back in the middle of a cycle.");
        leaving.add("The admin can remove a member who breaks the rules.");
        out.add(leaving);

        // 9. Words explained.
        // Only words no earlier page explains.
        Section words = new Section("Words explained");
        if (rotating) {
            words.add("Round: one payment period. Everyone pays once per round.");
            words.add("Slot: your place in line to receive the pot.");
            words.add("Pot: all the money the other members pay in during one round.");
            words.add("Grace period: extra days allowed before a payment counts as late.");
        }
        if (fixed) {
            words.add("Milestone: a step toward the goal that needs money released.");
            words.add("Emergency grant: help for a member in urgent need, paid only if the group votes yes.");
        }
        words.add("Trust score: a number showing how reliably you pay. It grows when you pay on time.");
        words.add("Wallet: your account on the network. Only you can sign for it.");
        out.add(words);
        return out;
    }

    /**
     * The heart of a goal group, spelled out: the goal, the person who set it, the group wallet that is made when the group is created,
     * and where the money goes when the goal is reached. The money never passes through a Harvana "system wallet": members pay into the
     * group wallet, and the network pays out of that wallet to the beneficiary.
     */
    static Section goalSection(Map<String, Object> g, String name, String ticker) {
        if (!GroupKinds.isOnChain(g)) return keptInAppSection("The goal and where the money goes", "members gather money together for one goal");
        Section s = new Section("The goal and where the money goes");
        double goal = num(g, "targetGoalAmount");
        String desc = str(g, "groupDescription", str(g, "description", ""));
        s.add("The purpose of this group: members gather money together for one goal. The goal is \u201c" + name + "\u201d"
            + (goal > 0 ? ", a total of " + fmt(goal) + " " + ticker : "") + "." + (desc.isEmpty() ? "" : " " + desc));
        s.add("Who set the goal: the admin, the person who created this group. The admin chose the goal, the target, who receives the money and how it is paid out.");
        String wallet = str(g, "groupWalletAddress", "");
        s.add("The group wallet: when the group was created, the network made a wallet just for this goal" + (wallet.isEmpty() ? "" : " (" + shortAddr(wallet) + ")")
            + ". It has no private key, so nobody, not the admin and not Harvana, can spend from it by themselves. It only pays out by the rules of this page.");
        s.add("Where your deposit goes: every deposit you make goes from your wallet straight into the group wallet. Nothing is held in a Harvana company wallet on the way.");
        String ben = str(g, "beneficiaryAddress", "");
        String rule = str(g, "disbursementRule", "");
        boolean grants = rule.toLowerCase(Locale.US).contains("emergency");
        if (grants) {
            s.add("Who receives the money: no one is chosen in advance. When a member asks for an emergency grant and the group votes yes, that member is paid from the group wallet.");
        } else {
            s.add("Who receives the money when the goal is reached: " + (ben.isEmpty()
                ? "the wallet chosen by the admin when the group was created (it is shown on the group page)"
                : shortAddr(ben) + ", chosen by the admin when the group was created") + ". The money is paid from the group wallet to that wallet.");
        }
        String r = rule.toLowerCase(Locale.US);
        if (r.contains("all at once")) s.add("When it is paid: as soon as the goal is reached, the network sends the whole pot automatically. No vote is needed, and deposits stop.");
        else if (r.contains("admin")) s.add("When it is paid: reaching the goal does not send the money. The admin asks for a payout, the group votes, and only if the vote passes is the money sent.");
        else if (r.contains("milestone")) s.add("When it is paid: in parts. Each part is asked for, voted on by the group, and only then sent.");
        else if (grants) s.add("When it is paid: only after a member asks, the group votes yes, and the network sends it.");
        else s.add("When it is paid: by the payout rule the admin chose, shown below.");
        s.add("If the goal is never reached: the money stays in the group wallet and leaves only by a group vote.");
        s.add("A treasurer is optional. The group may have none, in which case the admin handles payout requests. If there is one, the admin named them, or the group voted them in with the admin's yes. A treasurer can ask for a payout or a milestone release, but it is paid only after the group votes and the admin votes yes too. The treasurer can be removed by the admin, or by a group vote.");
        return s;
    }

    /** A rotating circle: what it is for, who started it, and exactly where each payment and each payout goes. */
    static Section circleSection(Map<String, Object> g, String name, String ticker) {
        if (!GroupKinds.isOnChain(g)) return keptInAppSection("The circle and where the money goes", "members take turns receiving a lump sum that all the others pay in");
        Section s = new Section("The circle and where the money goes");
        long slots = (long) num(g, "totalSlots");
        double amount = num(g, "contributionAmount");
        s.add("The purpose of this circle: members take turns receiving a lump sum that all the others pay in. It is a way to get a large amount at once, not a way to earn extra.");
        s.add("Who started it: the admin, the person who created this group. The admin set the contribution" + (amount > 0 ? " (" + fmt(amount) + " " + ticker + ")" : "")
            + ", the number of places" + (slots > 0 ? " (" + slots + ")" : "") + " and how often everyone pays.");
        String wallet = str(g, "groupWalletAddress", "");
        s.add("The circle wallet: when the group was created, the network made a wallet just for this circle" + (wallet.isEmpty() ? "" : " (" + shortAddr(wallet) + ")")
            + ". It has no private key, so nobody, not the admin and not Harvana, can spend from it by themselves.");
        s.add("Where your payments go: each round you pay from your own wallet into the circle wallet. Nothing is held in a Harvana company wallet on the way.");
        s.add("Where your bond goes: the bond you lock when you join also sits in the circle wallet. It comes back to you if you pay on time. A missed payment is covered from it, with a penalty.");
        s.add("Who receives the money: each round, the member whose turn it is receives the pot, paid from the circle wallet to their own wallet. The network sets the order when the circle fills: members who have already finished a circle take the first 30% of places, and everyone else follows in the order they joined.");
        s.add("If someone does not pay: the round is covered from their bond first, so the member whose turn it is is not left short. Please pay on time.");
        return s;
    }

    /** An accumulative savings group: what it is for, who started it, where a member's savings sit and how they come back. */
    static Section savingsSection(Map<String, Object> g, String name, String ticker) {
        if (!GroupKinds.isOnChain(g)) return keptInAppSection("The savings group and where the money goes", "members save at their own pace inside one group");
        Section s = new Section("The savings group and where the money goes");
        s.add("The purpose of this group: members save at their own pace inside one group, each in their own savings line, and the group can lend from the pooled money for interest.");
        s.add("Who started it: the admin, the person who created this group. The admin set the loan interest and the withdrawal rule" + ".");
        String wallet = str(g, "groupWalletAddress", "");
        s.add("The group wallet: when the group was created, the network made a wallet for this group" + (wallet.isEmpty() ? "" : " (" + shortAddr(wallet) + ")")
            + ". It has no private key, so nobody, not the admin and not Harvana, can spend from it by themselves.");
        s.add("Where your savings go: every amount you save goes from your own wallet into the group wallet and is recorded on your own savings line. Nothing is held in a Harvana company wallet on the way.");
        s.add("Whose money it is: what is in your line is yours. The network keeps a separate record for each member, so the admin cannot take your line.");
        String w = str(g, "withdrawalRule", "");
        s.add("A treasurer is optional. The group may have none, in which case the admin approves withdrawal requests. If there is one, the admin named them, or the group voted them in with the admin's yes. A treasurer can approve a member's withdrawal request, but the money still goes only to the member it belongs to. The treasurer can be removed by the admin, or by a group vote.");
        s.add("How your money comes back: it is paid from the group wallet to your own wallet when you claim it" + (w.isEmpty() ? "" : ", under the group's withdrawal rule (" + w + ")")
            + ". The admin has to approve a withdrawal request, and an unpaid loan can be taken from your savings.");
        return s;
    }

    /** An older group that exists only in the app, not on the network: no network wallet, and the admin can still change its settings. */
    private static Section keptInAppSection(String title, String purpose) {
        Section s = new Section(title);
        s.add("The purpose of this group: " + purpose + ".");
        s.add("Who started it: the admin, the person who created this group.");
        s.add("This group is kept in the app only. It is not held on the Harvana network, so the network does not make a wallet for it or enforce its payouts. The admin looks after the group wallet and the records.");
        s.add("The admin can change this group's settings. If the rules on this page change, read them again before you pay.");
        return s;
    }

    private static String shortAddr(String a) { return a.length() > 14 ? a.substring(0, 8) + "\u2026" + a.substring(a.length() - 4) : a; }

    private static Section exampleSection(boolean rotating, boolean fixed, boolean accum, double amount, long slots,
                                          String freq, String ticker, Map<String, Object> g) {
        if (amount <= 0) return null;
        Section ex = new Section("A simple example");
        String per = freq.isEmpty() ? "each round" : "every " + everyWord(freq);
        if (rotating && slots > 1) {
            double pot = amount * (slots - 1);
            ex.add("With " + slots + " people each paying " + fmt(amount) + " " + ticker + " " + per + ", the pot each round is " + fmt(pot) + " " + ticker
                + " (" + (slots - 1) + " x " + fmt(amount) + "), because the person whose turn it is does not pay.");
            ex.add("After " + slots + " rounds everyone has received the pot once. Over the whole circle you pay " + fmt(pot) + " " + ticker + " (every round except your own) and receive "
                + fmt(pot) + " " + ticker + " once, before any commission.");
            return ex;
        }
        if (fixed) {
            double goal = num(g, "targetGoalAmount");
            if (goal <= 0) return null;
            long times = (long) Math.ceil(goal / amount);
            ex.add("Paying " + fmt(amount) + " " + ticker + " " + per + ", one person alone would reach the " + fmt(goal) + " " + ticker + " goal in " + times + " payments. With more members it is faster.");
            return ex;
        }
        return null;                       // a savings group's example would only repeat the pages before it
    }

    private static String everyWord(String freq) {
        switch (freq) {
            case "daily": return "day";
            case "weekly": return "week";
            case "monthly": return "month";
            case "quarterly": return "3 months";
            case "annually": return "year";
            default: return freq;
        }
    }

    public static boolean allowsLoans(Map<String, Object> g) {
        Object v = g.get("allowLoans");
        boolean flag = !(v instanceof Boolean) || (Boolean) v;
        String t = str(g, "groupType", "").toLowerCase(Locale.US);
        return flag && !t.contains("rotating");
    }

    /** Deterministic text the member signs the hash of. */
    public static String canonicalText(String groupId, Map<String, Object> g, String ticker) {
        StringBuilder sb = new StringBuilder("HARVANA_GROUP_CONTRACT_V1\n").append(groupId).append('\n');
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
        return "HARVANA_GROUP_JOIN_ACK_V1:" + groupId + ":" + memberAddress + ":" + contractHash + ":" + signedAtMs;
    }

    private static String fmt4(double d) {
        String s = String.format(Locale.US, "%.4f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
        return s.isEmpty() ? "0" : s;
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
