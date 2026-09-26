package com.ajoo.app.group;

import android.view.View;
import android.widget.TextView;

import java.util.Locale;
import java.util.Map;

/**
 * Shows/hides overview panels and configures footer buttons by groupType + role.
 *
 * Types from CreateNewGroupActivity:
 *   - "Rotating Pool"
 *   - "Fixed Contribution"
 *   - "Accumulative Funds"
 */
public final class GroupTypeUiController {

    private GroupTypeUiController() {}

    public static void applyOverviewPanels(View root, Map<String, Object> group, boolean isAdmin) {
        if (root == null || group == null) return;

        String type = str(group, "groupType", "");

        setVisible(root, "panel_rotating", type.contains("Rotating"));
        setVisible(root, "panel_fixed", type.contains("Fixed"));
        setVisible(root, "panel_accumulative", type.contains("Accumulative"));
        setVisible(root, "panel_payout_requests", isAdmin);

        // Group wallet — always useful
        setVisible(root, "card_group_wallet", true);
        String gWallet = str(group, "groupWalletAddress", str(group, "group_wallet", "—"));
        setText(root, "tv_group_wallet_address", gWallet);

        if (type.contains("Rotating")) {
            int slot = (int) num(group, "currentSlot", num(group, "currentRound", 1));
            int total = (int) num(group, "totalSlots", 0);
            setText(root, "tv_rotating_current", "Current slot: " + slot + (total > 0 ? " / " + total : ""));
            setText(root, "tv_rotating_next", "Next recipient: " + str(group, "nextRecipientName", "—"));
            setText(root, "tv_rotating_method", "Order: " + str(group, "distributionMethod", str(group, "disbursementRule", "—")));
        }

        if (type.contains("Fixed")) {
            double goal = num(group, "targetGoalAmount", 0);
            double raised = num(group, "raisedAmount", 0);
            setText(root, "tv_fixed_goal", "Target: " + com.ajoo.app.wallet.BalanceFormatter.smart(goal));
            setText(root, "tv_fixed_progress", "Raised: " + com.ajoo.app.wallet.BalanceFormatter.smart(raised)
                    + " / " + com.ajoo.app.wallet.BalanceFormatter.smart(goal));
            setText(root, "tv_fixed_rule", "Disbursement: " + str(group, "disbursementRule", "—"));
        }

        if (type.contains("Accumulative")) {
            double rate = num(group, "loanInterestRate", 0);
            setText(root, "tv_loan_rate", String.format(Locale.US, "Interest: %.2f%%", rate));
            setText(root, "tv_loan_withdrawal", "Withdrawal rule: " + str(group, "withdrawalRule", "—"));
        }
    }

    // THIS IS THE NEW METHOD TO FIX ERROR #5
    public static void setAdminActionsVisible(View root, boolean isAdmin) {
        if (root == null) return;
        
        // Add the String IDs of any admin-only views you want to toggle here.
        // For example, if you have a LinearLayout with id "panel_admin_actions":
        setVisible(root, "panel_admin_actions", isAdmin);
        
        // You can duplicate the setVisible line for as many admin buttons/panels as you have:
        // setVisible(root, "btn_edit_group", isAdmin);
        // setVisible(root, "btn_delete_group", isAdmin);
    }

    /**
     * Footer buttons on ActivityGroupDetails:
     *  Member: Request Payout | Apply for Loan
     *  Admin:  Approve Payout (or list) | Transfer Funds
     *
     * For Accumulative, members care more about loans; for Rotating, payouts.
     */
    public static void configureFooterButtons(TextView btnPayout,
                                              TextView btnLoanOrTransfer,
                                              String groupType,
                                              boolean isAdmin,
                                              Runnable onRequestPayout,
                                              Runnable onApprovePayout,
                                              Runnable onApplyLoan,
                                              Runnable onAdminTransfer) {
        if (btnPayout == null || btnLoanOrTransfer == null) return;

        if (isAdmin) {
            btnPayout.setText("Approve Payout");
            btnPayout.setOnClickListener(v -> {
                if (onApprovePayout != null) onApprovePayout.run();
            });
            btnLoanOrTransfer.setText("Transfer Funds");
            btnLoanOrTransfer.setOnClickListener(v -> {
                if (onAdminTransfer != null) onAdminTransfer.run();
            });
        } else {
            btnPayout.setText("Request Payout");
            btnPayout.setOnClickListener(v -> {
                if (onRequestPayout != null) onRequestPayout.run();
            });
            // Accumulative emphasizes loans; others still allow apply
            if (groupType != null && groupType.contains("Accumulative")) {
                btnLoanOrTransfer.setText("Apply for Loan");
            } else {
                btnLoanOrTransfer.setText("Apply for Loan");
            }
            btnLoanOrTransfer.setOnClickListener(v -> {
                if (onApplyLoan != null) onApplyLoan.run();
            });
        }
    }

    public static void setGroupWalletBalance(View root, double balanceAjc) {
        setText(root, "tv_group_wallet_balance",
                com.ajoo.app.wallet.BalanceFormatter.smart(balanceAjc));
    }

    private static void setVisible(View root, String idName, boolean visible) {
        int id = root.getResources().getIdentifier(idName, "id", root.getContext().getPackageName());
        if (id == 0) return;
        View v = root.findViewById(id);
        if (v != null) v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private static void setText(View root, String idName, String value) {
        int id = root.getResources().getIdentifier(idName, "id", root.getContext().getPackageName());
        if (id == 0) return;
        View v = root.findViewById(id);
        if (v instanceof TextView) ((TextView) v).setText(value != null ? value : "");
    }

    private static String str(Map<String, Object> m, String k, String d) {
        Object v = m.get(k);
        if (v == null) return d;
        String s = String.valueOf(v).trim();
        return s.isEmpty() || "null".equalsIgnoreCase(s) ? d : s;
    }

    private static double num(Map<String, Object> m, String k, double d) {
        Object v = m.get(k);
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v != null) {
            try { return Double.parseDouble(String.valueOf(v)); } catch (Exception ignored) {}
        }
        return d;
    }
}
