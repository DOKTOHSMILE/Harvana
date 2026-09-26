package com.ajoo.app.group;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Locale;

/**
 * Reusable "review before you send" confirmation sheet — same layout for
 * any payment flow (group contribution, loan disbursement, group wallet
 * transfer), not written once and copy-pasted per screen.
 */
public final class PaymentConfirmSheet {

	public interface OnConfirm {
		void onConfirmed();
	}

	private PaymentConfirmSheet() {}

	public static void show(Context context, String title, double amountAjc,
	                         String recipientAddress, String memo, String feeDisplay,
	                         OnConfirm onConfirm) {
		if (context == null) return;

		BottomSheetDialog dialog = new BottomSheetDialog(context);
		View sheetView = LayoutInflater.from(context).inflate(R_layout(context, "bottom_sheet_payment_confirm"), null);
		dialog.setContentView(sheetView);
		BottomSheetUtils.expandFullScreen(dialog, sheetView);

		TextView tvTitle = findId(sheetView, "tv_confirm_title");
		TextView tvAmount = findId(sheetView, "tv_confirm_amount");
		TextView tvRecipient = findId(sheetView, "tv_confirm_recipient");
		TextView tvFee = findId(sheetView, "tv_confirm_fee");
		TextView tvMemo = findId(sheetView, "tv_confirm_memo");
		View rowMemo = sheetView.findViewById(idOf(context, "row_confirm_memo"));
		View btnCancel = sheetView.findViewById(idOf(context, "btn_confirm_cancel"));
		View btnSend = sheetView.findViewById(idOf(context, "btn_confirm_send"));

		if (tvTitle != null && title != null) tvTitle.setText(title);
		if (tvAmount != null) tvAmount.setText(com.ajoo.app.wallet.BalanceFormatter.smart(amountAjc));
		if (tvRecipient != null) tvRecipient.setText(_shortAddr(recipientAddress));
		if (tvFee != null) tvFee.setText(feeDisplay != null && !feeDisplay.isEmpty() ? feeDisplay : "—");
		if (memo != null && !memo.trim().isEmpty()) {
			if (rowMemo != null) rowMemo.setVisibility(View.VISIBLE);
			if (tvMemo != null) tvMemo.setText(memo.trim());
		}

		if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
		if (btnSend != null) {
			btnSend.setOnClickListener(v -> {
				dialog.dismiss();
				if (onConfirm != null) onConfirm.onConfirmed();
			});
		}

		dialog.show();
	}

	private static String _shortAddr(String a) {
		if (a == null || a.length() < 12) return a != null ? a : "—";
		return a.substring(0, 8) + "…" + a.substring(a.length() - 4);
	}

	// ── Resource lookup helpers — avoids a hard compile-time R class
	// dependency, matching the getIdentifier() pattern already used
	// throughout this codebase's group/ package. ──

	private static int R_layout(Context ctx, String name) {
		return ctx.getResources().getIdentifier(name, "layout", ctx.getPackageName());
	}

	private static int idOf(Context ctx, String name) {
		return ctx.getResources().getIdentifier(name, "id", ctx.getPackageName());
	}

	private static TextView findId(View root, String name) {
		int id = idOf(root.getContext(), name);
		return id != 0 ? root.findViewById(id) : null;
	}
}
