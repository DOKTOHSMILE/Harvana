package com.harvana.app.telemetry;

import android.content.Context;
import android.content.SharedPreferences;

import com.harvana.app.sync.HarvanaNodeClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Sends crash reports to the node's error tracker (POST /api/v1/app/error), which the moderator app reads.
 * A crash is saved while the app dies and sent on the next start; at most 10 wait in the queue.
 * Only the error and where it happened are sent: no wallet, address, balance or account data is added.
 */
public final class CrashReporter {

	private CrashReporter() {}

	private static final String PREFS = "crash_reports";
	private static final String KEY = "pending";
	private static final int MAX_PENDING = 10;

	/** Called from the uncaught-exception handler: saves synchronously, does not touch the network. */
	public static void saveCrash(Context c, Throwable t, String stack) {
		try {
			JSONObject r = new JSONObject();
			r.put("app", "wallet");
			r.put("version", version(c));
			r.put("kind", "CRASH");
			r.put("screen", screenOf(t));
			r.put("message", String.valueOf(t));
			r.put("stack", stack.length() > 1500 ? stack.substring(0, 1500) : stack);
			SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
			JSONArray q = new JSONArray(p.getString(KEY, "[]"));
			if (q.length() >= MAX_PENDING) q.remove(0);
			q.put(r);
			p.edit().putString(KEY, q.toString()).commit();
		} catch (Exception ignored) {
		}
	}

	/** Sends what was saved, on a background thread. Reports the node refuses for good are dropped; others wait. */
	public static void sendPending(Context ctx) {
		Context c = ctx.getApplicationContext();
		new Thread(() -> {
			SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
			JSONArray q;
			try { q = new JSONArray(p.getString(KEY, "[]")); } catch (Exception e) { q = new JSONArray(); }
			JSONArray left = new JSONArray();
			for (int i = 0; i < q.length(); i++) {
				JSONObject r = q.optJSONObject(i);
				if (r == null) continue;
				int code = post(r);
				if (code == 404 || code == 429 || code < 0 || code >= 500) left.put(r);   // 404: node not updated yet
			}
			p.edit().putString(KEY, left.toString()).apply();
		}, "crash-reporter").start();
	}

	private static int post(JSONObject r) {
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) new URL(HarvanaNodeClient.BASE + "/api/v1/app/error").openConnection();
			conn.setConnectTimeout(10_000);
			conn.setReadTimeout(10_000);
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
			try (OutputStream os = conn.getOutputStream()) { os.write(r.toString().getBytes(StandardCharsets.UTF_8)); }
			return conn.getResponseCode();
		} catch (Exception e) {
			return -1;
		} finally {
			if (conn != null) conn.disconnect();
		}
	}

	/** The first frame in the app's own code: "WalletActivity.onCreate". */
	static String screenOf(Throwable t) {
		for (Throwable x = t; x != null; x = x.getCause() == x ? null : x.getCause()) {
			for (StackTraceElement e : x.getStackTrace()) {
				String cls = e.getClassName();
				if (cls.startsWith("com.harvana.app")) {
					String simple = cls.substring(cls.lastIndexOf('.') + 1);
					int dollar = simple.indexOf('$');
					if (dollar > 0) simple = simple.substring(0, dollar);
					return simple + "." + e.getMethodName();
				}
			}
		}
		return "";
	}

	private static String version(Context c) {
		try { return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName; } catch (Exception e) { return ""; }
	}
}
