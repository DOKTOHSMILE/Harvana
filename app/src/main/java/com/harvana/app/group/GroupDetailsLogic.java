package com.harvana.app.group;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harvana.app.firestore.GroupFirestoreManager;
import com.harvana.app.sync.HarvanaNodeClient;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * All business logic for ActivityGroupDetailsActivity.
 * Call from initializeLogic / after group doc loads.
 */
public class GroupDetailsLogic {

    public interface UiBridge {
        void runOnUi(Runnable r);
        void showMessage(String msg);
        void openLoansTab();
        void showAdminTransferSheet();
        /** Sign+broadcast HARV from group wallet using SecureWalletStorage keys. */
        void transferFromGroupWallet(String from, String to, double amountHarv,
                                     GroupPayoutManager.Callback cb);
        String getMemberWalletAddress();
        int getMySlot();
    }

    private final Context appCtx;
    private final Activity activity;
    private final UiBridge ui;

    private String groupId = "";
    private String groupType = "";
    private String groupWalletAddress = "";
    private boolean isAdmin = false;
    private GroupIdentity identity = new GroupIdentity("", "", "", GroupIdentity.Kind.LEGACY_OFFCHAIN);
    private Map<String, Object> groupData = new HashMap<>();
    private double contributionAmount = 0;
    private int totalSlots = 0;

    public GroupDetailsLogic(Activity activity, UiBridge ui) {
        this.activity = activity;
        this.appCtx = activity != null ? activity.getApplicationContext() : null;
        this.ui = ui;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId != null ? groupId : "";
    }

    public String getGroupWalletAddress() { return groupWalletAddress; }
    public String getGroupType() { return groupType; }
    public boolean isAdmin() { return isAdmin; }
    public Map<String, Object> getGroupData() { return groupData; }

    /** Load group document then configure UI. */
    public void loadGroup(final TextView btnPayout, final TextView btnLoanOrTransfer, final ViewBinder headerBinder) {
        if (groupId.isEmpty()) {
            ui.showMessage("Missing group id.");
            return;
        }
        // Offline-first: render the last-known group doc immediately — this
        // is the shell every tab's header/footer/wallet-balance depends on,
        // so leaving it unhandled offline meant the whole screen sat blank
        // or stuck on "loading" even though the individual tabs (Members/
        // Payment/Loans/Notices) already had their own caching.
        final Map<String, Object> cachedGroup = com.harvana.app.offline.OfflineCacheManager.load(
                appCtx, "group_" + groupId);
        if (cachedGroup != null && !cachedGroup.isEmpty()) {
            _applyGroupData(cachedGroup, btnPayout, btnLoanOrTransfer, headerBinder);
        }

        GroupFirestoreManager.getInstance(appCtx).getGroup(groupId, new GroupFirestoreManager.DocCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                Map<String, Object> resolved = data != null ? data : new HashMap<>();
                com.harvana.app.offline.OfflineCacheManager.save(appCtx, "group_" + groupId, resolved);
                _applyGroupData(resolved, btnPayout, btnLoanOrTransfer, headerBinder);
            }

            @Override
            public void onError(final String error) {
                // A genuine connectivity failure leaves whatever cached
                // render already happened above alone — only surface a
                // real error, and only if there was nothing to fall back to.
                if (com.harvana.app.offline.NetworkStatus.isConnectivityError(error)
                        && cachedGroup != null && !cachedGroup.isEmpty()) {
                    return;
                }
                ui.runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        ui.showMessage("Failed to load group: " + error);
                    }
                });
            }
        });
    }

    private void _applyGroupData(Map<String, Object> data, final TextView btnPayout,
                                 final TextView btnLoanOrTransfer, final ViewBinder headerBinder) {
        groupData = data;
        identity = GroupIdentityResolver.resolve(groupId, data);
        groupType = str(groupData, "groupType", "");
        groupWalletAddress = str(groupData, "groupWalletAddress",
                str(groupData, "group_wallet", ""));
        contributionAmount = num(groupData, "contributionAmount", num(groupData, "amount", 0));
        totalSlots = (int) num(groupData, "totalSlots", num(groupData, "slots", 0));

        String creator = str(groupData, "creatorId", str(groupData, "adminUid", ""));
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        isAdmin = !identity.onChain() && !uid.isEmpty() && uid.equals(creator);

        ui.runOnUi(new Runnable() {
            @Override
            public void run() {
                if (headerBinder != null) headerBinder.bind(groupData, isAdmin);
                wireFooter(btnPayout, btnLoanOrTransfer);
                refreshGroupWalletBalance(null);
            }
        });
        if (!identity.onChain()) return;
        final String wallet = ui.getMemberWalletAddress();
        GroupRepository.loadProtocol(identity, wallet, (state, protocolCreator, escrow, raw, limit, round, callerIsCreator) -> {
            if (!escrow.isEmpty()) groupWalletAddress = escrow;
            if (raw > 0) contributionAmount = com.harvana.app.wallet.BalanceFormatter.rawToHarv(raw);
            if (limit > 0) totalSlots = limit;
            isAdmin = callerIsCreator;
            groupData.put("protocolState", state);
            groupData.put("protocolCreator", protocolCreator);
            groupData.put("family", identity.family);
            groupData.put("nodeGroupId", identity.nodeGroupId);
            if (round > 0) groupData.put("currentRound", round);
            ui.runOnUi(new Runnable() {
                @Override
                public void run() {
                    if (headerBinder != null) headerBinder.bind(groupData, isAdmin);
                    wireFooter(btnPayout, btnLoanOrTransfer);
                }
            });
        });
    }

    /** True when the group lives on the network, so its money moves only through signed requests the node carries out. */
    public boolean isOnChain() { return identity.onChain(); }

    /**
     * A group on the network is not paid by a Firestore request and a transfer from a phone: the node holds the money and
     * carries out what the admin signs and the members vote. So the footer opens the group's network actions (request a
     * payout, vote, approve a withdrawal) instead of the old payout and loan buttons, which only changed Firestore.
     */
    public void wireFooter(TextView btnPayout, TextView btnLoanOrTransfer) {
        // Goal groups: requests, votes, "return everyone's money" and past goals all sit on the Payments tab, so the
        // footer has nothing left to open.
        if (identity.onChain() && "FIXED".equals(com.harvana.app.sync.NodeGroupClient.familyOf(identity.protocolId()))) {
            if (btnPayout != null) btnPayout.setVisibility(android.view.View.GONE);
            if (btnLoanOrTransfer != null) btnLoanOrTransfer.setVisibility(android.view.View.GONE);
            android.view.ViewParent bar = btnPayout != null ? btnPayout.getParent() : null;
            if (bar instanceof android.view.ViewGroup && (btnLoanOrTransfer == null || btnLoanOrTransfer.getParent() == bar)) {
                android.view.ViewGroup g = (android.view.ViewGroup) bar;
                boolean any = false;
                for (int i = 0; i < g.getChildCount(); i++) if (g.getChildAt(i).getVisibility() == android.view.View.VISIBLE) any = true;
                if (!any) g.setVisibility(android.view.View.GONE);
            }
            return;
        }
        if (identity.onChain() && btnPayout != null) {
            btnPayout.setText("Actions & votes");
            btnPayout.setOnClickListener(v -> {
                android.content.Intent i = new android.content.Intent(activity, GroupActionsActivity.class);
                i.putExtra("nodeGroupId", identity.protocolId());
                i.putExtra("firestoreGroupId", groupId);
                activity.startActivity(i);
            });
            if (btnLoanOrTransfer != null) btnLoanOrTransfer.setVisibility(android.view.View.GONE);
            return;
        }
        GroupTypeUiController.configureFooterButtons(
                btnPayout,
                btnLoanOrTransfer,
                groupType,
                isAdmin,
                new Runnable() {
                    @Override public void run() { onMemberRequestPayout(); }
                },
                new Runnable() {
                    @Override public void run() { onAdminApprovePayoutFlow(); }
                },
                new Runnable() {
                    @Override public void run() { onMemberApplyLoan(); }
                },
                new Runnable() {
                    @Override public void run() { ui.showAdminTransferSheet(); }
                }
        );
    }

    // ── Member: Request Payout ───────────────────────────────────────────

    private void onMemberRequestPayout() {
        String wallet = ui.getMemberWalletAddress();
        int slot = ui.getMySlot();
        double amount = contributionAmount > 0 ? contributionAmount * Math.max(totalSlots, 1) : contributionAmount;

        if (groupType.contains("Rotating") && contributionAmount > 0 && totalSlots > 0) {
            amount = contributionAmount * totalSlots;
        }

        final double sendAmount = amount;
        GroupPayoutManager.requestPayout(appCtx, groupId, wallet, sendAmount, slot,
                new GroupPayoutManager.Callback() {
                    @Override public void onSuccess(final String message) {
                        ui.runOnUi(new Runnable() {
                            @Override public void run() { ui.showMessage(message); }
                        });
                    }
                    @Override public void onError(final String error) {
                        ui.runOnUi(new Runnable() {
                            @Override public void run() { ui.showMessage(error); }
                        });
                    }
                });
    }

    // ── Admin: Approve Payout ────────────────────────────────────────────

    private void onAdminApprovePayoutFlow() {
        GroupPayoutManager.listPendingPayoutRequests(appCtx, groupId,
                new GroupPayoutManager.ListCallback() {
                    @Override
                    public void onSuccess(final List<Map<String, Object>> items) {
                        ui.runOnUi(new Runnable() {
                            @Override public void run() { showApprovePicker(items); }
                        });
                    }
                    @Override
                    public void onError(final String error) {
                        ui.runOnUi(new Runnable() {
                            @Override public void run() { ui.showMessage("Could not load requests: " + error); }
                        });
                    }
                });
    }

    private void showApprovePicker(List<Map<String, Object>> items) {
        if (activity == null || activity.isFinishing()) return;
        BottomSheetDialog dialog = new com.harvana.app.ui.AppSheet(activity);
        int layoutId = activity.getResources().getIdentifier(
                "bottom_sheet_payout_requests", "layout", activity.getPackageName());
        if (layoutId == 0) {
            ui.showMessage("No pending payout requests.");
            return;
        }
        View sheetView = LayoutInflater.from(activity).inflate(layoutId, null);
        dialog.setContentView(sheetView);

        RecyclerView recycler = sheetView.findViewById(
                activity.getResources().getIdentifier("recycler_payout_requests", "id", activity.getPackageName()));
        TextView empty = sheetView.findViewById(
                activity.getResources().getIdentifier("tv_payout_requests_empty", "id", activity.getPackageName()));

        if (items == null || items.isEmpty()) {
            if (empty != null) empty.setVisibility(View.VISIBLE);
            if (recycler != null) recycler.setVisibility(View.GONE);
        } else if (recycler != null) {
            recycler.setLayoutManager(new LinearLayoutManager(activity));
            recycler.setAdapter(new PayoutRequestAdapter(items, dialog));
        }

        dialog.show();
    }

    private class PayoutRequestAdapter extends RecyclerView.Adapter<PayoutRequestAdapter.VH> {
        private final List<Map<String, Object>> data;
        private final BottomSheetDialog dialog;

        PayoutRequestAdapter(List<Map<String, Object>> data, BottomSheetDialog dialog) {
            this.data = data;
            this.dialog = dialog;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layoutId = parent.getResources().getIdentifier(
                    "payout_request_row", "layout", parent.getContext().getPackageName());
            View v = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull final VH h, int pos) {
            Map<String, Object> req = data.get(pos);
            final String toWallet = str(req, "requesterWallet", "");
            final double amt = num(req, "amountHarv", 0);
            final String reqId = str(req, "_docId", "");
            final String requesterUid = str(req, "requesterUid", "");
            long slot = (long) num(req, "slot", 0);

            h.itemView.setTag(requesterUid);

            if (h.slot != null) h.slot.setText(slot > 0 ? "Slot " + slot : "");
            if (h.amount != null) h.amount.setText(com.harvana.app.wallet.BalanceFormatter.smart(amt));
            if (h.wallet != null) h.wallet.setText(shortAddr(toWallet));
            if (h.requester != null) h.requester.setText("Member");

            if (!requesterUid.isEmpty() && h.requester != null) {
                // Fix: was resolving via legacy RTDB userdb/{uid}. Firestore
                // is the real profile source post-migration — same fix as
                // everywhere else names are resolved in this codebase.
                com.harvana.app.trust.ProfileTrust.nameWithTrust(activity, h.requester, requesterUid, "Member");
            }

            if (h.approveBtn != null) {
                h.approveBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        if (toWallet.isEmpty() || !toWallet.startsWith("HV0x")) {
                            promptWalletAndApprove(reqId, amt);
                            return;
                        }
                        doApproveTransfer(reqId, toWallet, amt);
                    }
                });
            }
        }

        @Override
        public int getItemCount() { return data != null ? data.size() : 0; }

        class VH extends RecyclerView.ViewHolder {
            TextView requester, slot, amount, wallet, approveBtn;
            VH(View v) {
                super(v);
                requester = v.findViewById(v.getResources().getIdentifier("tv_payout_requester", "id", v.getContext().getPackageName()));
                slot = v.findViewById(v.getResources().getIdentifier("tv_payout_slot", "id", v.getContext().getPackageName()));
                amount = v.findViewById(v.getResources().getIdentifier("tv_payout_amount", "id", v.getContext().getPackageName()));
                wallet = v.findViewById(v.getResources().getIdentifier("tv_payout_wallet", "id", v.getContext().getPackageName()));
                approveBtn = v.findViewById(v.getResources().getIdentifier("btn_payout_approve", "id", v.getContext().getPackageName()));
            }
        }
    }

    private void promptWalletAndApprove(final String reqId, final double amt) {
        if (activity == null || activity.isFinishing()) return;
        final EditText input = new com.google.android.material.textfield.TextInputEditText(activity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Member HV0x wallet");
        LinearLayout wrap = new LinearLayout(activity);
        wrap.setPadding(48, 24, 48, 0);
        wrap.addView(input);
        new com.harvana.app.ui.SheetDialog.Builder(activity)
                .setTitle("Member wallet")
                .setView(wrap)
                .setPositiveButton("Approve", (d, w) -> {
                    String to = input.getText().toString().trim();
                    doApproveTransfer(reqId, to, amt > 0 ? amt : contributionAmount);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // THE FIX (client-side half): tracks reqIds currently being approved
    // so a double-tap on the row, or reopening the sheet before Firestore
    // reflects a status change, can't fire a second transfer for the same
    // request while the first is still in flight. The server-side
    // re-validation in GroupPayoutManager.approvePayoutAndTransfer() is the
    // real guarantee; this is the fast, local first line of defense.
    private final java.util.Set<String> payoutRequestsInFlight = new java.util.HashSet<>();

    private void doApproveTransfer(String reqId, String toWallet, double amountHarv) {
        if (groupWalletAddress.isEmpty()) {
            ui.showMessage("Group wallet not loaded.");
            return;
        }
        if (!payoutRequestsInFlight.add(reqId)) {
            // Already approving this exact request — ignore the extra tap.
            return;
        }
        GroupPayoutManager.approvePayoutAndTransfer(
                appCtx, groupId, reqId, groupWalletAddress, toWallet, amountHarv,
                new GroupPayoutManager.GroupWalletTransfer() {
                    @Override
                    public void transfer(String from, String to, double amt, GroupPayoutManager.Callback cb) {
                        ui.transferFromGroupWallet(from, to, amt, cb);
                    }
                },
                new GroupPayoutManager.Callback() {
                    @Override
                    public void onSuccess(final String message) {
                        payoutRequestsInFlight.remove(reqId);
                        ui.runOnUi(new Runnable() {
                            @Override public void run() { ui.showMessage(message); }
                        });
                    }
                    @Override
                    public void onError(final String error) {
                        payoutRequestsInFlight.remove(reqId);
                        ui.runOnUi(new Runnable() {
                            @Override public void run() { ui.showMessage(error); }
                        });
                    }
                });
    }

    // ── Member: Apply for Loan ───────────────────────────────────────────

    private void onMemberApplyLoan() {
        if (activity == null || activity.isFinishing()) return;
        final double rate = num(groupData, "loanInterestRate", 0);
        final String interestPeriod = str(groupData, "loanInterestPeriod", "MONTHLY");
        showLoanApplicationSheet(rate, interestPeriod);
    }

    /**
     * Bottom sheet loan application — same bottom_sheet_payment_confirm
     * layout and dynamic-swap-in-place technique as the repay sheet
     * (LoansFragmentActivity._showLoanAmountSheet): the amount card's
     * static TextView is hidden and an EditText added in its place, same
     * idea used here for the amount AND the note field, instead of the
     * old plain AlertDialog. "To" row is repurposed to show the interest
     * rate; the network-fee row is hidden since applying is a Firestore
     * write, not an on-chain transfer.
     */
    private void showLoanApplicationSheet(final double rate, final String interestPeriod) {
        int layoutId = activity.getResources().getIdentifier(
                "bottom_sheet_payment_confirm", "layout", activity.getPackageName());
        if (layoutId == 0) {
            ui.showMessage("Payment confirm layout missing.");
            return;
        }

        final BottomSheetDialog sheet = new com.harvana.app.ui.AppSheet(activity);
        View root = LayoutInflater.from(activity).inflate(layoutId, null);
        sheet.setContentView(root);

        TextView tvTitle = root.findViewById(activity.getResources().getIdentifier(
                "tv_confirm_title", "id", activity.getPackageName()));
        TextView tvAmount = root.findViewById(activity.getResources().getIdentifier(
                "tv_confirm_amount", "id", activity.getPackageName()));
        TextView tvToLabel = root.findViewById(activity.getResources().getIdentifier(
                "textview6", "id", activity.getPackageName()));
        TextView tvRecipient = root.findViewById(activity.getResources().getIdentifier(
                "tv_confirm_recipient", "id", activity.getPackageName()));
        View rowFee = root.findViewById(activity.getResources().getIdentifier(
                "linear44", "id", activity.getPackageName()));
        LinearLayout detailsColumn = root.findViewById(activity.getResources().getIdentifier(
                "linear42", "id", activity.getPackageName()));
        View btnCancel = root.findViewById(activity.getResources().getIdentifier(
                "btn_confirm_cancel", "id", activity.getPackageName()));
        View btnSend = root.findViewById(activity.getResources().getIdentifier(
                "btn_confirm_send", "id", activity.getPackageName()));

        if (tvTitle != null) {
            tvTitle.setText(rate > 0
                    ? String.format(Locale.US, "Apply for loan (%.2f%% %s)", rate, interestPeriod.toLowerCase())
                    : "Apply for loan");
        }

        // Amount card -> editable principal input.
        final EditText amountInput = new com.google.android.material.textfield.TextInputEditText(activity);
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amountInput.setHint(("Amount in " + com.harvana.app.CoinInfo.ticker()));
        amountInput.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        amountInput.setTextSize(20);
        amountInput.setBackground(null);
        if (tvAmount != null) {
            amountInput.setTextColor(tvAmount.getCurrentTextColor());
            if (tvAmount.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) tvAmount.getParent();
                int idx = parent.indexOfChild(tvAmount);
                tvAmount.setVisibility(View.GONE);
                parent.addView(amountInput, idx + 1,
                        new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        }

        // "To" row -> interest rate summary.
        if (tvToLabel != null) tvToLabel.setText("Interest");
        if (tvRecipient != null) {
            tvRecipient.setText(rate > 0
                    ? String.format(Locale.US, "%.2f%% %s", rate, interestPeriod.toLowerCase())
                    : "No interest");
        }

        // No on-chain fee at application time - hide that row.
        if (rowFee != null) rowFee.setVisibility(View.GONE);

        // Note field: appended as its own full-width row rather than
        // squeezed into the memo row's label+value split, which is too
        // narrow for comfortable typing.
        final EditText noteInput = new com.google.android.material.textfield.TextInputEditText(activity);
        noteInput.setHint("Note (optional)");
        noteInput.setTextSize(13);
        if (detailsColumn != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = (int) (8 * activity.getResources().getDisplayMetrics().density);
            detailsColumn.addView(noteInput, lp);
        }

        if (btnCancel != null) {
            if (btnCancel instanceof TextView) ((TextView) btnCancel).setText("Cancel");
            btnCancel.setOnClickListener(v -> sheet.dismiss());
        }

        if (btnSend != null) {
            if (btnSend instanceof TextView) ((TextView) btnSend).setText("Submit");
            btnSend.setOnClickListener(v -> {
                double principal;
                try {
                    principal = Double.parseDouble(amountInput.getText().toString().trim());
                } catch (Exception e) {
                    ui.showMessage("Invalid amount");
                    return;
                }
                if (principal <= 0) {
                    ui.showMessage("Amount must be greater than zero.");
                    return;
                }
                String wallet = ui.getMemberWalletAddress();
                String note = noteInput.getText().toString().trim();
                sheet.dismiss();

                GroupLoanManager.applyForLoan(
                        appCtx,
                        groupId,
                        wallet,
                        principal,
                        rate,
                        interestPeriod,
                        note,
                        new GroupLoanManager.Callback() {
                            @Override
                            public void onSuccess(final String message) {
                                ui.runOnUi(new Runnable() {
                                    @Override public void run() { ui.showMessage(message); }
                                });
                            }
                            @Override
                            public void onError(final String error) {
                                ui.runOnUi(new Runnable() {
                                    @Override public void run() { ui.showMessage(error); }
                                });
                            }
                        });
            });
        }

        sheet.show();
    }

    // ── Group wallet balance from node ───────────────────────────────────

    public void refreshGroupWalletBalance(final TextView balanceView) {
        if (groupWalletAddress.isEmpty() || !groupWalletAddress.startsWith("HV0x")) return;
        HarvanaNodeClient.getWallet(groupWalletAddress, new HarvanaNodeClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                long raw = json.optLong("balance", 0L);
                final double harv = HarvanaNodeClient.toHarvDisplay(raw);
                ui.runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        if (balanceView != null) {
                            balanceView.setText(com.harvana.app.wallet.BalanceFormatter.smart(harv));
                        }
                    }
                });
            }
            @Override public void onError(String error) { /* keep last value */ }
        });
    }

    // ── helpers ──────────────────────────────────────────────────────────

    public interface ViewBinder {
        void bind(Map<String, Object> group, boolean isAdmin);
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

    private static String shortAddr(String a) {
        if (a == null || a.length() < 12) return a != null ? a : "";
        return a.substring(0, 8) + "…" + a.substring(a.length() - 4);
    }
}