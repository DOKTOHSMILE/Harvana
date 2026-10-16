package com.harvana.app.telemetry;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.provider.MediaStore;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.harvana.app.DebugActivity;
import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.utils.CrashExplainer;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Intercepts uncaught application crashes, builds detailed system and stack diagnostics,
 * writes the report to the user's public Download folder, sends the crash log to Firestore
 * for real-time monitoring by the moderator console, and launches DebugActivity.
 */
public final class CrashReportManager {

    private static final String TAG = "CrashReportManager";
    public static final String PREFS_NAME = "crash_prefs";
    public static final String KEY_CRASH_ERROR = "crash_error";
    public static final String KEY_CRASH_REPORT = "crash_report";
    public static final String KEY_CRASH_PATH = "crash_saved_path";
    public static final String KEY_CRASH_TIME = "crash_time";

    private static volatile boolean installed = false;

    private CrashReportManager() {}

    /**
     * Installs the global uncaught exception handler. Safe to call multiple times (idempotent).
     */
    public static synchronized void install(final Context context) {
        if (installed || context == null) return;
        installed = true;

        final Context app = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                try {
                    String stackTrace = Log.getStackTraceString(throwable);

                    // Prevent infinite crash loops if DebugActivity or CrashReportManager crashes
                    if (stackTrace != null && (stackTrace.contains("DebugActivity") || stackTrace.contains("CrashReportManager.uncaughtException"))) {
                        Log.e(TAG, "Fatal crash inside DebugActivity/CrashReportManager, terminating directly", throwable);
                        Process.killProcess(Process.myPid());
                        System.exit(1);
                        return;
                    }

                    // 1. Build comprehensive diagnostic report
                    String fullReport = buildFullReport(app, thread, throwable, stackTrace);

                    // 2. Save report to the user-visible Download folder
                    String savedPath = saveReportToDownloads(app, fullReport);

                    // 3. Send crash log immediately to Firestore CRASH_LOGS collection & node
                    sendCrashToFirestore(app, throwable, stackTrace);

                    // 4. Queue crash for node telemetry (for moderator/admin tracking)
                    try {
                        CrashReporter.saveCrash(app, throwable, stackTrace);
                    } catch (Throwable ignored) {}

                    // 5. Save to SharedPreferences synchronously
                    try {
                        SharedPreferences prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        prefs.edit()
                                .putString(KEY_CRASH_ERROR, stackTrace)
                                .putString(KEY_CRASH_REPORT, fullReport)
                                .putString(KEY_CRASH_PATH, savedPath != null ? savedPath : "Download/Harvana_Latest_Crash.txt")
                                .putLong(KEY_CRASH_TIME, System.currentTimeMillis())
                                .commit();
                    } catch (Throwable t) {
                        Log.e(TAG, "Failed to write crash to shared preferences", t);
                    }

                    // 6. Launch modern DebugActivity in a fresh task
                    Intent intent = new Intent(app, DebugActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("crash_stack", stackTrace);
                    intent.putExtra("crash_report", fullReport);
                    intent.putExtra("crash_saved_path", savedPath);
                    app.startActivity(intent);

                    // Give ActivityManager a moment to launch DebugActivity before terminating current process
                    try {
                        Thread.sleep(350);
                    } catch (InterruptedException ignored) {}

                    Process.killProcess(Process.myPid());
                    System.exit(1);

                } catch (Throwable fatal) {
                    Log.e(TAG, "Unexpected error in uncaught exception handler", fatal);
                    if (defaultHandler != null) {
                        defaultHandler.uncaughtException(thread, throwable);
                    } else {
                        Process.killProcess(Process.myPid());
                        System.exit(1);
                    }
                }
            }
        });

        Log.i(TAG, "CrashReportManager uncaught exception handler installed successfully");
    }

    /**
     * Sends the crash report to Firestore collection CRASH_LOGS asynchronously.
     */
    public static void sendCrashToFirestore(final Context context, final Throwable throwable, final String stackTrace) {
        new Thread(() -> {
            try {
                String versionName = "1.2.0";
                long versionCode = 3;
                try {
                    PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                    versionName = pInfo.versionName;
                    versionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? pInfo.getLongVersionCode() : pInfo.versionCode;
                } catch (Throwable ignored) {}

                CrashExplainer.Result why = CrashExplainer.explain(stackTrace != null ? stackTrace : "");
                String screen = why.where != null && !why.where.isEmpty() ? why.where : "Unknown";
                String msg = throwable != null ? String.valueOf(throwable.getMessage()) : "Fatal crash";
                long now = System.currentTimeMillis();

                // 1. Firebase Firestore SDK
                try {
                    Map<String, Object> doc = new HashMap<>();
                    doc.put("app", "wallet");
                    doc.put("appName", "Harvana Wallet");
                    doc.put("version", versionName);
                    doc.put("versionCode", versionCode);
                    doc.put("kind", "CRASH");
                    doc.put("severity", "CRITICAL");
                    doc.put("screen", screen);
                    doc.put("where", screen);
                    doc.put("meaning", why.meaning != null ? why.meaning : "");
                    doc.put("advice", why.advice != null ? why.advice : "");
                    doc.put("message", msg);
                    doc.put("stack", stackTrace != null && stackTrace.length() > 2000 ? stackTrace.substring(0, 2000) : stackTrace);
                    doc.put("device", Build.MANUFACTURER + " " + Build.MODEL);
                    doc.put("androidVersion", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
                    doc.put("timestamp", now);
                    doc.put("status", "OPEN");
                    doc.put("fixed", false);

                    FirebaseFirestore.getInstance()
                            .collection("CRASH_LOGS")
                            .add(doc);
                } catch (Throwable t) {
                    Log.w(TAG, "Firebase SDK write to CRASH_LOGS skipped: " + t.getMessage());
                }

                // 2. HTTP POST to node error endpoint (which mirrors to Firestore with service credentials)
                try {
                    JSONObject r = new JSONObject();
                    r.put("app", "wallet");
                    r.put("version", versionName);
                    r.put("kind", "CRASH");
                    r.put("screen", screen);
                    r.put("message", msg);
                    r.put("stack", stackTrace != null && stackTrace.length() > 1500 ? stackTrace.substring(0, 1500) : stackTrace);

                    HttpURLConnection conn = (HttpURLConnection) new URL(HarvanaNodeClient.BASE + "/api/v1/app/error").openConnection();
                    conn.setConnectTimeout(8_000);
                    conn.setReadTimeout(8_000);
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(r.toString().getBytes(StandardCharsets.UTF_8));
                    }
                    int code = conn.getResponseCode();
                    Log.i(TAG, "Sent crash to node error tracker, HTTP " + code);
                    conn.disconnect();
                } catch (Throwable t) {
                    Log.w(TAG, "Failed sending crash to node /api/v1/app/error: " + t.getMessage());
                }

            } catch (Throwable t) {
                Log.w(TAG, "sendCrashToFirestore error: " + t.getMessage());
            }
        }, "firestore-crash-sender").start();
    }

    /**
     * Builds a detailed human-readable and machine-scannable report with full system diagnostics.
     */
    public static String buildFullReport(Context context, Thread thread, Throwable throwable, String stackTrace) {
        StringBuilder sb = new StringBuilder();

        SimpleDateFormat sdfUtc = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US);
        sdfUtc.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timeStr = sdfUtc.format(new Date());

        String versionName = "Unknown";
        long versionCode = 0;
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionName = pInfo.versionName;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = pInfo.getLongVersionCode();
            } else {
                versionCode = pInfo.versionCode;
            }
        } catch (Throwable ignored) {}

        // Explainer analysis
        CrashExplainer.Result why = CrashExplainer.explain(stackTrace != null ? stackTrace : "");

        sb.append("======================================================================\n");
        sb.append("                   HARVANA WALLET CRASH REPORT                        \n");
        sb.append("======================================================================\n");
        sb.append("Timestamp:       ").append(timeStr).append(" (epoch: ").append(System.currentTimeMillis()).append(")\n");
        sb.append("Application:     ").append(context.getPackageName()).append(" (v").append(versionName).append(" #").append(versionCode).append(")\n");
        sb.append("Process PID:     ").append(Process.myPid()).append("\n");
        if (thread != null) {
            sb.append("Thread:          ").append(thread.getName()).append(" (id: ").append(thread.getId()).append(", priority: ").append(thread.getPriority()).append(")\n");
        }
        sb.append("\n");

        sb.append("----------------------------------------------------------------------\n");
        sb.append("DIAGNOSTIC SUMMARY\n");
        sb.append("----------------------------------------------------------------------\n");
        sb.append("Crash Meaning:   ").append(why.meaning).append("\n");
        if (!why.where.isEmpty()) {
            sb.append("Location:        ").append(why.where).append("\n");
        }
        if (!why.advice.isEmpty()) {
            sb.append("Recommendation:  ").append(why.advice).append("\n");
        }
        if (throwable != null) {
            sb.append("Exception Class: ").append(throwable.getClass().getName()).append("\n");
            sb.append("Message:         ").append(throwable.getMessage()).append("\n");
        }
        sb.append("\n");

        sb.append("----------------------------------------------------------------------\n");
        sb.append("DEVICE & ENVIRONMENT\n");
        sb.append("----------------------------------------------------------------------\n");
        sb.append("Device Model:    ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append(" (").append(Build.DEVICE).append(")\n");
        sb.append("Brand:           ").append(Build.BRAND).append(" / Product: ").append(Build.PRODUCT).append("\n");
        sb.append("Hardware/Board:  ").append(Build.HARDWARE).append(" / ").append(Build.BOARD).append("\n");
        sb.append("Android Version: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("Build ID:        ").append(Build.ID).append("\n");
        sb.append("Fingerprint:     ").append(Build.FINGERPRINT).append("\n");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sb.append("Supported ABIs:  ").append(Arrays.toString(Build.SUPPORTED_ABIS)).append("\n");
        }
        sb.append("\n");

        sb.append("----------------------------------------------------------------------\n");
        sb.append("MEMORY & RUNTIME STATUS\n");
        sb.append("----------------------------------------------------------------------\n");
        Runtime rt = Runtime.getRuntime();
        long maxMemMb = rt.maxMemory() / (1024 * 1024);
        long totalMemMb = rt.totalMemory() / (1024 * 1024);
        long freeMemMb = rt.freeMemory() / (1024 * 1024);
        long usedMemMb = totalMemMb - freeMemMb;
        sb.append("JVM Heap:        Used ").append(usedMemMb).append("MB / Allocated ").append(totalMemMb).append("MB (Max: ").append(maxMemMb).append("MB)\n");

        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                sb.append("System RAM:      Avail ").append(mi.availMem / (1024 * 1024)).append("MB / Total ").append(mi.totalMem / (1024 * 1024)).append("MB (Low RAM: ").append(mi.lowMemory).append(")\n");
            }
        } catch (Throwable ignored) {}
        sb.append("\n");

        sb.append("======================================================================\n");
        sb.append("COMPLETE STACK TRACE\n");
        sb.append("======================================================================\n");
        sb.append(stackTrace != null && !stackTrace.isEmpty() ? stackTrace : "No stack trace provided.").append("\n\n");

        sb.append("======================================================================\n");
        sb.append("END OF CRASH REPORT\n");
        sb.append("======================================================================\n");

        return sb.toString();
    }

    /**
     * Saves the crash report to the user's Download folder across all Android versions.
     * Returns the primary user-visible path where the report was saved.
     */
    public static String saveReportToDownloads(Context context, String report) {
        if (context == null || report == null) return null;

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "Harvana_Crash_" + timeStamp + ".txt";
        String resolvedPath = null;

        // Method 1: MediaStore on Android 10+ (API 29+) -> Public Download directory without runtime permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                ContentResolver resolver = context.getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream os = resolver.openOutputStream(uri)) {
                        if (os != null) {
                            os.write(report.getBytes(StandardCharsets.UTF_8));
                            os.flush();
                            resolvedPath = "Download/" + fileName;
                            Log.i(TAG, "Crash report saved via MediaStore to: " + resolvedPath);
                        }
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "MediaStore save failed: " + t.getMessage());
            }
        }

        // Method 2: Public Download directory via Environment.getExternalStoragePublicDirectory
        try {
            File publicDownloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (publicDownloadDir != null) {
                if (!publicDownloadDir.exists()) {
                    publicDownloadDir.mkdirs();
                }
                if (publicDownloadDir.exists() && publicDownloadDir.canWrite()) {
                    File crashFile = new File(publicDownloadDir, fileName);
                    try (FileOutputStream fos = new FileOutputStream(crashFile)) {
                        fos.write(report.getBytes(StandardCharsets.UTF_8));
                        fos.flush();
                        resolvedPath = crashFile.getAbsolutePath();
                        Log.i(TAG, "Crash report saved to public downloads: " + resolvedPath);
                    }

                    // Also maintain a fixed latest crash copy for easy reference
                    File latestFile = new File(publicDownloadDir, "Harvana_Latest_Crash.txt");
                    try (FileOutputStream fos = new FileOutputStream(latestFile)) {
                        fos.write(report.getBytes(StandardCharsets.UTF_8));
                        fos.flush();
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Public download directory save failed: " + t.getMessage());
        }

        // Method 3: App-specific external files dir (always accessible via file managers and USB)
        try {
            File extDownloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (extDownloadDir != null) {
                if (!extDownloadDir.exists()) {
                    extDownloadDir.mkdirs();
                }
                File extFile = new File(extDownloadDir, fileName);
                try (FileOutputStream fos = new FileOutputStream(extFile)) {
                    fos.write(report.getBytes(StandardCharsets.UTF_8));
                    fos.flush();
                    if (resolvedPath == null) {
                        resolvedPath = extFile.getAbsolutePath();
                    }
                }

                File extLatest = new File(extDownloadDir, "Harvana_Latest_Crash.txt");
                try (FileOutputStream fos = new FileOutputStream(extLatest)) {
                    fos.write(report.getBytes(StandardCharsets.UTF_8));
                    fos.flush();
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "External files directory save failed: " + t.getMessage());
        }

        // Method 4: Internal private directory backup
        try {
            File internalDir = new File(context.getFilesDir(), "CrashLogs");
            if (!internalDir.exists()) {
                internalDir.mkdirs();
            }
            File internalFile = new File(internalDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(internalFile)) {
                fos.write(report.getBytes(StandardCharsets.UTF_8));
                fos.flush();
                if (resolvedPath == null) {
                    resolvedPath = internalFile.getAbsolutePath();
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Internal directory backup save failed: " + t.getMessage());
        }

        return resolvedPath != null ? resolvedPath : "Download/" + fileName;
    }
}
