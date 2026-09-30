package com.harvanna.app.group;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Logic for LoansFragmentActivity — list + admin approve/reject */
public class LoansFragmentLogic {

	private final Context ctx;
	private final Activity activity;
	private String groupId = "";
	private boolean isAdmin = false;
	private String groupWallet = "";
	private GroupPayoutManager.GroupWalletTransfer transfer;

	public LoansFragmentLogic(Activity activity) {
		this.activity = activity;
		this.ctx = activity.getApplicationContext();
	}

	public void setGroupId(String id) { groupId = id != null ? id : ""; }
	public void setAdmin(boolean a) { isAdmin = a; }
	public void setGroupWallet(String w) { groupWallet = w != null ? w : ""; }
	public void setTransfer(GroupPayoutManager.GroupWalletTransfer t) { transfer = t; }

	public void load(final RecyclerView recycler, final TextView empty) {
		if (groupId.isEmpty()) return;
		GroupLoanManager.listLoans(ctx, groupId, new GroupLoanManager.ListCallback() {
			@Override
			public void onSuccess(List<Map<String, Object>> items) {
				final List<Map<String, Object>> list = items != null ? items : new ArrayList<Map<String, Object>>();
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (empty != null) {
							empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
						}
						if (recycler == null) return;
						if (recycler.getLayoutManager() == null) {
							recycler.setLayoutManager(new LinearLayoutManager(ctx));
						}
						recycler.setAdapter(new LoanAdapter(list, isAdmin, groupId, groupWallet, transfer, activity, ctx));
					}
				});
			}
			@Override public void onError(String error) {}
		});
	}

	/** Static nested adapter — required so VH / helpers can be static */
	private static class LoanAdapter extends RecyclerView.Adapter<LoanAdapter.VH> {
		private final List<Map<String, Object>> items;
		private final boolean isAdmin;
		private final String groupId;
		private final String groupWallet;
		private final GroupPayoutManager.GroupWalletTransfer transfer;
		private final Activity activity;
		private final Context ctx;

		LoanAdapter(List<Map<String, Object>> items, boolean isAdmin, String groupId,
					String groupWallet, GroupPayoutManager.GroupWalletTransfer transfer,
					Activity activity, Context ctx) {
			this.items = items;
			this.isAdmin = isAdmin;
			this.groupId = groupId;
			this.groupWallet = groupWallet;
			this.transfer = transfer;
			this.activity = activity;
			this.ctx = ctx;
		}

		@NonNull
		@Override
		public VH onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
			TextView tv = new TextView(parent.getContext());
			tv.setPadding(32, 28, 32, 28);
			tv.setTextColor(0xFFF2F5F9);
			tv.setTextSize(13f);
			return new VH(tv);
		}

		@Override
		public void onBindViewHolder(@NonNull VH h, int pos) {
			Map<String, Object> loan = items.get(pos);
			String status = str(loan, "status", "PENDING");
			double principal = num(loan, "principalHarv", num(loan, "amount", 0));
			String wallet = str(loan, "borrowerWallet", "");
			final String loanId = str(loan, "_docId", "");
			h.tv.setText(status + " · " + com.harvanna.app.wallet.BalanceFormatter.smart(principal) + "\n" + shortAddr(wallet));

			if (isAdmin && ("PENDING".equalsIgnoreCase(status) || "pending".equalsIgnoreCase(status))) {
				final double fPrincipal = principal;
				final String fWallet = wallet;
				h.tv.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						confirmApprove(loanId, fWallet, fPrincipal);
					}
				});
			} else {
				h.tv.setOnClickListener(null);
			}
		}

		private void confirmApprove(final String loanId, final String borrowerWallet, final double principal) {
			new AlertDialog.Builder(activity)
					.setTitle("Approve loan?")
					.setMessage("Disburse " + com.harvanna.app.wallet.BalanceFormatter.smart(principal)
							+ " from group wallet to " + shortAddr(borrowerWallet))
					.setPositiveButton("Approve", new android.content.DialogInterface.OnClickListener() {
						@Override
						public void onClick(android.content.DialogInterface d, int w) {
							GroupLoanManager.approveAndDisburse(ctx, groupId, loanId,
									groupWallet, borrowerWallet, principal, transfer,
									new GroupLoanManager.Callback() {
										@Override
										public void onSuccess(String message) {
											activity.runOnUiThread(new Runnable() {
												@Override
												public void run() {
													Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
												}
											});
										}
										@Override
										public void onError(String error) {
											activity.runOnUiThread(new Runnable() {
												@Override
												public void run() {
													Toast.makeText(ctx, error, Toast.LENGTH_SHORT).show();
												}
											});
										}
									});
						}
					})
					.setNeutralButton("Reject", new android.content.DialogInterface.OnClickListener() {
						@Override
						public void onClick(android.content.DialogInterface d, int w) {
							GroupLoanManager.rejectLoan(ctx, groupId, loanId, new GroupLoanManager.Callback() {
								@Override
								public void onSuccess(String message) {
									activity.runOnUiThread(new Runnable() {
										@Override
										public void run() {
											Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
										}
									});
								}
								@Override public void onError(String error) {}
							});
						}
					})
					.setNegativeButton("Cancel", null)
					.show();
		}

		@Override
		public int getItemCount() {
			return items.size();
		}

		static class VH extends RecyclerView.ViewHolder {
			TextView tv;
			VH(View v) {
				super(v);
				tv = (TextView) v;
			}
		}

		static String str(Map<String, Object> m, String k, String d) {
			Object v = m.get(k);
			return v != null ? String.valueOf(v) : d;
		}

		static double num(Map<String, Object> m, String k, double d) {
			Object v = m.get(k);
			if (v instanceof Number) return ((Number) v).doubleValue();
			try {
				return Double.parseDouble(String.valueOf(v));
			} catch (Exception e) {
				return d;
			}
		}

		static String shortAddr(String a) {
			if (a == null || a.length() < 12) return a != null ? a : "";
			return a.substring(0, 8) + "…" + a.substring(a.length() - 4);
		}
	}
}