package com.harvanna.app.wallet;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import com.harvanna.app.ChainMath;
import com.harvanna.app.crypto.Ed25519;
import com.harvanna.app.sync.FirestoreSyncManager;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * Wallet helpers used by fragments / activities.
 * deriveAddressFromSeed accepts BOTH:
 *   - 12-word mnemonic text
 *   - Base64-encoded 32-byte raw seed
 */
public class WalletManager {
	
	private static final String PREF_NAME = "ajoo_secure_wallet";
	
	/**
	 * Delegates to WalletGenerator — single source of truth for the initial
	 * wallet document shape. Kept here too since AjooChainSdk and other call
	 * sites already reference WalletManager.createInitialWalletDoc().
	 */	
	public static Map<String, Object> createInitialWalletDoc(String address) {
		return WalletGenerator.createInitialWalletDoc(address);
	}
	
	public static String[] loadBip39Wordlist(Context context) {
		if (context != null) {
			try {
				InputStream is = context.getAssets().open("bip39_english.txt");
				BufferedReader reader = new BufferedReader(
					new InputStreamReader(is, StandardCharsets.UTF_8));
				java.util.List<String> words = new java.util.ArrayList<>();
				String line;
				while ((line = reader.readLine()) != null) {
					String trimmed = line.trim();
					if (!trimmed.isEmpty()) words.add(trimmed);
				}
				reader.close();
				if (!words.isEmpty()) return words.toArray(new String[0]);
			} catch (Exception ignored) {}
		}
		return new String[]{};
	}
	
	/**
	 * PRIMARY method used by WalletAssetFragmentActivity.
	 * Accepts a 12-word mnemonic OR a Base64 raw seed.
	 * Always returns a valid HV0x… address or null.
	 */	
	public static String deriveAddressFromSeed(String seed) {
		if (seed == null || seed.trim().isEmpty()) return null;
		
		String trimmed = seed.trim();
		
		// Path A — looks like a mnemonic (contains spaces / is 12 words)
		if (trimmed.contains(" ") || trimmed.split("\\s+").length >= 8) {
			return WalletGenerator.deriveAddressFromMnemonic(trimmed);
		}
		
		// Path B — try Base64-decoded 32-byte seed
		try {
			byte[] raw = Base64.decode(trimmed, Base64.NO_WRAP);
			if (raw != null && raw.length == 32) {
				return WalletGenerator.addressFromPublicKey(Ed25519.publicKeyFromSeed(raw));
			}
		} catch (Exception ignored) {}
		
		// Path C — treat plain string as mnemonic fallback (SHA-256 → address)
		return WalletGenerator.deriveAddressFromMnemonic(trimmed);
	}
	
	public static void saveKeysLocally(Context context, String address,
	String privateKey, String publicKey) {
		if (context == null || address == null) return;
		SecureWalletStorage.savePrivateKey(context, address, privateKey);
		SecureWalletStorage.savePublicKey(context, address, publicKey);
	}
	
	public static String loadPrivateKey(Context context, String address) {
		if (context == null || address == null) return null;
		return SecureWalletStorage.loadPrivateKey(context, address);
	}
	
	public static String getPublicKey(Context context, String address) {
		if (context == null || address == null) return null;
		return SecureWalletStorage.loadPublicKey(context, address);
	}
	
	public static void getAJCBalance(String address, FirestoreSyncManager.SyncCallback callback) {
		FirestoreSyncManager.getDocument("WALLETS", address, callback);
	}
	
	public static void getTokenBalance(String address, String contractAddress,
	FirestoreSyncManager.SyncCallback callback) {
		FirestoreSyncManager.getDocument("WALLETS", address, callback);
	}
	
	public static void updateTokenBalance(String address, String contractAddress,
	double newBalance,
	FirestoreSyncManager.SyncCallback callback) {
		FirestoreSyncManager.getDocument("WALLETS", address, new FirestoreSyncManager.SyncCallback() {
			@Override
			public void onSuccess(String json) {
				Map<String, Object> doc = FirestoreSyncManager.parseFirestoreDocument(json);
				if (doc == null) {
					callback.onError("Wallet not found");
					return;
				}
				
				java.util.List<Map<String, Object>> assets = asAssetList(doc.get("assets"));
				boolean found = false;
				for (Map<String, Object> asset : assets) {
					Object c = asset.get("contract");
					if (c != null && c.toString().equals(contractAddress)) {
						asset.put("balance", newBalance);
						found = true;
						break;
					}
				}
				if (!found) {
					Map<String, Object> newAsset = new HashMap<>();
					newAsset.put("contract", contractAddress);
					newAsset.put("symbol", "");
					newAsset.put("balance", newBalance);
					newAsset.put("decimals", 18);
					assets.add(newAsset);
				}
				
				doc.put("assets", assets);
				doc.put("lastActivity", System.currentTimeMillis());
				FirestoreSyncManager.updateDocument("WALLETS", address, doc, callback);
			}
			
			@Override
			public void onError(String error) {
				callback.onError(error);
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	private static java.util.List<Map<String, Object>> asAssetList(Object value) {
		java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
		if (value instanceof java.util.List) {
			for (Object item : (java.util.List<?>) value) {
				if (item instanceof Map) {
					result.add((Map<String, Object>) item);
				}
			}
		}
		return result;
	}
	
	/**
	 * Returns token balances as contract-address → balance, built from the
	 * "assets" array. Return type kept as Map<String,Object> for backward
	 * compatibility with existing call sites (CryptoWalletActivity,
	 * WalletAssetFragmentActivity) — only the internal source changed.
	 */	
	public static Map<String, Object> getTokenBalances(Map<String, Object> walletDoc) {
		Map<String, Object> result = new HashMap<>();
		if (walletDoc == null) return result;
		for (Map<String, Object> asset : asAssetList(walletDoc.get("assets"))) {
			Object c = asset.get("contract");
			if (c != null) {
				result.put(c.toString(), asset.get("balance"));
			}
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private static Map<String, Object> asObjectMap(Object value) {
		if (value instanceof Map) return (Map<String, Object>) value;
		return new HashMap<>();
	}
	
	public static void getAllBalances(String address, FirestoreSyncManager.SyncCallback callback) {
		FirestoreSyncManager.getDocument("WALLETS", address, callback);
	}
	
	public static void incrementNonce(String address, FirestoreSyncManager.SyncCallback callback) {
		FirestoreSyncManager.getDocument("WALLETS", address, new FirestoreSyncManager.SyncCallback() {
			@Override
			public void onSuccess(String json) {
				Map<String, Object> doc = FirestoreSyncManager.parseFirestoreDocument(json);
				if (doc != null) {
					long nonce = doc.get("nonce") instanceof Number
					? ((Number) doc.get("nonce")).longValue() : 0L;
					doc.put("nonce", nonce + 1);
					FirestoreSyncManager.updateDocument("WALLETS", address, doc, callback);
				} else {
					callback.onError("Wallet not found");
				}
			}
			
			@Override
			public void onError(String error) {
				callback.onError(error);
			}
		});
	}
	
	public static double getNativeBalance(Map<String, Object> walletDoc) {
		if (walletDoc == null) return 0.0;
		return ChainMath.toDouble(walletDoc.get("balance_ajc"));
	}
	
	public static void getTokenBalances(String address, FirestoreSyncManager.SyncCallback callback) {
		getAllBalances(address, callback);
	}
	
	public static double getTokenBalance(Map<String, Object> walletDoc, String contractAddress) {
		if (walletDoc == null || contractAddress == null || contractAddress.trim().isEmpty()) {
			return 0.0;
		}
		return ChainMath.toDouble(getTokenBalances(walletDoc).get(contractAddress.trim()));
	}
	
	public static Map<String, Object> getBalances(Map<String, Object> walletDoc) {
		return getTokenBalances(walletDoc);
	}
	
	/**
	 * Single source of truth for the active wallet address.
	 * Used by Receive sheet, Send screen, etc.
	 *
	 * THE FUTURE MULTI-WALLET EXTENSION POINT: today this is one global
	 * "active_wallet_address" preference - one wallet per signed-in user.
	 * When multi-wallet-seed login lands, this is the one place to change:
	 * swap the flat SharedPreferences read for a lookup against whichever
	 * wallet the user has switched to (e.g. keyed by a wallet id/index),
	 * and every caller listed above inherits the new behavior automatically
	 * since none of them read "active_wallet_address" directly - they all
	 * go through this method.
	 *
	 * Screens that need to act on a SPECIFIC address regardless of which
	 * wallet is "active" (e.g. SendAssetActivity's group-wallet transfer,
	 * launched with a "fromWalletAddress" intent extra) intentionally
	 * bypass this method entirely rather than calling it and overriding
	 * the result - they never read or write active_wallet_address, so
	 * they stay unaffected no matter how this method's internals change.
	 * Key/mnemonic storage (SecureWalletStorage) is already keyed by
	 * address rather than by "the active one", so it needs no changes
	 * either - only the address-selection logic in this one method does.
	 */	
	public static String getActiveWalletAddress(android.content.Context context) {
		if (context == null) return "";
		
		android.content.SharedPreferences prefs =
		context.getSharedPreferences("ajc_wallet_prefs", android.content.Context.MODE_PRIVATE);
		
		String address = prefs.getString("active_wallet_address", "");
		if (address == null || address.trim().isEmpty()) {
			address = prefs.getString("wallet_address", "");
		}
		if (address == null || address.trim().isEmpty()) {
			address = prefs.getString("active_address", "");
		}
		if (address == null || address.trim().isEmpty()) {
			android.content.SharedPreferences old =
			context.getSharedPreferences("walletsp", android.content.Context.MODE_PRIVATE);
			address = old.getString("wallet_address", "");
		}
		return address != null ? address.trim() : "";
	}
	
}