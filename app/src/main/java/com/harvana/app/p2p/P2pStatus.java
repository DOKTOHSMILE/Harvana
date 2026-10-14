package com.harvana.app.p2p;

import com.harvana.app.R;

/** Order status names and colours, the same on every P2P screen. */
public final class P2pStatus {

	private P2pStatus() {}

	public static boolean active(String s) {
		return "AWAITING_PAYMENT".equals(s) || "PAYMENT_SUBMITTED".equals(s) || "RELEASE_PROCESSING".equals(s)
			|| "DISPUTED".equals(s) || "STUCK_NEEDS_MANUAL_REVIEW".equals(s);
	}

	public static String label(String s) {
		switch (s == null ? "" : s) {
			case "AWAITING_PAYMENT": return "Unpaid";
			case "PAYMENT_SUBMITTED": return "Paid";
			case "RELEASE_PROCESSING": return "Releasing";
			case "COMPLETED": return "Completed";
			case "CANCELLED": return "Cancelled";
			case "DISPUTED": return "Appeal";
			case "DISPUTE_RESOLVED_BUYER": return "Appeal: to buyer";
			case "DISPUTE_RESOLVED_SELLER": return "Appeal: to seller";
			case "STUCK_NEEDS_MANUAL_REVIEW": return "Under review";
			default: return s == null ? "" : s;
		}
	}

	public static int color(String s) {
		switch (s == null ? "" : s) {
			case "AWAITING_PAYMENT": return R.color.colorAmber;
			case "PAYMENT_SUBMITTED": case "RELEASE_PROCESSING": return R.color.colorInfo;
			case "COMPLETED": case "DISPUTE_RESOLVED_BUYER": case "DISPUTE_RESOLVED_SELLER": return R.color.colorSuccess;
			case "DISPUTED": case "STUCK_NEEDS_MANUAL_REVIEW": return R.color.colorError;
			default: return R.color.colorTextTertiary;
		}
	}
}
