package com.harvana.app.auth;

import android.content.Context;

import java.io.File;
import java.security.KeyStore;
import java.util.Enumeration;

/**
 * The one sign-out. Every way out of an account (Settings, the side menu, email check, signed in on another phone) ends here,
 * and nothing of that account is left on the phone:
 * <ul>
 *   <li>every SharedPreferences file the app has written, found on disk rather than from a list (keys, wallets, settings,
 *       theme, currency, added coins, caches);</li>
 *   <li>the Android Keystore keys that lock the wallet and the offline cache;</li>
 *   <li>the app's databases and its own files and cache folders;</li>
 *   <li>scheduled background work, shown notifications, and every cache held in memory;</li>
 *   <li>the Firebase session and its ID token.</li>
 * </ul>
 * Google libraries' own bookkeeping (Firebase installation id, WorkManager's queue) is left in place: it holds nothing of the
 * person and the libraries break if it vanishes under them.
 */
public final class LogoutManager {

	private LogoutManager() {}

	public static void logout(Context context) {
		Context app = context.getApplicationContext();

		// 1. Stop everything that could write the old account back while it is being cleared.
		quietly(() -> com.harvana.app.utils.IncomingTxWatcher.stop());
		quietly(() -> com.harvana.app.utils.PresenceHeartbeat.stop());
		quietly(() -> androidx.work.WorkManager.getInstance(app).cancelAllWork());
		quietly(() -> androidx.core.app.NotificationManagerCompat.from(app).cancelAll());

		// 2. The Firebase session.
		quietly(() -> com.google.firebase.auth.FirebaseAuth.getInstance().signOut());
		quietly(com.harvana.app.sync.AuthTokenProvider::clear);

		// 3. Every preferences file on disk.
		clearAllPrefs(app);

		// 4. The keys that lock the wallet and the offline cache.
		clearKeystore();

		// 5. Databases, files and cache folders.
		quietly(() -> com.harvana.app.offline.OfflineDatabaseHelper.wipe(app));
		for (String db : app.databaseList()) {
			if (isLibrary(db)) continue;
			quietly(() -> app.deleteDatabase(db));
		}
		quietly(com.harvana.app.sync.NodeReadCache::clear);
		File[] files = app.getFilesDir().listFiles();
		if (files != null) for (File f : files) if (!isLibrary(f.getName()) && !"node_reads".equals(f.getName())) delete(f);
		delete(app.getCacheDir(), false);
		quietly(() -> { File ext = app.getExternalFilesDir(null); if (ext != null) delete(ext, false); });

		// 6. The dApp browser: site storage, cookies and connected-site data.
		quietly(() -> android.webkit.WebStorage.getInstance().deleteAllData());
		quietly(() -> { android.webkit.CookieManager.getInstance().removeAllCookies(null); android.webkit.CookieManager.getInstance().flush(); });

		// 7. What the app holds in memory.
		quietly(com.harvana.app.trust.ProfileTrust::forgetAll);
		quietly(com.harvana.app.trust.NodeBadges::forgetAll);
		quietly(com.harvana.app.group.MemberNameResolver::forgetAll);
		quietly(com.harvana.app.wallet.PriceChart::forgetAll);
		quietly(com.harvana.app.group.NodeSettingsPanel::forgetAll);
		quietly(com.harvana.app.wallet.CustomAssets::forgetAll);
		quietly(com.harvana.app.milestone.MilestoneLoader::invalidate);
	}

	/** Every SharedPreferences file in the app's folder, emptied (the in-memory copy too) and then deleted. */
	private static void clearAllPrefs(Context app) {
		File dir = new File(app.getApplicationInfo().dataDir, "shared_prefs");
		File[] files = dir.listFiles();
		if (files == null) return;
		for (File f : files) {
			String file = f.getName();
			if (!file.endsWith(".xml")) continue;
			String name = file.substring(0, file.length() - 4);
			if (isLibrary(name)) continue;
			quietly(() -> app.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit());
			if (android.os.Build.VERSION.SDK_INT >= 24) quietly(() -> app.deleteSharedPreferences(name));
		}
	}

	/** Every key this app made in the Android Keystore (only the app can see its own). */
	private static void clearKeystore() {
		try {
			KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
			ks.load(null);
			Enumeration<String> aliases = ks.aliases();
			java.util.List<String> all = new java.util.ArrayList<>();
			while (aliases.hasMoreElements()) all.add(aliases.nextElement());
			for (String a : all) quietly(() -> ks.deleteEntry(a));
		} catch (Exception ignored) { }
	}

	private static boolean isLibrary(String name) {
		return name.startsWith("com.google.") || name.startsWith("androidx.") || name.startsWith("PersistedInstallation")
				|| name.startsWith("FirebaseHeartBeat") || name.startsWith("generatefid") || name.startsWith("firestore.")
				|| name.startsWith("WebView") || name.equals("app_webview");
	}

	private static void delete(File f) { delete(f, true); }

	private static void delete(File f, boolean self) {
		if (f == null) return;
		File[] kids = f.listFiles();
		if (kids != null) for (File k : kids) delete(k, true);
		//noinspection ResultOfMethodCallIgnored
		if (self) f.delete();
	}

	private interface Step { void run() throws Exception; }

	private static void quietly(Step s) {
		try { s.run(); } catch (Throwable ignored) { }
	}
}
