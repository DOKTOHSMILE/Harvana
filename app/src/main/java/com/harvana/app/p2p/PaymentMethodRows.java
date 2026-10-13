package com.harvana.app.p2p;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.harvana.app.R;
import com.harvana.app.banks.BankLogo;
import com.harvana.app.wallet.PaymentMethod;

import java.util.List;

/** Payment accounts as rows with the bank's logo (res/layout/item_payment_method_select.xml), for every P2P screen. */
public final class PaymentMethodRows {

	private PaymentMethodRows() {}

	public interface Toggle { void changed(boolean chosen); }

	public interface Pick { void picked(PaymentMethod pm); }

	private static String title(PaymentMethod pm) {
		return pm.label != null && !pm.label.trim().isEmpty() ? pm.label.trim() : nz(pm.primaryField);
	}

	private static String subtitle(PaymentMethod pm) {
		StringBuilder sb = new StringBuilder();
		String bank = nz(pm.primaryField);
		if (!bank.isEmpty() && !bank.equalsIgnoreCase(title(pm))) sb.append(bank);
		for (String part : new String[]{nz(pm.accountIdentifier), nz(pm.accountName)}) {
			if (part.isEmpty()) continue;
			if (sb.length() > 0) sb.append(" · ");
			sb.append(part);
		}
		return sb.toString();
	}

	private static String nz(String s) { return s == null ? "" : s.trim(); }

	/** One row; {@code chosen} null hides the check (read-only lists). */
	public static View row(Activity a, ViewGroup parent, PaymentMethod pm, Boolean chosen) {
		View r = LayoutInflater.from(a).inflate(R.layout.item_payment_method_select, parent, false);
		BankLogo.into((ImageView) r.findViewById(R.id.pm_logo), nz(pm.primaryField));
		((TextView) r.findViewById(R.id.pm_title)).setText(title(pm));
		TextView sub = r.findViewById(R.id.pm_subtitle);
		String s = subtitle(pm);
		sub.setText(s);
		sub.setVisibility(s.isEmpty() ? View.GONE : View.VISIBLE);
		r.findViewById(R.id.pm_badge).setVisibility(pm.isDefault ? View.VISIBLE : View.GONE);
		setChosen(r, chosen != null && chosen);
		if (chosen == null) r.setForeground(null);
		return r;
	}

	private static void setChosen(View r, boolean chosen) {
		r.setActivated(chosen);
		r.findViewById(R.id.pm_check).setVisibility(chosen ? View.VISIBLE : View.INVISIBLE);
	}

	/** A row that toggles on tap (offer payment methods). */
	public static void addSelectable(Activity a, LinearLayout list, PaymentMethod pm, boolean chosen, Toggle t) {
		View r = row(a, list, pm, chosen);
		r.setOnClickListener(v -> {
			boolean now = !v.isActivated();
			setChosen(v, now);
			t.changed(now);
		});
		list.addView(r);
	}

	/** A read-only row (review and confirm screens). */
	public static void addReadOnly(Activity a, LinearLayout list, PaymentMethod pm) {
		list.addView(row(a, list, pm, null));
	}

	/** A bottom sheet of accounts; tapping one picks it and closes the sheet. */
	public static void pickSheet(Activity a, String title, List<PaymentMethod> methods, PaymentMethod current, Pick pick) {
		BottomSheetDialog d = ThemedDialogs.sheet(a);
		LinearLayout root = ThemedDialogs.sheetRoot(a);
		root.addView(ThemedDialogs.sheetTitle(a, title));
		LinearLayout list = new LinearLayout(a);
		list.setOrientation(LinearLayout.VERTICAL);
		for (PaymentMethod pm : methods) {
			boolean isCurrent = current != null && pm.id != null && pm.id.equals(current.id);
			View r = row(a, list, pm, isCurrent);
			r.setOnClickListener(v -> { d.dismiss(); pick.picked(pm); });
			list.addView(r);
		}
		root.addView(list);
		d.setContentView(root);
		d.show();
	}
}
