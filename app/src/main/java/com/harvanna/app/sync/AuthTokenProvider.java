package com.harvanna.app.sync;

import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import java.util.concurrent.TimeUnit;

/**
 * Provides Firebase Auth ID tokens for Firestore REST calls.
 * Cached for a short time; force-refresh when server returns 401.
 */
public final class AuthTokenProvider {

	private static final String TAG = "AuthTokenProvider";
	private static final long CACHE_MS = 45 * 60 * 1000L; // 45 minutes

	private static String cachedToken;
	private static long cachedAt;

	private AuthTokenProvider() {}

	/** Current Firebase uid, or null if signed out. */
	public static String getUid() {
		try {
			FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
			return user != null ? user.getUid() : null;
		} catch (Throwable t) {
			return null;
		}
	}

	public static boolean isSignedIn() {
		return getUid() != null;
	}

	/**
	 * Returns a valid ID token, or null if not signed in / Auth unavailable.
	 * Safe to call from a background thread (OkHttp callback thread is fine).
	 */
	public static String getIdToken(boolean forceRefresh) {
		try {
			FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
			if (user == null) {
				cachedToken = null;
				return null;
			}

			long now = System.currentTimeMillis();
			if (!forceRefresh && cachedToken != null && (now - cachedAt) < CACHE_MS) {
				return cachedToken;
			}

			GetTokenResult result = Tasks.await(
					user.getIdToken(forceRefresh),
					15,
					TimeUnit.SECONDS
			);
			if (result == null || result.getToken() == null) {
				return null;
			}
			cachedToken = result.getToken();
			cachedAt = now;
			return cachedToken;
		} catch (Throwable t) {
			Log.w(TAG, "getIdToken failed: " + t.getMessage());
			return null;
		}
	}

	/** Call on logout. */
	public static void clear() {
		cachedToken = null;
		cachedAt = 0L;
	}
}