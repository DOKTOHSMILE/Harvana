package com.harvana.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.harvana.app.ChainConstants;
import com.harvana.app.sync.FirestoreSyncManager;
import com.harvana.app.wallet.WalletManager;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
* Polls TRANSACTIONS for incoming transfers (Sketchware-safe, no native Firestore).
*/
public final class IncomingTxWatcher {
	
	private static final String TAG = "IncomingTxWatcher";
	private static final String PREF = "harvana_incoming_tx";
	private static final String KEY_LAST_TS = "last_seen_ts";
	private static final long POLL_MS = 60_000L;
	
	private static final Handler handler = new Handler(Looper.getMainLooper());
	private static final Set<String> notified = new HashSet<>();
	private static Context appCtx;
	private static String wallet;
	private static boolean running;
	
	private static final Runnable pollRunnable = new Runnable() {
		@Override
		public void run() {
			if (!running || appCtx == null) return;
			pollOnce();
			handler.postDelayed(this, POLL_MS);
		}
	};
	
	private IncomingTxWatcher() {}
	
	public static synchronized void start(Context context) {
		// Retired: this polled the old Firestore transaction copy. Incoming money is now announced from the node's
		// confirmed history by notify/WalletTxNotifier (foreground sync, background worker and the live socket).
		Log.i(TAG, "IncomingTxWatcher is retired; WalletTxNotifier handles wallet notifications.");
	}
	
	public static synchronized void stop() {
		running = false;
		handler.removeCallbacks(pollRunnable);
	}
	
	private static void pollOnce() {
		final String me = wallet;
		if (me == null) return;
		
		final SharedPreferences prefs = appCtx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
		final long lastSeen = prefs.getLong(KEY_LAST_TS, System.currentTimeMillis() - 60_000L);
		
		FirestoreSyncManager.getWalletTransactionHistory(me, new FirestoreSyncManager.CollectionCallback() {
			@Override
			public void onSuccess(List<Map<String, Object>> docs) {
				if (docs == null) return;
				long newest = lastSeen;
				for (Map<String, Object> doc : docs) {
					if (doc == null) continue;
					
					String to = str(doc.get("to"));
					if (!me.equalsIgnoreCase(to)) continue;
					
					String from = str(doc.get("from"));
					if (me.equalsIgnoreCase(from)) continue; // skip outgoing
					
					String txHash = str(doc.get("txHash"));
					if (txHash.isEmpty()) txHash = str(doc.get("_documentId"));
					if (txHash.isEmpty()) continue;
					
					long ts = toLong(doc.get("timestamp"));
					if (ts > 0 && ts <= lastSeen) continue;
					
					if (!markNotifiedIfNew(txHash)) continue; // already shown via another path
					if (ts > newest) newest = ts;
					
					double amount = toDouble(doc.get("value"));
					String symbol = str(doc.get("tokenSymbol"));
					if (symbol.isEmpty()) symbol = com.harvana.app.CoinInfo.ticker();
					String amountStr = String.format(java.util.Locale.US, "%.4f", amount);
					
					TransactionNotification.showReceived(appCtx, amountStr, symbol, from, txHash);
					// Never log the amount, symbol or sender address: the notification is already
					// shown to the user. The log line only records the event for diagnostics.
					Log.i(TAG, "Incoming tx notification shown");
				}
				if (newest > lastSeen) {
					prefs.edit().putLong(KEY_LAST_TS, newest).apply();
				}
			}
			
			@Override
			public void onError(String error) {
				Log.w(TAG, "Poll failed: " + error);
			}
		});
	}
	
	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
	
	private static long toLong(Object o) {
		if (o instanceof Number) return ((Number) o).longValue();
		try { return Long.parseLong(String.valueOf(o)); } catch (Exception e) { return 0L; }
	}
	
	private static double toDouble(Object o) {
		if (o instanceof Number) return ((Number) o).doubleValue();
		try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0.0; }
	}
	/**
* Cross-system dedup — both this watcher's own poll and
* WalletController's live socket-triggered notification call this
* before actually showing anything, so the same incoming transaction
* can never trigger both a system notification and an in-app banner.
* Whichever path checks first wins; the other silently skips.
*/	
	public static synchronized boolean markNotifiedIfNew(String txHash) {
		if (txHash == null || txHash.isEmpty()) return false;
		if (notified.contains(txHash)) return false;
		notified.add(txHash);
		if (notified.size() > 200) notified.clear();
		return true;
	}
	
}
