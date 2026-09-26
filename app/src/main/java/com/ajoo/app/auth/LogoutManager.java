package com.ajoo.app.auth;

import android.content.Context;

/**
* Centralized logout — clears every persistent store this app writes to.
* Built by auditing every getSharedPreferences() call site across the
* whole app, plus every wrapper class with its own file (WalletCache,
* SecureWalletStorage, MiningPreferences), including two files
* (ajoo_secure_wallet_enc, ajoo_secure_wallet — the actual private key
* storage) that don't show up in a literal-string search since they're
* referenced through constants, not literals, at their call sites.
*/
public final class LogoutManager {
	
	private LogoutManager() {}
	
	public static void logout(Context context) {
		Context appContext = context.getApplicationContext();
		
		// 1. Stop anything still running against the wallet we're about to
		//    wipe — otherwise a stray background callback could write fresh
		//    data back into a prefs file we just cleared.
		com.ajoo.app.utils.IncomingTxWatcher.stop();
		
		// 2. Every SharedPreferences file this app writes to, cleared
		//    entirely rather than removing individual known keys — safer
		//    against anything not accounted for below.
		clearPrefs(appContext, "authsp");               // Firebase Auth identity — the actual login-state file SplashScreenActivity checks
		clearPrefs(appContext, "ajc_wallet_prefs");      // active wallet address + metadata
		clearPrefs(appContext, "walletsp");              // legacy wallet prefs, still read as fallback in places
		clearPrefs(appContext, "ajc_mining_log");        // persisted mining log lines
		clearPrefs(appContext, "ajoo_mining_prefs");     // mining settings (thread count, solo mode, wallet address)
		clearPrefs(appContext, "ajoo_wallet_cache");     // balance cache + tx history cache + pending tx store (WalletCache / PendingTxStore share this file)
		clearPrefs(appContext, "currencysp");            // display currency preference
		clearPrefs(appContext, "groupsp");               // group-related data
		clearPrefs(appContext, "ajoo_secure_wallet_enc"); // CRITICAL — current encrypted mnemonic/private/public keys
		clearPrefs(appContext, "ajoo_secure_wallet");     // CRITICAL — legacy pre-migration plaintext keys, if any never got migrated
		
		// 3. Firebase Auth SDK's own internal session state — separate
		//    from every SharedPreferences file above, and not touched by
		//    any of them.
		try {
			com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
		} catch (Exception ignored) {}
		
		// 4. Firebase ID token cache — has its own clear() method, same one
		//    this button already called before.
		com.ajoo.app.sync.AuthTokenProvider.clear();
	}
	
	
	private static void clearPrefs(Context context, String prefsName) {
		context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
		.edit()
		.clear()
		.apply();
	}
}
