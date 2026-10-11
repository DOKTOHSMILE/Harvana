package com.harvana.app.tx;

import com.harvana.app.R;

/**
 * Names and icons a transaction by what it is for, read from its memo, so the history says "Staked", "Group
 * created", "Contribution" and so on instead of calling everything a transfer. Null means a plain transfer.
 */
public final class TxKind {
    public final String title;
    public final String detail;
    public final int icon;

    private TxKind(String title, String detail, int icon) {
        this.title = title;
        this.detail = detail;
        this.icon = icon;
    }

    /**
     * The "For" line of a transaction in plain words: the kind's title and a short reference instead of the raw memo
     * (e.g. "Savings contribution · ref 5b2cde66" instead of "ACCUM_LINE_CONTRIBUTION:ACCUM-...:LINE-5b2cde66732bb197").
     * A memo that is plain text (a note someone typed) is shown as it is.
     */
    public static String describe(String memo, boolean incoming) {
        if (memo == null || memo.trim().isEmpty()) return "";
        String m = memo.trim();
        if (!m.matches("^[A-Z][A-Z0-9_]+:.*")) return m;                 // a typed note
        TxKind kind = of(m, incoming);
        String last = m.substring(m.lastIndexOf(':') + 1);
        int dash = last.lastIndexOf('-');
        if (dash >= 0) last = last.substring(dash + 1);
        String ref = last.length() > 8 ? last.substring(0, 8) : last;
        return kind.title + (ref.isEmpty() ? "" : " \u00b7 ref " + ref);
    }

    public static TxKind of(String memo, boolean incoming) {
        if (memo == null) return null;
        String m = memo.trim();
        if (m.isEmpty()) return null;

        if (m.startsWith("ACTION_FEE:")) {
            String[] parts = m.split(":");
            String what = parts.length > 1 ? parts[1] : "";
            String label = what.startsWith("STAKING_CLAIM") ? "claiming staking rewards" : what.startsWith("STAKING_UNSTAKE") ? "unstaking"
                    : what.startsWith("ACCUM_CLAIM") ? "a savings payout" : what.startsWith("CIRCLE_WITHDRAW") ? "withdrawing a bond"
                    : what.startsWith("FIXED_DISBURSE") ? "a goal payout" : what.startsWith("FIXED_") ? "a goal group action" : what.startsWith("GENLOAN") ? "a loan request" : what.startsWith("P2P_CANCEL") ? "cancelling a listing" : "an action";
            return new TxKind("Network fee", "For " + label, R.drawable.ic_tx_fee);
        }
        // Stable-asset bridges (hUSD, hEUR, hNGN, …): the reserve token in from another network, or out to one.
        int us = m.indexOf('_');
        String stable = us == 4 ? m.substring(0, 4) : "";
        if (com.harvana.app.husd.Stable.isStable(stable) && (m.startsWith(stable + "_MINT:") || m.startsWith(stable + "_WITHDRAW:"))) {
            String[] parts = m.split(":");
            String chain = com.harvana.app.husd.HusdActivity.chainName(parts.length > 1 ? parts[1] : "");
            String sym = com.harvana.app.husd.Stable.symbol(stable), reserve = com.harvana.app.husd.Stable.reserve(stable, null);
            int icon = "HUSD".equals(stable) ? R.drawable.ic_coin_husd : R.drawable.ic_coin_stable;
            return m.startsWith(stable + "_MINT:")
                    ? new TxKind("Deposited " + sym, reserve + " from " + chain, icon)
                    : new TxKind("Withdrew to " + reserve, sym + " to " + chain, icon);
        }
        if (m.startsWith("STAKE_LOCK:")) {
            String days = m.substring("STAKE_LOCK:".length());
            return new TxKind("Staked HARV", "0".equals(days) ? "Flexible stake" : days + "-day lock", R.drawable.ic_tx_stake);
        }
        if (m.startsWith("STAKING_PAYOUT:")) {
            return new TxKind(incoming ? "Staking payout" : "Staking payout sent", "Rewards or unstaked principal", R.drawable.ic_tx_stake_reward);
        }
        if (m.contains("CREATION_FEE:")) return new TxKind("Group created", "Creation fee", R.drawable.ic_tx_group_create);
        if (m.startsWith("CIRCLE_BOND_REFUND") || m.startsWith("CIRCLE_BOND_LOCK:"))
            return new TxKind(m.startsWith("CIRCLE_BOND_LOCK:") ? "Circle bond locked" : "Circle bond returned", "Rotating group", R.drawable.ic_tx_bond);
        if (m.startsWith("CIRCLE_CURE:")) return new TxKind("Default cured", "Rotating group", R.drawable.ic_tx_bond);
        if (m.startsWith("CIRCLE_CONTRIBUTION")) return new TxKind(m.contains("REFUND") ? "Contribution refunded" : "Group contribution", "Rotating group", R.drawable.ic_tx_contribution);
        if (m.startsWith("ACCUM_LINE")) return new TxKind("Savings contribution", "Savings group", R.drawable.ic_tx_contribution);
        if (m.startsWith("FIXED_DEPOSIT:")) return new TxKind("Goal deposit", "Goal group", R.drawable.ic_tx_contribution);
        if (m.startsWith("Group contribution")) return new TxKind("Group contribution", m, R.drawable.ic_tx_contribution);
        if (m.startsWith("GENLOAN_REPAY:") || m.startsWith("Loan repayment")) return new TxKind("Loan repayment", "Group loan", R.drawable.ic_tx_loan);
        if (m.startsWith("Loan disbursement") || m.startsWith("GENLOAN")) return new TxKind(incoming ? "Loan received" : "Loan paid out", "Group loan", R.drawable.ic_tx_loan);
        if (m.startsWith("Fixed month close")) return new TxKind(incoming ? "Group payout" : "Group payout sent", "Goal group", R.drawable.ic_tx_group);
        if (m.startsWith("LEARN_GIFT:")) return new TxKind(incoming ? "Gift received" : "Gift sent", "Learn Hub", R.drawable.ic_tx_gift);
        if (m.startsWith("P2P_")) return new TxKind("P2P escrow", "Peer-to-peer trade", R.drawable.ic_tx_p2p);
        if (m.startsWith("CIRCLE_") || m.startsWith("ACCUM_") || m.startsWith("FIXED_"))
            return new TxKind(incoming ? "Group payout" : "Group transfer", "Group wallet", R.drawable.ic_tx_group);
        return null;
    }
}
