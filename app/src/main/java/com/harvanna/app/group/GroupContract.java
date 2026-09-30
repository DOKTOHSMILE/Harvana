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
        double amount = num(g, "contributionAmount");
        String freq = str(g, "frequency", "").toLowerCase(Locale.US);
        long slots = (long) num(g, "totalSlots");
        boolean loans = allowsLoans(g);

        // 1. In short: the four things anyone must know.
        Section brief = new Section("In short");
        if (rotating) {
            brief.add("What it is: a rotating savings circle. Everyone pays the same amount each round. Each round, one member takes home the whole pot.");
            if (amount > 0) brief.add("You pay: " + fmt(amount) + " " + ticker + (freq.isEmpty() ? "" : " every " + everyWord(freq)) + ".");
            if (amount > 0 && slots > 0) brief.add("You get: one payout of about " + fmt(amount * slots) + " " + ticker + " when it is your turn.");
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

        // 2. A worked example with the group's real numbers.
        Section ex = exampleSection(rotating, fixed, accum, amount, slots, freq, ticker, g);
        if (ex != null) out.add(ex);

        // 3. About the group and the admin.
        Section about = new Section("About " + name);
        about.add("Type: " + (type.isEmpty() ? "Savings group" : type) + ".");
        String desc = str(g, "groupDescription", str(g, "description", ""));
        if (!desc.isEmpty()) about.add(desc);
        if (slots > 0) about.add("Size: up to " + slots + " members.");
        if (GroupKinds.isOnChain(g)) {
            about.add("The admin is the person who created the group. Joining is open: anyone can lock their bond and take a place until the group is full. The network, not the admin, holds the money and sets the payout order.");
        } else {
            about.add("The admin is the person who created the group. The admin sets the rules below, accepts or removes members, and looks after the group wallet.");
        }
        out.add(about);

        // 4. Money rules.
        Section money = new Section("The money rules");
        if (amount > 0) {
            money.add("Contribution: " + fmt(amount) + " " + ticker + (freq.isEmpty() ? "" : " (" + freq + ")") + ".");
        }
        if (rotating) {
            money.add("Each member has a slot number. Your slot decides which round you receive the pot.");
            money.add("If someone pays late, the member whose turn it is has to wait. So please pay on time.");
            double grace = num(g, "gracePeriodDays");
            if (grace > 0) money.add("Grace period: you have " + fmt(grace) + " extra day(s) after the due date before your payment counts as late.");
            String cons = str(g, "defaultConsequence", "");
            if (!cons.isEmpty()) money.add("If you fail to pay: " + cons + ".");
            double comm = num(g, "commissionPercent");
            if (comm > 0) money.add("Commission: " + fmt(comm) + "% is taken from each payout.");
        } else if (fixed) {
            String rule = str(g, "disbursementRule", "");
            if (!rule.isEmpty()) money.add("How money is paid out: " + rule + ".");
            money.add("You cannot withdraw your deposit by yourself. Payouts, milestones and emergency grants each need a group vote.");
        } else if (accum) {
            String w = str(g, "withdrawalRule", "");
            money.add("You build your own savings line inside the group and can add to it.");
            if (!w.isEmpty()) money.add("Withdrawal rule: " + w + ".");
        }
        out.add(money);

        // 5. Loans.
        if (loans) {
            Section ls = new Section("Loans");
            double rate = num(g, "loanInterestRate");
            String period = str(g, "loanInterestPeriod", "MONTHLY").toLowerCase(Locale.US);
            ls.add("Members of this group can borrow from the group's money.");
            ls.add("Interest: " + fmt(rate) + "% per " + period.replace("ly", "") + ". Example: borrow 100 " + ticker + " and the interest for one "
                + period.replace("ly", "") + " is " + fmt(rate) + " " + ticker + ".");
            ls.add("You must pay a loan back in full. Unpaid loans lower your trust score and can be taken from your savings in the group.");
            out.add(ls);
        } else {
            out.add(new Section("Loans").add("This group does not offer loans."));
        }

        // 6. Voting.
        Section govern = new Section("How decisions are made");
        if (fixed || accum) {
            govern.add("Big decisions are voted on. The admin's vote counts 40%. The other 60% is shared among the group's most active members (up to 5), or among all eligible members while the group is new.");
            govern.add("A vote passes when at least 51% of the total vote weight says yes, enough people voted (at least 2, or 15% of the group), and someone other than the person asking also said yes.");
            govern.add("You can vote once you have paid at least once and been in the group for 48 hours.");
            govern.add("If the admin loses access to their wallet, the group can vote to choose a new admin. That request expires after 14 days.");
        } else {
            govern.add("Payments and payouts follow the schedule above. The admin manages who joins, removals and notices.");
        }
        out.add(govern);

        // 7. How the network records it.
        Section node = new Section("How Harvanna keeps it safe");
        node.add("Your payments are transactions on the Harvanna network. You sign each one with your own wallet, so nobody can move your money without you.");
        node.add("You need a wallet on this phone and you must have written down its 12 recovery words. Nobody, not even Harvanna, can recover them for you.");
        if (rotating) {
            node.add("Joining a circle registers your wallet with the network and locks a bond that covers missed payments (with a penalty).");
        } else {
            node.add("The network records you as a member when you make your first payment or start your savings line.");
        }
        node.add("When you sign below, your wallet records that you read and agreed to this page. It is not a payment. No money moves.");
        out.add(node);

        Section chat = new Section("Group chat");
        chat.add("If the admin adds a group chat, it is on another app such as WhatsApp or Telegram. Harvanna does not run it.");
        chat.add("Never share your recovery words, your passphrase or your PIN in any chat. Payments happen only in the Harvanna app.");
        out.add(chat);

        // 8. Leaving.
        Section leaving = new Section("If you want to leave");
        leaving.add("You can ask to leave at any time. Money you already paid follows the rules above, and may not come back in the middle of a cycle.");
        leaving.add("The admin can remove a member who breaks the rules.");
        out.add(leaving);

        // 9. Words explained.
        Section words = new Section("Words explained");
        if (rotating) {
            words.add("Round: one payment period. Everyone pays once per round.");
            words.add("Slot: your place in line to receive the pot.");
            words.add("Pot: all the money paid in during one round.");
            words.add("Bond: a deposit locked when you join. It is returned if you pay on time.");
            words.add("Grace period: extra days allowed before a payment counts as late.");
        }
        if (fixed) {
            words.add("Goal: the total amount the group wants to save.");
            words.add("Milestone: a step toward the goal that needs money released.");
            words.add("Emergency grant: help for a member in urgent need, paid only if the group votes yes.");
        }
        if (accum) {
            words.add("Savings line: your own saving plan inside the group.");
            words.add("Interest: extra money you earn for saving.");
        }
        words.add("Vote weight: how much your vote counts.");
        words.add("Trust score: a number showing how reliably you pay. It grows when you pay on time.");
        words.add("Wallet: your account on the network. Only you can sign for it.");
        out.add(words);
        return out;
    }

    private static Section exampleSection(boolean rotating, boolean fixed, boolean accum, double amount, long slots,
                                          String freq, String ticker, Map<String, Object> g) {
        if (amount <= 0) return null;
        Section ex = new Section("A simple example");
        String per = freq.isEmpty() ? "each round" : "every " + everyWord(freq);
        if (rotating && slots > 1) {
            double pot = amount * slots;
            ex.add("Say " + slots + " people join and each pays " + fmt(amount) + " " + ticker + " " + per + ".");
            ex.add("Each round the pot is " + fmt(pot) + " " + ticker + " (" + slots + " x " + fmt(amount) + ").");
            ex.add("One person takes the whole pot each round, in slot order. After " + slots + " rounds everyone has received the pot once.");
            ex.add("Over the whole circle you pay " + fmt(pot) + " " + ticker + " in total and you receive " + fmt(pot) + " " + ticker + " once, before any commission. It is a way to get a large sum at once, not a way to earn extra.");
            return ex;
        }
        if (fixed) {
            double goal = num(g, "targetGoalAmount");
            ex.add("If you pay " + fmt(amount) + " " + ticker + " " + per + ", it goes into the shared pool.");
            if (goal > 0) {
                long times = (long) Math.ceil(goal / amount);
                ex.add("If you were the only one paying, reaching " + fmt(goal) + " " + ticker + " would take " + times + " payments. With more members it is faster.");
            }
            ex.add("Money only comes out by the group's rule and vote, never because one person asks.");
            return ex;
        }
        if (accum) {
            double rate = num(g, "loanInterestRate");
            ex.add("If you save " + fmt(amount) + " " + ticker + " " + per + ", that money sits in your own line.");
            ex.add("At the end of your saving period you can take your savings back" + (rate > 0 ? ", with interest as set by the group" : "") + ".");
            return ex;
        }
        return null;
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
