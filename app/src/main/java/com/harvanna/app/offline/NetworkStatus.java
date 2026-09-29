package com.harvanna.app.offline;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

/**
 * Two things every offline-aware screen needs:
 *   1. isOnline() — a direct connectivity check before even attempting a
 *      call, so a screen can go straight to cached data instead of waiting
 *      out a call it already knows will fail.
 *   2. isConnectivityError(String) — classifies an error message that
 *      already came back from a failed call. This is the actual fix for
 *      the splash-screen bug: every *.java manager in this app
 *      (UserProfileManager, GroupFirestoreManager, etc.) follows the same
 *      OkHttp pattern — onFailure(IOException) produces an error string
 *      prefixed "Network error: ", while a real server response (including
 *      a genuine 404 "this profile doesn't exist") produces "HTTP <code>: ".
 *      Only the former is a connectivity problem; treating both the same
 *      is what sent an offline *existing* user to account creation instead
 *      of into the app with cached data.
 */
public final class NetworkStatus {

    private NetworkStatus() {}

    public static boolean isOnline(Context context) {
        if (context == null) return true; // fail open — don't block on a bad context
        try {
            ConnectivityManager cm = (ConnectivityManager)
                    context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return true;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.net.Network network = cm.getActiveNetwork();
                if (network == null) return false;
                NetworkCapabilities caps = cm.getNetworkCapabilities(network);
                return caps != null
                        && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
            } else {
                @SuppressWarnings("deprecation")
                NetworkInfo info = cm.getActiveNetworkInfo();
                return info != null && info.isConnected();
            }
        } catch (Exception e) {
            return true; // fail open — a permissions/OS quirk shouldn't block the whole app
        }
    }

    /** True for a genuine connectivity failure (timeout, DNS, no route,
     *  connection refused) as opposed to a real server response. Two error
     *  formats exist in this app and both are covered here:
     *    - UserProfileManager/GroupFirestoreManager (OkHttp): prefix
     *      "Network error: " on IOException, "HTTP <code>: " on a real
     *      response.
     *    - AjooNodeClient (HttpURLConnection): raw Java exception message
     *      on a transport failure (e.g. "Unable to resolve host ...",
     *      "failed to connect to ...", "Connection timed out", or its own
     *      generic "network error" fallback when getMessage() is null),
     *      "HTTP <code>: " on a real response.
     *  Since "HTTP " is the one consistent, unambiguous marker of a real
     *  server response across both formats, anything that ISN'T that is
     *  treated as connectivity-related by default — matching either format
     *  exactly would mean maintaining two parallel keyword lists that drift
     *  out of sync the next time a client's exception wording changes. */
    public static boolean isConnectivityError(String error) {
        if (error == null) return false;
        String trimmed = error.trim();
        if (trimmed.isEmpty()) return false;
        if (trimmed.startsWith("HTTP ")) return false;
        // App-level validation errors thrown before any network call is
        // even made (missing/invalid input, not signed in, etc.) shouldn't
        // be treated as "offline" — a cached fallback wouldn't fix them and
        // showing stale data in response to a validation error would be
        // actively misleading.
        String lower = trimmed.toLowerCase(java.util.Locale.US);
        if (lower.startsWith("missing") || lower.startsWith("invalid")
                || lower.startsWith("not signed in") || lower.contains("permission")) {
            return false;
        }
        return true;
    }
}
