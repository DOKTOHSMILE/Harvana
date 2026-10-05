package com.harvana.app.engine;

/**
 * What a fixed-goal group says once its goal is reached. The wording depends on what has actually happened to the money:
 * a goal that is reached has not necessarily paid out. Only a group the network marks COMPLETED has sent the pot; under the
 * "all at once" rule a reached goal is being sent; under every other rule the money stays in the group wallet until the admin
 * asks for a payout and the members vote. Pure Java, so FixedGoalTextTest runs on the JVM.
 */
public final class FixedGoalText {
    private FixedGoalText() {}

    public enum Stage { OPEN, SENDING, HELD, PAID }

    public static Stage stage(String rule, String state, boolean goalMet) {
        if ("COMPLETED".equalsIgnoreCase(state)) return Stage.PAID;
        if (!goalMet) return Stage.OPEN;
        return "ALL_AT_ONCE_AT_GOAL".equals(rule) ? Stage.SENDING : Stage.HELD;
    }

    /** One or two sentences for the goal's board, overview and payments tab. {@code money} is e.g. "40 HARV"; {@code who} the beneficiary. */
    public static String sentence(String rule, String state, boolean goalMet, String money, String who) {
        switch (stage(rule, state, goalMet)) {
            case PAID:
                return "Goal reached. " + money + " was sent to " + who + ". That wallet was chosen when the group was created.";
            case SENDING:
                return "Goal reached. The network is sending " + money + " to " + who + ". Deposits are closed.";
            case HELD:
                return "Goal reached. Deposits are closed. " + money + " is held by the group wallet and has not been paid out. " + howItLeaves(rule);
            default:
                return "Deposit any day until the goal is reached. A quiet day is not a missed payment.";
        }
    }

    public static String howItLeaves(String rule) {
        if ("ADMIN_TRIGGERED".equals(rule)) return "The admin asks for a payout and the members vote.";
        if ("MILESTONE_BASED".equals(rule)) return "It is released in milestones, each voted by the group.";
        if ("EMERGENCY_GRANT".equals(rule)) return "A member asks for a grant and the group votes.";
        return "It leaves when the group decides.";
    }

    /** True while money can still be asked for: the group is not finished and part of the pot has not been paid out. */
    public static boolean fundsLeft(String state, long raised, long disbursed) {
        return !"COMPLETED".equalsIgnoreCase(state) && raised - disbursed > 0;
    }
}
