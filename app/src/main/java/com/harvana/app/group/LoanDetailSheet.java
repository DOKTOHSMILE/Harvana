package com.harvana.app.group;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.harvana.app.R;
import com.harvana.app.utils.InfoTable;
import com.harvana.app.utils.UiColors;
import com.harvana.app.wallet.BalanceFormatter;
import com.harvana.app.wallet.WalletManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The proper loan detail bottom sheet — inflates {@code bottom_sheet_loan_detail.xml}
 * and wires the two action buttons that were missing in the legacy
 * {@code LoansFragmentLogic} flow:
 *
 * <ul>
 *   <li><strong>Repay</strong> — visible to the borrower when the loan is ACTIVE.
 *       Tapping it opens a confirm sheet with the current total due; on confirm it calls
 *       {@link GroupLoanManager#recordRepayment} which routes to
 *       {@code chain.NodeGroupClient.genLoanRepay} for on-chain groups and to the
 *       legacy Firestore path otherwise.</li>
 *   <li><strong>Sign as guardian</strong> — visible to a guardian when the loan is
 *       PENDING_APPROVAL. Tapping it calls
 *       {@link GroupLoanManager#guardianSignLoan} which signs
 *       {@code HARVANA_GENLOAN_APPROVE_V1:groupId:loanId} and submits via
 *       {@code chain.NodeGroupClient.genLoanGuardianSignature} (no-nonce path).</li>
 * </ul>
 *
 * The sheet also shows a "Network" row in the breakdown card for on-chain loans,
 * so the user can see whether their action is going to the node or staying in Firestore.
 */
public final class LoanDetailSheet {

    private LoanDetailSheet() {}

    /** Open the sheet for the given loan. Call from any activity / fragment. */
    public static void show(Context ctx, String groupId, String loanId,
                            Map<String, Object> loan,
                            List<Map<String, Object>> repayments,
                            boolean isBorrower, boolean isGuardian) {
        BottomSheetDialog d = new com.harvana.app.ui.AppSheet(ctx);
        View v = LayoutInflater.from(ctx).inflate(R.layout.bottom_sheet_loan_detail, null);

        // Wire close.
        v.findViewById(R.id.btn_loan_detail_close).setOnClickListener(x -> d.dismiss());

        // Populate the read-only fields.
        populate(v, loan, repayments, isBorrower, isGuardian);

        // Wire the action buttons.
        MaterialButton btnRepay = v.findViewById(R.id.btn_loan_detail_repay);
        MaterialButton btnSign = v.findViewById(R.id.btn_loan_detail_guardian_sign);
        LinearLayout actions = v.findViewById(R.id.row_loan_detail_actions);

        String status = str(loan, "status", "PENDING");
        double totalDue = num(loan, "totalDue", 0);
        String source = str(loan, "source", "");

        boolean showActions = false;
        if (isBorrower && "ACTIVE".equalsIgnoreCase(status) && totalDue > 0.0001) {
            btnRepay.setVisibility(View.VISIBLE);
            btnRepay.setOnClickListener(x -> {
                d.dismiss();
                promptRepay(ctx, groupId, loanId, loan, totalDue);
            });
            showActions = true;
        } else {
            btnRepay.setVisibility(View.GONE);
        }
        if (isGuardian && ("PENDING_APPROVAL".equalsIgnoreCase(status)
                || "PENDING".equalsIgnoreCase(status))) {
            btnSign.setVisibility(View.VISIBLE);
            btnSign.setOnClickListener(x -> {
                d.dismiss();
                GroupLoanManager.guardianSignLoan(ctx, groupId, loanId,
                        new GroupLoanManager.Callback() {
                            @Override public void onSuccess(String message) {
                                toast(ctx, message);
                            }
                            @Override public void onError(String error) {
                                toast(ctx, error);
                            }
                        });
            });
            showActions = true;
        } else {
            btnSign.setVisibility(View.GONE        );
        }
        actions.setVisibility(showActions ? View.VISIBLE : View.GONE);

        d.setContentView(v);
        d.show();
    }

    private static void populate(View v, Map<String, Object> loan,
                                 List<Map<String, Object>> repayments,
                                 boolean isBorrower, boolean isGuardian) {
        String status = str(loan, "status", "PENDING");
        String loanId = str(loan, "_docId", "");
        String borrower = str(loan, "borrowerWallet", "");
        String source = str(loan, "source", "");
        double principal = num(loan, "principalHarv", num(loan, "amount", 0));
        double interestRate = num(loan, "interestRate", 0);
        String interestPeriod = str(loan, "interestPeriod", "DAILY");
        long disbursedAt = (long) num(loan, "disbursedAt", 0);
        boolean onChain = "node".equalsIgnoreCase(source) || isOnChainGroup(loan);

        // Status pill.
        TextView tvStatus = v.findViewById(R.id.tv_loan_detail_status);
        tvStatus.setText(status);
        // Borrower
        TextView tvBorrower = v.findViewById(R.id.tv_loan_detail_borrower);
        tvBorrower.setText(shortAddr(borrower));
        // Breakdown rows
        ((TextView) v.findViewById(R.id.tv_loan_detail_id)).setText(loanId);
        ((TextView) v.findViewById(R.id.tv_loan_detail_disbursed)).setText(disbursedAt > 0
                ? new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(disbursedAt))
                : "—");
        ((TextView) v.findViewById(R.id.tv_loan_detail_principal)).setText(BalanceFormatter.smart(principal));
        ((TextView) v.findViewById(R.id.tv_loan_detail_rate)).setText(interestRate + "% / " + interestPeriod);
        double accrued = GroupLoanManager.computeAccruedTotalDue(principal, interestRate, interestPeriod, disbursedAt);
        ((TextView) v.findViewById(R.id.tv_loan_detail_interest_accrued)).setText(BalanceFormatter.smart(accrued - principal));
        double alreadyRepaid = 0;
        if (repayments != null) {
            for (Map<String, Object> p : repayments) {
                if (!"LOAN_REPAY".equalsIgnoreCase(str(p, "type", ""))) continue;
                if (!loanId.equals(str(p, "loanId", ""))) continue;
                String st = str(p, "status", "");
                if (!st.isEmpty() && !"CONFIRMED".equalsIgnoreCase(st)) continue;
                alreadyRepaid += num(p, "amountHarv", 0);
            }
        }
        ((TextView) v.findViewById(R.id.tv_loan_detail_repaid)).setText(BalanceFormatter.smart(alreadyRepaid));
        double totalDue = Math.max(0, accrued - alreadyRepaid);
        ((TextView) v.findViewById(R.id.tv_loan_detail_total_due)).setText(BalanceFormatter.smart(totalDue));
        // Stash the totalDue so the action button uses the same value the user just saw.
        loan.put("totalDue", totalDue);

        // Append a "Network" row to the breakdown card so the user knows where the action goes.
        InfoTable net = new InfoTable(v.getContext());
        net.row(R.drawable.ic_info, "Network", onChain ? "On-chain (signed on the node)" : "Firestore (legacy)");
        LinearLayout card = (LinearLayout) ((android.view.ViewGroup) v.findViewById(R.id.cardview3)).getChildAt(0);
        if (card != null) card.addView(net.view());

        // Repayment history
        LinearLayout history = v.findViewById(R.id.layout_loan_repayment_history);
        TextView empty = v.findViewById(R.id.tv_loan_no_repayments);
        if (repayments == null || repayments.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
        } else {
            empty.setVisibility(View.GONE);
            for (Map<String, Object> p : repayments) {
                if (!"LOAN_REPAY".equalsIgnoreCase(str(p, "type", ""))) continue;
                if (!loanId.equals(str(p, "loanId", ""))) continue;
                String st = str(p, "status", "");
                long ts = (long) num(p, "timestamp", num(p, "confirmedAt", 0));
                String when = ts > 0
                        ? new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(ts))
                        : "—";
                InfoTable t = new InfoTable(v.getContext());
                t.row(R.drawable.ic_check_circle_line, "Repayment · " + when, BalanceFormatter.smart(num(p, "amountHarv", 0)));
                t.row(R.drawable.ic_info, "Source", "node".equalsIgnoreCase(str(p, "source", "")) ? "On-chain" : "Firestore");
                t.row(R.drawable.ic_info, "Status", st);
                history.addView(t.view());
            }
        }
    }

    private static void promptRepay(Context ctx, String groupId, String loanId,
                                    Map<String, Object> loan, double totalDue) {
        com.harvana.app.ui.SheetDialog d = new com.harvana.app.ui.SheetDialog.Builder(ctx)
                .setTitle("Repay this loan?")
                .setMessage("You owe " + BalanceFormatter.smart(totalDue) + " today. The repayment goes through "
                        + (isOnChainGroup(loan) ? "the network (on-chain)." : "the group ledger (legacy)."))
                .setPositiveButton("Repay full amount", (x, w) -> {
                    GroupLoanManager.recordRepayment(ctx, groupId, loanId,
                            WalletManager.getActiveWalletAddress(ctx),
                            str(loan, "groupWallet", str(loan, "_groupWallet", "")),
                            totalDue, "", true,
                            new GroupLoanManager.Callback() {
                                @Override public void onSuccess(String message) { toast(ctx, message); }
                                @Override public void onError(String error) { toast(ctx, error); }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static boolean isOnChainGroup(Map<String, Object> loan) {
        Object o = loan.get("source");
        if (o != null && "node".equalsIgnoreCase(String.valueOf(o))) return true;
        Object g = loan.get("groupOnChain");
        if (g instanceof Boolean) return (Boolean) g;
        return false;
    }

    private static String str(Map<String, Object> m, String k, String d) {
        if (m == null) return d;
        Object v = m.get(k);
        return v != null ? String.valueOf(v) : d;
    }

    private static double num(Map<String, Object> m, String k, double d) {
        if (m == null) return d;
        Object v = m.get(k);
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return d; }
    }

    private static String shortAddr(String a) {
        if (a == null || a.length() < 12) return a == null ? "—" : a;
        return a.substring(0, 6) + "…" + a.substring(a.length() - 4);
    }

    private static void toast(Context ctx, String s) {
        android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
        main.post(() -> Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show());
    }
}
