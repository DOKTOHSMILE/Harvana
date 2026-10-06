package com.harvana.app.group;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Locale;

/**
 * Reusable "what's happening on-chain right now" progress sheet — same
 * four real stages (sign, broadcast, mempool, confirmed) regardless of
 * which screen triggered the transaction, so a member sees genuine
 * transaction state, not a generic spinner.
 *
 * Non-dismissible only through stages 1-2 (signing/broadcasting) — an
 * accidental dismiss before the node has even accepted the transaction
 * could be genuinely confusing. Once stage 3 is reached, the transaction
 * has already been safely accepted by the node — its fate no longer
 * depends on this sheet staying open, so it becomes dismissible from
 * there. Confirmation can take longer than expected or never resolve
 * within the watcher's attempt limit; trapping the user until something
 * that might not happen happens was a real bug, not a safety feature.
 */
public final class TransactionProgressSheet {

	private final BottomSheetDialog dialog;
	private final Context context;
	private final View root;
	private int currentStage = 0;

	private TransactionProgressSheet(Context context, String title, double amountHarv) {
		this.context = context;
		dialog = new com.harvana.app.ui.AppSheet(context);
		root = LayoutInflater.from(context).inflate(R_layout("bottom_sheet_transaction_progress"), null);
		dialog.setContentView(root);
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);
		com.harvana.app.group.BottomSheetUtils.expandFullScreen(dialog, root);

		TextView tvTitle = findId("tv_progress_title");
		TextView tvAmount = findId("tv_progress_amount");
		if (tvTitle != null && title != null) tvTitle.setText(title);
		if (tvAmount != null) tvAmount.setText(com.harvana.app.wallet.BalanceFormatter.smart(amountHarv));

		View btnClose = root.findViewById(idOf("btn_progress_close"));
		if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
	}

	public static TransactionProgressSheet show(Context context, String title, double amountHarv) {
		TransactionProgressSheet sheet = new TransactionProgressSheet(context, title, amountHarv);
		sheet.dialog.show();
		sheet.advanceTo(1);
		return sheet;
	}

	/** Moves to the given stage (1-4) — every stage before it gets marked
	 *  done, this one becomes the active spinner, later ones stay pending. */
	public void advanceTo(int stage) {
		for (int i = 1; i <= 4; i++) {
			ProgressBar pb = findPb(i);
			View check = root.findViewById(idOf("iv_stage_" + i + "_check"));
			View dot = root.findViewById(idOf("dot_stage_" + i));

			if (i < stage) {
				if (pb != null) pb.setVisibility(View.GONE);
				if (dot != null) dot.setVisibility(View.GONE);
				if (check != null) check.setVisibility(View.VISIBLE);
			} else if (i == stage) {
				if (dot != null) dot.setVisibility(View.GONE);
				if (check != null) check.setVisibility(View.GONE);
				if (pb != null) pb.setVisibility(View.VISIBLE);
			} else {
				if (pb != null) pb.setVisibility(View.GONE);
				if (check != null) check.setVisibility(View.GONE);
				if (dot != null) dot.setVisibility(View.VISIBLE);
			}
		}
		currentStage = stage;

		// The real fix: once the node has accepted the transaction (stage 3+),
		// there's no reason to keep the person trapped waiting for a
		// confirmation that might take a while, or might not resolve within
		// the watcher's attempt limit at all.
		if (stage >= 3) {
			dialog.setCancelable(true);
			dialog.setCanceledOnTouchOutside(true);
			_showBackgroundOption();
		}
	}

	/** Shown once it's genuinely safe to leave — relabels the close button
	 *  so it's clear the transaction keeps going, not that it's finished. */
	private void _showBackgroundOption() {
		View closeCard = root.findViewById(idOf("card_progress_close"));
		TextView btnClose = findId("btn_progress_close");
		if (btnClose != null) btnClose.setText("Continue in Background");
		if (closeCard != null) closeCard.setVisibility(View.VISIBLE);
	}

	/** Stage 4 (Confirmed) has no spinner — it's a terminal state, so it
	 *  goes straight from pending dot to checkmark. */
	public void showConfirmed() {
		for (int i = 1; i <= 3; i++) {
			ProgressBar pb = findPb(i);
			View check = root.findViewById(idOf("iv_stage_" + i + "_check"));
			View dot = root.findViewById(idOf("dot_stage_" + i));
			if (pb != null) pb.setVisibility(View.GONE);
			if (dot != null) dot.setVisibility(View.GONE);
			if (check != null) check.setVisibility(View.VISIBLE);
		}
		View check4 = root.findViewById(idOf("iv_stage_4_check"));
		View dot4 = root.findViewById(idOf("dot_stage_4"));
		if (dot4 != null) dot4.setVisibility(View.GONE);
		if (check4 != null) check4.setVisibility(View.VISIBLE);

		View closeCard = root.findViewById(idOf("card_progress_close"));
		TextView btnClose = findId("btn_progress_close");
		if (btnClose != null) btnClose.setText("Done");
		if (closeCard != null) closeCard.setVisibility(View.VISIBLE);
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);
	}

	public void showError(String message) {
		// Stop whichever stage was in progress — don't claim it succeeded
		ProgressBar activePb = findPb(currentStage > 0 ? currentStage : 1);
		if (activePb != null) activePb.setVisibility(View.GONE);

		TextView tvError = findId("tv_progress_error");
		if (tvError != null) {
			tvError.setText(message != null ? message : "Transaction failed.");
			tvError.setVisibility(View.VISIBLE);
		}
		View closeCard = root.findViewById(idOf("card_progress_close"));
		TextView btnCloseErr = findId("btn_progress_close");
		if (btnCloseErr != null) btnCloseErr.setText("Done");
		if (closeCard != null) closeCard.setVisibility(View.VISIBLE);
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);
	}

	public void dismiss() {
		if (dialog.isShowing()) dialog.dismiss();
	}

	// ── Resource lookup helpers ──────────────────────────────────────

	private int R_layout(String name) {
		return context.getResources().getIdentifier(name, "layout", context.getPackageName());
	}

	private int idOf(String name) {
		return context.getResources().getIdentifier(name, "id", context.getPackageName());
	}

	private TextView findId(String name) {
		int id = idOf(name);
		return id != 0 ? root.findViewById(id) : null;
	}

	private ProgressBar findPb(int stage) {
		int id = idOf("pb_stage_" + stage);
		return id != 0 ? root.findViewById(id) : null;
	}
}