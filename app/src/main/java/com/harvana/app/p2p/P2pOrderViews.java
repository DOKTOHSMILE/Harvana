package com.harvana.app.p2p;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.harvana.app.R;
import com.harvana.app.utils.InfoTable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Shared pieces of the buy and sell trade screens: the order's timeline, the unpaid-order limit and the appeal form. */
public final class P2pOrderViews {

	private P2pOrderViews() {}

	/** Longest appeal the node accepts (category and details together). */
	public static final int MAX_APPEAL = 500;

	/** The node's own record of what happened to the order, one row per step, oldest first. */
	public static void events(Activity a, LinearLayout host, JSONArray events, String buyer, String seller) {
		if (host == null) return;
		host.removeAllViews();
		if (events == null || events.length() == 0) { host.setVisibility(View.GONE); return; }
		InfoTable t = new InfoTable(a).section("Timeline");
		SimpleDateFormat f = new SimpleDateFormat("d MMM HH:mm", Locale.getDefault());
		for (int i = 0; i < events.length(); i++) {
			JSONObject e = events.optJSONObject(i);
			if (e == null) continue;
			String by = e.optString("by", "");
			String who = by.equals(buyer) ? " · Buyer" : by.equals(seller) ? " · Seller" : by.isEmpty() ? "" : " · Network";
			long at = e.optLong("at", 0);
			t.row(label(e.optString("event", "")) + who, at > 0 ? f.format(new Date(at)) : "");
		}
		host.addView(t.view());
		host.setVisibility(View.VISIBLE);
	}

	private static String label(String event) {
		switch (event) {
			case "CREATED": return "Order created";
			case "ESCROW_LOCKED": return "Escrow locked";
			case "PAYMENT_MARKED": return "Marked paid";
			case "RELEASED": return "Released";
			case "CANCELLED": return "Cancelled";
			case "DISPUTED": return "Appeal opened";
			case "RESOLVED": return "Appeal resolved";
			case "ESCALATED": return "Sent for review";
			default: return event.isEmpty() ? "Update" : event.replace('_', ' ');
		}
	}

	/** Unpaid orders cancelled in the last 24 hours against the limit; shown only once one has been counted. */
	public static void unpaidLimit(Activity a, LinearLayout host, JSONObject status) {
		if (host == null || status == null) return;
		host.removeAllViews();
		int used = status.optInt("unpaidInLastDay", 0);
		int limit = status.optInt("limit", 3);
		long until = status.optLong("restrictedUntil", 0);
		if (used <= 0 && until <= 0) { host.setVisibility(View.GONE); return; }
		InfoTable t = new InfoTable(a).row("Unpaid cancels (24h)", used + " / " + limit,
			used >= limit ? a.getColor(R.color.colorAmber) : com.harvana.app.utils.UiColors.textPrimary());
		if (status.optBoolean("restricted", false) && until > 0) {
			t.row("New orders from", new SimpleDateFormat("d MMM HH:mm", Locale.getDefault()).format(new Date(until)));
		}
		host.addView(t.view());
		host.setVisibility(View.VISIBLE);
	}

	public interface AppealCallback { void submit(String reason); }

	/** The appeal form (res/layout/p2p_dispute_form.xml): a category and the details, sent as "Category: details". */
	public static void appealForm(Activity a, String defaultCategory, AppealCallback cb) {
		View form = a.getLayoutInflater().inflate(R.layout.p2p_dispute_form, null);
		RadioGroup group = form.findViewById(R.id.dispute_category);
		TextView details = form.findViewById(R.id.dispute_reason);
		for (int i = 0; i < group.getChildCount(); i++) {
			View c = group.getChildAt(i);
			if (c instanceof RadioButton && ((RadioButton) c).getText().toString().equals(defaultCategory)) {
				group.check(c.getId());
			}
		}
		ThemedDialogs.show(ThemedDialogs.alert(a)
			.setTitle("Appeal")
			.setView(form)
			.setPositiveButton("Submit", (d, w) -> {
				RadioButton picked = form.findViewById(group.getCheckedRadioButtonId());
				String category = picked != null ? picked.getText().toString() : "Other";
				String text = details.getText().toString().trim().replaceAll("\\s+", " ");
				if (text.isEmpty()) {
					Toast.makeText(a, "Add the appeal details.", Toast.LENGTH_LONG).show();
					return;
				}
				String reason = category + ": " + text;
				if (reason.length() > MAX_APPEAL) reason = reason.substring(0, MAX_APPEAL);
				cb.submit(reason);
			})
			.setNegativeButton("Cancel", null));
	}
}
