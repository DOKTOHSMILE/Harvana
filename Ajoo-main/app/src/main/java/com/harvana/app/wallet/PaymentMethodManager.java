package com.harvana.app.wallet;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Local, on-device storage for saved payment methods — deliberately
 * not synced to Firestore. This is autofill convenience for the
 * device that's actually posting listings, not something a person
 * needs on every device; each completed listing already carries its
 * own copy of the payment details it needs via P2PTradeRouter, so
 * nothing downstream depends on this list surviving a reinstall.
 */
public class PaymentMethodManager {

	private static final String PREFS_NAME = "payment_methods_prefs";
	private static final String KEY_METHODS = "methods";

	public static List<PaymentMethod> loadAll(Context context) {
		List<PaymentMethod> result = new ArrayList<>();
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		String raw = prefs.getString(KEY_METHODS, "[]");
		try {
			JSONArray arr = new JSONArray(raw);
			for (int i = 0; i < arr.length(); i++) {
				result.add(PaymentMethod.fromJson(arr.getJSONObject(i)));
			}
		} catch (JSONException e) {
			// Corrupt or unexpected data — fail to an empty list rather
			// than crash the Settings screen or the P2P listing flow.
		}
		return result;
	}

	public static void saveAll(Context context, List<PaymentMethod> methods) {
		JSONArray arr = new JSONArray();
		try {
			for (PaymentMethod pm : methods) {
				arr.put(pm.toJson());
			}
		} catch (JSONException e) {
			return;
		}
		context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
		.edit()
		.putString(KEY_METHODS, arr.toString())
		.apply();
	}

	public static void add(Context context, PaymentMethod method) {
		List<PaymentMethod> all = loadAll(context);
		if (method.id == null || method.id.isEmpty()) {
			method.id = UUID.randomUUID().toString();
		}
		// Only one default at a time — the first saved method becomes the
		// default automatically so there's always a sensible pre-selection
		// once at least one exists.
		if (all.isEmpty()) {
			method.isDefault = true;
		} else if (method.isDefault) {
			for (PaymentMethod existing : all) existing.isDefault = false;
		}
		all.add(method);
		saveAll(context, all);
	}

	public static void update(Context context, PaymentMethod method) {
		List<PaymentMethod> all = loadAll(context);
		if (method.isDefault) {
			for (PaymentMethod existing : all) existing.isDefault = false;
		}
		for (int i = 0; i < all.size(); i++) {
			if (all.get(i).id.equals(method.id)) {
				all.set(i, method);
				break;
			}
		}
		saveAll(context, all);
	}

	public static void remove(Context context, String id) {
		List<PaymentMethod> all = loadAll(context);
		boolean removedWasDefault = false;
		for (int i = 0; i < all.size(); i++) {
			if (all.get(i).id.equals(id)) {
				removedWasDefault = all.get(i).isDefault;
				all.remove(i);
				break;
			}
		}
		// If the default method was just removed, promote whichever one
		// is left first — CreateP2pListingActivity should always have a
		// sensible pre-selection whenever at least one method exists.
		if (removedWasDefault && !all.isEmpty()) {
			all.get(0).isDefault = true;
		}
		saveAll(context, all);
	}

	public static PaymentMethod getDefault(Context context) {
		for (PaymentMethod pm : loadAll(context)) {
			if (pm.isDefault) return pm;
		}
		List<PaymentMethod> all = loadAll(context);
		return all.isEmpty() ? null : all.get(0);
	}
}
