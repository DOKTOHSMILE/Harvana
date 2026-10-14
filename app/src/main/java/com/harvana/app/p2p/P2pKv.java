package com.harvana.app.p2p;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.harvana.app.R;

/** Label / value lines inside P2P cards (res/layout/item_p2p_kv.xml). */
public final class P2pKv {

	private P2pKv() {}

	public static TextView add(LinearLayout host, String label, CharSequence value) { return add(host, label, value, false); }

	/** Adds one line; {@code copy} adds a copy icon (and a tap on the line copies the value). Returns the value view. */
	public static TextView add(LinearLayout host, String label, CharSequence value, boolean copy) {
		Context c = host.getContext();
		View row = LayoutInflater.from(c).inflate(R.layout.item_p2p_kv, host, false);
		((TextView) row.findViewById(R.id.tv_kv_label)).setText(label);
		TextView v = row.findViewById(R.id.tv_kv_value);
		v.setText(value == null || value.length() == 0 ? "—" : value);
		if (copy && value != null && value.length() > 0) {
			row.findViewById(R.id.iv_kv_copy).setVisibility(View.VISIBLE);
			row.setOnClickListener(x -> copy(c, label, String.valueOf(value)));
		}
		host.addView(row);
		return v;
	}

	public static void copy(Context c, String label, String value) {
		ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
		if (cm != null) cm.setPrimaryClip(ClipData.newPlainText(label, value));
		Toast.makeText(c, label + " copied", Toast.LENGTH_SHORT).show();
	}
}
