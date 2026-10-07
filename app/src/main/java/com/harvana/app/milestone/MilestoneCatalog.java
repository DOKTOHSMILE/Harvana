package com.harvana.app.milestone;

import com.harvana.app.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every milestone the shelf can show: its name, what earns it, its tiers, its icon and where its count comes from.
 *
 * Sources:
 *  NETWORK  counted by the node from confirmed activity (node Milestones.java; ids and tiers must match it). Public.
 *  LEARN    counted from Learn Hub documents (posts, handshakes, comments, views); nobody can type the number. Public.
 *  PHONE    counted on this phone only (visits, screens). Shown to the owner only; never public, never in the trust score.
 */
public final class MilestoneCatalog {
    private MilestoneCatalog() {}

    public enum Source { NETWORK, LEARN, PHONE }

    public static final class Def {
        public final String id, name, earn, unit, family;
        public final long[] tiers;
        public final int icon;
        public final Source source;
        Def(String id, String name, String earn, String unit, String family, long[] tiers, int icon, Source source) {
            this.id = id; this.name = name; this.earn = earn; this.unit = unit; this.family = family;
            this.tiers = tiers; this.icon = icon; this.source = source;
        }
        public boolean single() { return tiers.length == 1; }
    }

    public static final String SAVING = "Saving", GROUPS = "Groups and leadership", MONEY = "Trading, loans and giving",
            LEARNING = "Learning and writing", STANDING = "Standing", PHONE_FAMILY = "Your habits";

    private static final Map<String, Def> BY_ID = new LinkedHashMap<>();

    private static void add(String id, String name, String earn, String unit, String family, long[] tiers, int icon, Source source) {
        BY_ID.put(id, new Def(id, name, earn, unit, family, tiers, icon, source));
    }

    static {
        Source N = Source.NETWORK, L = Source.LEARN, P = Source.PHONE;
        add("heavy_saver", "Heavy Saver", "Pay into any group: circles, savings lines and goals all count.", "payments", SAVING, new long[]{10, 50, 200}, R.drawable.ms3d_heavy_saver, N);
        add("circle_keeper", "Circle Keeper", "Pay your share in rotating circles.", "circle payments", SAVING, new long[]{10, 50, 150}, R.drawable.ms3d_circle_keeper, N);
        add("steady_saver", "Steady Saver", "Pay into savings lines.", "savings payments", SAVING, new long[]{10, 60, 250}, R.drawable.ms3d_steady_saver, N);
        add("goal_getter", "Goal Getter", "Deposit toward group goals.", "goal deposits", SAVING, new long[]{5, 25, 100}, R.drawable.ms3d_goal_getter, N);
        add("clean_finisher", "Clean Finisher", "Finish rotating circles without a default.", "clean circles", SAVING, new long[]{1, 3, 10}, R.drawable.ms3d_clean_finisher, N);
        add("never_missed", "Never Missed", "Keep a record with no default at all.", "", SAVING, new long[]{1}, R.drawable.ms3d_never_missed, N);
        add("early_payer", "Early Bird", "Pay your circle share early, round after round.", "", SAVING, new long[]{1}, R.drawable.ms3d_early_payer, N);

        add("team_leader", "Team Leader", "Run a circle as its admin all the way to the end.", "circles led", GROUPS, new long[]{1, 3, 10}, R.drawable.ms3d_team_leader, N);
        add("group_founder", "Group Founder", "Start groups and new goals.", "groups started", GROUPS, new long[]{1, 3, 10}, R.drawable.ms3d_group_founder, N);
        add("group_governor", "Group Governor", "Vote on group decisions and approve requests.", "votes", GROUPS, new long[]{5, 25, 100}, R.drawable.ms3d_group_governor, N);
        add("voice_of_the_group", "Voice of the Group", "Bring requests to your groups: payouts, milestones, grants.", "requests", GROUPS, new long[]{1, 10, 40}, R.drawable.ms3d_voice_of_the_group, N);
        add("circle_veteran", "Circle Veteran", "Take part in many rotating circles over time.", "", GROUPS, new long[]{1}, R.drawable.ms3d_circle_veteran, N);
        add("founding_member", "Founding Member", "Be one of the first members of the network.", "", GROUPS, new long[]{1}, R.drawable.ms3d_founding_member, N);

        add("trusted_borrower", "Trusted Borrower", "Repay group loans.", "loans repaid", MONEY, new long[]{1, 5, 20}, R.drawable.ms3d_trusted_borrower, N);
        add("fair_trader", "Fair Trader", "Complete trades on the marketplace.", "trades", MONEY, new long[]{1, 10, 50}, R.drawable.ms3d_fair_trader, N);
        add("generous_giver", "Generous Giver", "Gift Learn Hub creators.", "gifts sent", MONEY, new long[]{1, 10, 50}, R.drawable.ms3d_generous_giver, N);
        add("steady_staker", "Steady Staker", "Stake HARV to help secure the network.", "stakes", MONEY, new long[]{1, 5, 20}, R.drawable.ms3d_steady_staker, N);

        add("good_teacher", "Good Teacher", "Receive gifts for what you teach on Learn Hub.", "gifts received", LEARNING, new long[]{1, 10, 50}, R.drawable.ms3d_good_teacher, N);
        add("storyteller", "Storyteller", "Publish posts on Learn Hub.", "posts", LEARNING, new long[]{1, 10, 50}, R.drawable.ms3d_storyteller, L);
        add("master_writer", "Master Writer", "Write posts that move people: each post with 5 or more handshakes counts.", "engaging posts", LEARNING, new long[]{1, 5, 20}, R.drawable.ms3d_master_writer, L);
        add("crowd_favourite", "Crowd Favourite", "Collect handshakes on your posts.", "handshakes", LEARNING, new long[]{10, 100, 500}, R.drawable.ms3d_crowd_favourite, L);
        add("conversation_starter", "Conversation Starter", "Get people talking in the comments of your posts.", "comments", LEARNING, new long[]{10, 50, 250}, R.drawable.ms3d_conversation_starter, L);
        add("widely_read", "Widely Read", "Have your posts read.", "views", LEARNING, new long[]{100, 1000, 10000}, R.drawable.ms3d_widely_read, L);
        add("avid_learner", "Avid Learner", "Read Learn Hub posts all the way through (verified reads).", "verified reads", LEARNING, new long[]{10, 50, 200}, R.drawable.ms3d_avid_learner, N);

        add("verified_person", "Verified", "Bind your wallet to this phone.", "", STANDING, new long[]{1}, R.drawable.ms3d_verified_person, N);
        add("trusted_by_peers", "Trusted by Peers", "Be vouched for by members in good standing.", "vouches", STANDING, new long[]{1, 3, 5}, R.drawable.ms3d_trusted_by_peers, N);
        add("connector", "Connector", "Invite people who join and keep a clean record.", "referrals", STANDING, new long[]{1, 5, 20}, R.drawable.ms3d_connector, N);
        add("daily_habit", "Daily Habit", "Be active on many days in the last 30.", "active days", STANDING, new long[]{7, 15, 25}, R.drawable.ms3d_daily_habit, N);
        add("veteran", "Veteran", "Keep going: days since your first confirmed action.", "days", STANDING, new long[]{30, 180, 365}, R.drawable.ms3d_veteran, N);
        add("comeback", "Comeback", "Make good a default and rebuild a clean record.", "", STANDING, new long[]{1}, R.drawable.ms3d_comeback, N);

        add("regular_visitor", "Regular", "Open the app often (counted on this phone).", "visits", PHONE_FAMILY, new long[]{10, 50, 200}, R.drawable.ms3d_regular_visitor, P);
        add("explorer", "Explorer", "Find your way around: different screens opened (counted on this phone).", "screens", PHONE_FAMILY, new long[]{5, 12, 20}, R.drawable.ms3d_explorer, P);
    }

    public static Def get(String id) { return BY_ID.get(id); }

    public static List<Def> all() { return Collections.unmodifiableList(new ArrayList<>(BY_ID.values())); }

    public static List<String> families() {
        return java.util.Arrays.asList(SAVING, GROUPS, MONEY, LEARNING, STANDING, PHONE_FAMILY);
    }

    /** Tier reached for a count: 0 = not yet, then 1 bronze, 2 silver, 3 gold (single milestones use 1 = earned). */
    public static int tierFor(Def d, long count) {
        int t = 0;
        for (long threshold : d.tiers) if (count >= threshold) t++;
        return t;
    }

    public static String tierName(Def d, int tier) {
        if (tier <= 0) return "Not yet";
        if (d.single()) return "Earned";
        return tier == 1 ? "Bronze" : tier == 2 ? "Silver" : "Gold";
    }

    /** The light card behind a badge: peach for bronze, pale blue-grey for silver, soft gold, mint for one-off badges, grey while locked. */
    public static int tierBackground(Def d, int tier) {
        if (tier <= 0) return 0xFFEEF1F0;
        if (d.single()) return 0xFFDDF3EE;
        return tier == 1 ? 0xFFF8E7D8 : tier == 2 ? 0xFFE8EEF2 : 0xFFFFF1C9;
    }

    /** Text colour for the tier name on a light badge card (dark enough to read on it). */
    public static int tierInk(Def d, int tier) {
        if (tier <= 0) return 0xFF7A8886;
        if (d.single()) return 0xFF0B5F57;
        return tier == 1 ? 0xFF9A5B2B : tier == 2 ? 0xFF55636B : 0xFF946412;
    }

    /** Ink for names and body text on a light badge card. */
    public static final int CARD_TEXT = 0xFF17302D, CARD_MUTED = 0xFF5B6E6B;

    public static int tierColor(Def d, int tier) {
        if (tier <= 0) return 0xFF6F8582;
        if (d.single()) return 0xFF7FD1C3;
        return tier == 1 ? 0xFFC98A55 : tier == 2 ? 0xFFC3CED3 : 0xFFF2C46D;
    }
}
