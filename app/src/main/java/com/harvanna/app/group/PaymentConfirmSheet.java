package com.harvanna.app.group;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.harvanna.app.wallet.PinPromptHelper;

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

	public static void show(Context context, String title, double amountHarv,
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
		if (tvAmount != null) tvAmount.setText(com.harvanna.app.wallet.BalanceFormatter.smart(amountHarv));
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

	/**
	 * Payment summary, then the PIN or fingerprint. Cancel on either step
	 * sends nothing. rows are label/value lines (who it is for, the fee, what it pays).
	 */
	public static void reviewThenAuthorize(Context context, String title, String amountText,
	                                        java.util.List<String[]> rows, Runnable onAuthorized) {
		if (context == null) return;
		int layout = R_layout(context, "bottom_sheet_payment_confirm");
		if (layout == 0) {
			PinPromptHelper.authorize(context, _action(title, amountText), () -> {
				if (onAuthorized != null) onAuthorized.run();
			});
			return;
		}

		BottomSheetDialog dialog = new BottomSheetDialog(context);
		View sheetView = LayoutInflater.from(context).inflate(layout, null);
		dialog.setContentView(sheetView);
		BottomSheetUtils.expandFullScreen(dialog, sheetView);

		TextView tvTitle = findId(sheetView, "tv_confirm_title");
		TextView tvAmount = findId(sheetView, "tv_confirm_amount");
		View rowTo = sheetView.findViewById(idOf(context, "linear43"));
		View rowMemo = sheetView.findViewById(idOf(context, "row_confirm_memo"));
		View rowFee = sheetView.findViewById(idOf(context, "linear44"));
		View rowsHost = sheetView.findViewById(idOf(context, "linear42"));
		View btnCancel = sheetView.findViewById(idOf(context, "btn_confirm_cancel"));
		View btnSend = sheetView.findViewById(idOf(context, "btn_confirm_send"));

		if (tvTitle != null && title != null) tvTitle.setText(title);
		if (tvAmount != null) {
			String shown = amountText == null ? "" : amountText.trim();
			boolean hideAmount = shown.isEmpty() || "—".equals(shown);
			tvAmount.setText(hideAmount ? "" : shown);
			View amountCard = sheetView.findViewById(idOf(context, "cardview4"));
			if (amountCard != null) amountCard.setVisibility(hideAmount ? View.GONE : View.VISIBLE);
		}
		if (rowTo != null) rowTo.setVisibility(View.GONE);
		if (rowMemo != null) rowMemo.setVisibility(View.GONE);
		if (rowFee != null) rowFee.setVisibility(View.GONE);

		if (rowsHost instanceof LinearLayout) {
			LinearLayout host = (LinearLayout) rowsHost;
			if (rows != null) {
				for (String[] row : rows) {
					if (row == null || row.length < 2 || row[0] == null) continue;
					String value = row[1] == null ? "" : row[1].trim();
					host.addView(detailRow(context, row[0], value.isEmpty() ? "—" : _present(value)));
				}
			}
			TextView note = new TextView(context);
			note.setText("Nothing is sent until you verify with your PIN or fingerprint.");
			note.setTextSize(12.5f);
			int pad = (int) (10 * context.getResources().getDisplayMetrics().density);
			note.setPadding(0, pad, 0, 0);
			host.addView(note);
		}

		if (btnSend instanceof TextView) ((TextView) btnSend).setText("Continue");
		if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
		if (btnSend != null) {
			btnSend.setOnClickListener(v -> {
				dialog.dismiss();
				PinPromptHelper.authorize(context, _action(title, amountText), () -> {
					if (onAuthorized != null) onAuthorized.run();
				});
			});
		}
		dialog.show();
	}

	private static String _action(String title, String amountText) {
		String t = title == null || title.trim().isEmpty() ? "Authorize this payment" : title.trim();
		if (amountText == null || amountText.trim().isEmpty() || "—".equals(amountText.trim())) return t;
		return t + " — " + amountText.trim();
	}

	private static String _present(String value) {
		if ((value.startsWith("HV0x") || value.startsWith("AJ0x")) && value.length() > 16) {
			return _shortAddr(value);
		}
		return value;
	}

	private static LinearLayout detailRow(Context ctx, String label, String value) {
		LinearLayout row = new LinearLayout(ctx);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		int pad = (int) (8 * ctx.getResources().getDisplayMetrics().density);
		row.setPadding(0, pad, 0, pad);
		TextView left = new TextView(ctx);
		left.setText(label);
		left.setTextSize(13.5f);
		TextView right = new TextView(ctx);
		right.setText(value);
		right.setTextSize(13.5f);
		right.setGravity(Gravity.END);
		row.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f));
		row.addView(right, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f));
		return row;
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
