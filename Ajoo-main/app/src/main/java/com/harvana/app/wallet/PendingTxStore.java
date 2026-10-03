package com.harvana.app.wallet;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
* Local-only ledger for transactions the app has just submitted, before
* (or instead of) confirmation. The node/Firestore never persist pending
* or failed transactions at all — FirestoreBridgeService explicitly only
* writes CONFIRMED records, and a rejected send leaves no server-side
* trace whatsoever. This store is what makes "pending"/"failed" visible
* in history at all, purely client-side, until the real confirmed record
* (if any) arrives and this entry is pruned.
*/
public final class PendingTxStore {
	
	private static final long STALE_THRESHOLD_MS = 10 * 60 * 1000L; // 10 minutes
	
	private PendingTxStore() {}
	
	private static String cacheKey(String walletAddress) {
		return "pending_" + walletAddress;
	}
	
	/** Call the instant a send succeeds (server returned PENDING). */
	public static void addPending(Context context, String walletAddress, String txHash,
	String recipient, double amount, String symbol) {
		List<HashMap<String, Object>> current = loadAll(context, walletAddress);
		
		HashMap<String, Object> entry = new HashMap<>();
		entry.put("txHash", txHash);
		entry.put("senderAddress", walletAddress);
		entry.put("recipientAddress", recipient);
		entry.put("value", amount);
		entry.put("tokenSymbol", symbol != null ? symbol : com.harvana.app.CoinInfo.ticker());
		entry.put("status", "pending");
		entry.put("timestamp", System.currentTimeMillis());
		
		current.add(0, entry); // newest first
		WalletCache.saveList(context, cacheKey(walletAddress), current);
	}
	
	/** Call the instant a send fails — no txHash exists yet, so generate a local-only placeholder. */
	public static void addFailed(Context context, String walletAddress,
	String recipient, double amount, String symbol, String errorMessage) {
		List<HashMap<String, Object>> current = loadAll(context, walletAddress);
		
		HashMap<String, Object> entry = new HashMap<>();
		entry.put("txHash", "local-failed-" + System.currentTimeMillis());
		entry.put("senderAddress", walletAddress);
		entry.put("recipientAddress", recipient);
		entry.put("value", amount);
		entry.put("tokenSymbol", symbol != null ? symbol : com.harvana.app.CoinInfo.ticker());
		entry.put("status", "failed");
		entry.put("errorMessage", errorMessage != null ? errorMessage : "Send failed");
		entry.put("timestamp", System.currentTimeMillis());
		
		current.add(0, entry);
		WalletCache.saveList(context, cacheKey(walletAddress), current);
	}
	
	/** Every locally-tracked pending/failed entry for this wallet. */
	public static List<HashMap<String, Object>> loadAll(Context context, String walletAddress) {
		List<HashMap<String, Object>> loaded = WalletCache.loadList(context, cacheKey(walletAddress));
		return loaded != null ? loaded : new ArrayList<>();
	}
	
	/**
* Call after loading confirmed history — removes any locally-tracked
* pending entry whose txHash now appears in the confirmed set, so it
* doesn't show twice once the real record lands.
*/	
	public static void pruneConfirmed(Context context, String walletAddress,
	List<String> confirmedTxHashes) {
		List<HashMap<String, Object>> current = loadAll(context, walletAddress);
		if (current.isEmpty() || confirmedTxHashes == null || confirmedTxHashes.isEmpty()) return;
		
		List<HashMap<String, Object>> remaining = new ArrayList<>();
		for (HashMap<String, Object> entry : current) {
			String hash = String.valueOf(entry.get("txHash"));
			if (!confirmedTxHashes.contains(hash)) {
				remaining.add(entry);
			}
		}
		WalletCache.saveList(context, cacheKey(walletAddress), remaining);
	}
	/**
* Flips an existing pending entry to failed status IN PLACE (keeping
* its real txHash), rather than creating a new synthetic entry like
* addFailed() does. Use only after checking the node directly and
* confirming the transaction is genuinely gone (NOT_FOUND) — never
* based on elapsed time alone.
*/	
	public static void markFailed(Context context, String walletAddress, String txHash, String reason) {
		List<HashMap<String, Object>> current = loadAll(context, walletAddress);
		boolean changed = false;
		for (HashMap<String, Object> entry : current) {
			if (txHash.equals(String.valueOf(entry.get("txHash")))) {
				entry.put("status", "failed");
				entry.put("errorMessage", reason != null ? reason : "Transaction not found on node");
				changed = true;
			}
		}
		if (changed) {
			WalletCache.saveList(context, cacheKey(walletAddress), current);
		}
	}
	
	/**
* Checks every pending entry older than STALE_THRESHOLD_MS against the
* real node. Only a definitive NOT_FOUND response marks it failed —
* PENDING (still genuinely waiting) and network errors both leave the
* entry untouched, so nothing gets wrongly flagged on a guess.
*/	
	public static void checkStaleAndMarkFailed(Context context, String walletAddress) {
		List<HashMap<String, Object>> current = loadAll(context, walletAddress);
		long now = System.currentTimeMillis();
		
		for (HashMap<String, Object> entry : current) {
			String status = String.valueOf(entry.get("status"));
			if (!"pending".equalsIgnoreCase(status)) continue;
			
			Object tsObj = entry.get("timestamp");
			long ts = tsObj instanceof Number ? ((Number) tsObj).longValue() : now;
			if (now - ts < STALE_THRESHOLD_MS) continue; // still within normal window
			
			String txHash = String.valueOf(entry.get("txHash"));
			if (txHash == null || txHash.startsWith("local-failed-")) continue;
			
			com.harvana.app.sync.HarvanaNodeClient.getTxStatus(txHash,
			new com.harvana.app.sync.HarvanaNodeClient.JsonCallback() {
				@Override
				public void onSuccess(org.json.JSONObject json) {
					String nodeStatus = json.optString("status", "");
					if ("NOT_FOUND".equalsIgnoreCase(nodeStatus)) {
						markFailed(context, walletAddress, txHash,
						"Transaction no longer found on node — likely lost before confirming");
					}
					// PENDING → still genuinely waiting, leave it.
					// CONFIRMED → will be pruned normally on next history refresh.
				}
				
				@Override
				public void onError(String error) {
					// Network issue — don't guess, just skip this round
				}
			});
		}
	}
}
