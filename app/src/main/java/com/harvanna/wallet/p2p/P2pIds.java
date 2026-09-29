package com.harvanna.wallet.p2p;

import org.json.JSONObject;

/** Node responses historically mixed `id` / `listingId` / `orderId`. */
public final class P2pIds {

	private P2pIds() {}

	public static String first(JSONObject result, String... keys) {
		if (result == null) return "";
		for (String key : keys) {
			String value = result.optString(key, "");
			if (value != null && !value.trim().isEmpty() && !"null".equalsIgnoreCase(value)) {
				return value.trim();
			}
		}
		return "";
	}

	public static String listingId(JSONObject result) {
		return first(result, "listingId", "id");
	}

	public static String orderId(JSONObject result) {
		String order = first(result, "orderId");
		if (!order.isEmpty()) return order;
		String id = first(result, "id");
		String listing = first(result, "listingId");
		if (!id.isEmpty() && !id.equals(listing)) return id;
		return "";
	}
}
