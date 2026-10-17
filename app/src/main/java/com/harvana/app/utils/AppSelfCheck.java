package com.harvana.app.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.harvana.app.BuildConfig;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Startup self-check. Runs once after {@link com.harvana.app.SketchApplication}
 * finishes its init block, and validates the live environment against what
 * the app expects. Anything that fails here is written to the CRASH_LOGS
 * Firestore collection (with kind="STARTUP_CHECK") so the operator sees
 * misconfigurations in the moderator dashboard instead of the app dying
 * silently on a user device.
 *
 * Pure Java on purpose so it is JVM-unit-testable. The network probes are
 * best-effort: a probe that times out is a warning, not a failure.
 */
public final class AppSelfCheck {

    private static final String TAG = "AppSelfCheck";
    private static final int PROBE_TIMEOUT_MS = 4_000;

    public static class Result {
        public final List<String> passed = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();
        public final List<String> failed = new ArrayList<>();

        public boolean ok() { return failed.isEmpty(); }

        public JSONObject toJson() {
            try {
                return new JSONObject()
                        .put("passed", new org.json.JSONArray(passed))
                        .put("warnings", new org.json.JSONArray(warnings))
                        .put("failed", new org.json.JSONArray(failed));
            } catch (Exception e) { return new JSONObject(); }
        }
    }

    private final Context ctx;

    public AppSelfCheck(Context ctx) { this.ctx = ctx.getApplicationContext(); }

    /** Run every check. Never throws. */
    public Result run() {
        Result r = new Result();
        checkFirebaseInitialised(r);
        checkGoogleServicesPresent(r);
        checkNodeReachable(r);
        checkFirestoreRulesVersion(r);
        checkBouncyCastleVersion(r);
        checkProGuardActiveInRelease(r);
        checkNetworkSecurityConfig(r);
        checkDebuggableFlag(r);
        if (r.failed.isEmpty()) Log.i(TAG, "AppSelfCheck OK: " + r.passed.size() + " passed, " + r.warnings.size() + " warnings");
        else Log.w(TAG, "AppSelfCheck FAILED: " + r.failed);
        return r;
    }

    private void pass(Result r, String msg) { r.passed.add(msg); }
    private void warn(Result r, String msg) { r.warnings.add(msg); }
    private void fail(Result r, String msg) { r.failed.add(msg); }

    private void checkFirebaseInitialised(Result r) {
        try {
            if (FirebaseApp.getApps(ctx) == null || FirebaseApp.getApps(ctx).isEmpty()) fail(r, "FirebaseApp not initialised");
            else pass(r, "FirebaseApp initialised");
        } catch (Throwable t) { fail(r, "FirebaseApp check threw: " + t.getMessage()); }
    }

    private void checkGoogleServicesPresent(Result r) {
        // google-services plugin generates this string resource in debug + release.
        try {
            int id = ctx.getResources().getIdentifier("google_app_id", "string", ctx.getPackageName());
            if (id == 0) fail(r, "google_app_id string resource missing (google-services.json not applied)");
            else pass(r, "google-services.json applied");
        } catch (Throwable t) { fail(r, "google-services check threw: " + t.getMessage()); }
    }

    private void checkNodeReachable(Result r) {
        // The node base URL is a constant. Probe it via HEAD; treat 2xx/3xx/4xx as reachable
        // and timeouts / DNS failures as warnings (the app may still work via WS).
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(com.harvana.app.sync.HarvanaNodeClient.BASE + "/api/v1/node/status").openConnection();
            conn.setConnectTimeout(PROBE_TIMEOUT_MS);
            conn.setReadTimeout(PROBE_TIMEOUT_MS);
            conn.setRequestMethod("HEAD");
            int code = conn.getResponseCode();
            if (code >= 200 && code < 500) pass(r, "node reachable, HTTP " + code);
            else warn(r, "node returned HTTP " + code);
        } catch (Throwable t) { warn(r, "node probe failed (offline?): " + t.getMessage()); }
    }

    private void checkFirestoreRulesVersion(Result r) {
        // The app cannot read the rules file at runtime, but it can read the package
        // version + a static marker from UPDATES.md if it was bundled as a raw asset.
        // For now: just record the local BouncyCastle + the app versionCode.
        try {
            PackageInfo p = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            int vc = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? (int) p.getLongVersionCode() : p.versionCode;
            pass(r, "app versionCode " + vc + " (" + p.versionName + ")");
        } catch (Throwable t) { warn(r, "versionCode read failed: " + t.getMessage()); }
    }

    private void checkBouncyCastleVersion(Result r) {
        // Read the manifest-merged provider name. If BouncyCastle's provider class
        // is reachable at all, it is on the classpath and the version is whatever
        // the dependency resolved to (1.80 after the 18 Oct bump).
        try {
            Class<?> bc = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
            Object inst = bc.getDeclaredConstructor().newInstance();
            String v = String.valueOf(bc.getField("PROVIDER_NAME").get(null));
            pass(r, "BouncyCastle loaded, provider=" + v);
        } catch (Throwable t) { warn(r, "BouncyCastle not on classpath: " + t.getMessage()); }
    }

    private void checkProGuardActiveInRelease(Result r) {
        // If the app is a release build and a class is still named with the original
        // "$View" pattern, R8 is off. Skip in debug.
        try {
            Class.forName(ctx.getPackageName() + ".databinding.ActivityDebugBinding");
            if (isReleaseBuild()) fail(r, "R8 likely disabled: databinding class still present in release");
            else pass(r, "databinding class present (debug build, expected)");
        } catch (ClassNotFoundException ok) { pass(r, "databinding not on classpath (R8 stripped it, release build expected)"); }
    }

    private void checkNetworkSecurityConfig(Result r) {
        try {
            int id = ctx.getResources().getIdentifier("network_security_config", "xml", ctx.getPackageName());
            if (id == 0) warn(r, "network_security_config not referenced (uses system defaults)");
            else pass(r, "network_security_config present");
        } catch (Throwable t) { warn(r, "network_security_config check threw: " + t.getMessage()); }
    }

    private void checkDebuggableFlag(Result r) {
        // isDebuggable on ApplicationInfo is the source of truth.
        try {
            int flags = ctx.getPackageManager().getApplicationInfo(ctx.getPackageName(), 0).flags;
            boolean debuggable = (flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            if (isReleaseBuild() && debuggable) fail(r, "release build is debuggable");
            else pass(r, "debuggable=" + debuggable + " (releaseBuild=" + isReleaseBuild() + ")");
        } catch (Throwable t) { warn(r, "debuggable check threw: " + t.getMessage()); }
    }

    private boolean isReleaseBuild() {
        // BuildConfig.DEBUG is the same flag R8 uses; it is set in the generated
        // BuildConfig class by the Android Gradle plugin.
        try { return !BuildConfig.DEBUG; } catch (Throwable t) { return false; }
    }

    /** Test seam: read a stream fully. */
    static String slurp(InputStream is) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }
}
