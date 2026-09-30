package com.harvanna.app.wallet;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
* Local, non-sensitive cache of wallet display data (balance, assets) —
* NOT for private keys/mnemonics, see SecureWalletStorage for that.
*
* Lets the UI render the last known wallet state instantly on open, before
* the real Firestore fetch completes, instead of showing an empty/zero
* state for a few seconds every time. Save fresh data here every time a
* real fetch succeeds; load it synchronously the moment a screen opens.
*/
public final class WalletCache {
	
	private static final String PREF_NAME = "harvanna_wallet_cache";
	
	private WalletCache() {}
	
	/**
* Saves the latest known wallet document to local cache. Call this
* every time a fresh wallet read succeeds from Firestore.
*
* @param context   any context
* @param address   wallet address, used as the cache key
* @param walletDoc the wallet fields to cache (balance_harv, assets, etc.)
*/	
	public static void save(Context context, String address, Map<String, Object> walletDoc) {
		if (context == null || address == null || walletDoc == null) return;
		
		try {
			JSONObject json = new JSONObject();
			for (Map.Entry<String, Object> entry : walletDoc.entrySet()) {
				json.put(entry.getKey(), toJson(entry.getValue()));
			}
			json.put("_cachedAtMs", System.currentTimeMillis());
			
			context.getApplicationContext()
			.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit()
			.putString(address, json.toString())
			.apply();
		} catch (Exception e) {
			// Cache write failure is non-fatal — worst case, next open just
			// won't have instant data, nothing crashes
		}
	}
	
	/**
* Instantly (synchronously, no network call) returns the last cached
* wallet document for this address, or null if nothing was ever cached.
* Call this the moment a wallet screen opens, before kicking off the
* real Firestore fetch.
*
* @param context any context
* @param address wallet address to look up
* @return the cached fields, or null if no cache exists yet
*/	
	public static Map<String, Object> load(Context context, String address) {
		if (context == null || address == null) return null;
		
		String raw = context.getApplicationContext()
		.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
		.getString(address, null);
		
		if (raw == null) return null;
		
		try {
			JSONObject json = new JSONObject(raw);
			Map<String, Object> result = new HashMap<>();
			Iterator<String> keys = json.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				result.put(key, fromJson(json.get(key)));
			}
			return result;
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
* Returns how old the cached data is, in milliseconds — useful for
* showing something like "Updated 12s ago" while a fresh fetch is
* still in flight, so stale data never looks like it's live.
*
* @param context any context
* @param address wallet address to check
* @return age in ms, or -1 if there's no cache at all
*/	
	public static long ageMs(Context context, String address) {
		Map<String, Object> cached = load(context, address);
		if (cached == null || !(cached.get("_cachedAtMs") instanceof Number)) return -1L;
		long cachedAt = ((Number) cached.get("_cachedAtMs")).longValue();
		return System.currentTimeMillis() - cachedAt;
	}
	
	// ── JSON <-> Java conversion ─────────────────────────────────────────────
	
	private static Object toJson(Object value) throws Exception {
		if (value instanceof List) {
			JSONArray arr = new JSONArray();
			for (Object item : (List<?>) value) arr.put(toJson(item));
			return arr;
		}
		if (value instanceof Map) {
			JSONObject obj = new JSONObject();
			for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
				obj.put(String.valueOf(entry.getKey()), toJson(entry.getValue()));
			}
			return obj;
		}
		return value != null ? value : JSONObject.NULL;
	}
	
	private static Object fromJson(Object value) throws Exception {
		if (value instanceof JSONArray) {
			JSONArray arr = (JSONArray) value;
			List<Object> list = new ArrayList<>();
			for (int i = 0; i < arr.length(); i++) list.add(fromJson(arr.get(i)));
			return list;
		}
		if (value instanceof JSONObject) {
			JSONObject obj = (JSONObject) value;
			Map<String, Object> map = new HashMap<>();
			Iterator<String> keys = obj.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				map.put(key, fromJson(obj.get(key)));
			}
			return map;
		}
		if (value == JSONObject.NULL) return null;
		return value;
	}
	
	/**
* Saves any list of maps to cache under an arbitrary key — used for the
* coin list (Assets tab) and transaction history (Activity tab), which
* aren't single wallet documents like save()/load() handle.
*
* @param context  any context
* @param cacheKey unique key for this cached list, e.g. "coins_" + address
* @param list     the list to cache
*/	
	public static void saveList(Context context, String cacheKey, List<? extends Map<String, Object>> list) {
		if (context == null || cacheKey == null || list == null) return;
		
		try {
			JSONArray arr = new JSONArray();
			for (Map<String, Object> item : list) {
				JSONObject obj = new JSONObject();
				for (Map.Entry<String, Object> entry : item.entrySet()) {
					obj.put(entry.getKey(), toJson(entry.getValue()));
				}
				arr.put(obj);
			}
			
			JSONObject wrapper = new JSONObject();
			wrapper.put("_list", arr);
			wrapper.put("_cachedAtMs", System.currentTimeMillis());
			
			context.getApplicationContext()
			.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit()
			.putString(cacheKey, wrapper.toString())
			.apply();
		} catch (Exception e) {
			// Non-fatal — next open just won't have instant data
		}
	}
	
	/**
* Instantly (no network) returns a previously cached list, or null if
* nothing was ever cached under this key.
*
* @param context  any context
* @param cacheKey the same key used in saveList()
* @return the cached list, or null
*/	
	public static List<HashMap<String, Object>> loadList(Context context, String cacheKey) {
		if (context == null || cacheKey == null) return null;
		
		String raw = context.getApplicationContext()
		.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
		.getString(cacheKey, null);
		
		if (raw == null) return null;
		
		try {
			JSONObject wrapper = new JSONObject(raw);
			JSONArray arr = wrapper.getJSONArray("_list");
			
			List<HashMap<String, Object>> result = new ArrayList<>();
			for (int i = 0; i < arr.length(); i++) {
				JSONObject obj = arr.getJSONObject(i);
				HashMap<String, Object> item = new HashMap<>();
				Iterator<String> keys = obj.keys();
				while (keys.hasNext()) {
					String key = keys.next();
					item.put(key, fromJson(obj.get(key)));
				}
				result.add(item);
			}
			return result;
		} catch (Exception e) {
			return null;
		}
	}
	
}
