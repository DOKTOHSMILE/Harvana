package com.harvana.app;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.harvana.app.telemetry.CrashReportManager;

public class SketchApplication extends Application {

    private static final String TAG = "SketchApplication";
    private static Context appContext;

    public static Context getContext() {
        return appContext;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // Step 0: Install global uncaught exception interceptor before any class loading or onCreate
        try {
            CrashReportManager.install(base);
        } catch (Throwable t) {
            Log.e(TAG, "Early crash interceptor installation failed", t);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();

        // Ensure crash handler is bound to application context as well
        try {
            CrashReportManager.install(this);
        } catch (Throwable t) {
            Log.e(TAG, "CrashReportManager install in onCreate failed", t);
        }

        // 1. Core wallet and token configuration
        try {
            com.harvana.app.wallet.Net.load(this);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to load Net", t);
        }

        try {
            com.harvana.app.wallet.CustomAssets.all(this);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to load CustomAssets", t);
        }

        // 2. Theming and UI appearance
        try {
            com.harvana.app.utils.ThemeManager.applySavedTheme(this);
            com.harvana.app.utils.ThemeManager.keepAppContextInSync(this);
        } catch (Throwable t) {
            Log.e(TAG, "ThemeManager setup failed", t);
        }

        // 3. UI and Activity tracking helpers
        try {
            com.harvana.app.utils.BarMatcher.install(this);
            com.harvana.app.utils.ActivityTracker.install(this);
            com.harvana.app.tx.PaymentTracker.install(this);
            com.harvana.app.utils.SmartCaps.install(this);
            com.harvana.app.utils.SmartSpacing.install(this);
        } catch (Throwable t) {
            Log.e(TAG, "UI trackers setup failed", t);
        }

        // 4. Node read cache for offline responsiveness
        try {
            com.harvana.app.sync.NodeReadCache.init(this);
        } catch (Throwable t) {
            Log.e(TAG, "NodeReadCache init failed", t);
        }

        // 5. Firebase initialization with safety
        try {
            FirebaseApp.initializeApp(this);
        } catch (Throwable t) {
            Log.w(TAG, "FirebaseApp.initializeApp returned: " + t.getMessage());
        }

        // 6. Firestore offline persistence
        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().setFirestoreSettings(
                new com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(100L * 1024 * 1024)
                    .build());
        } catch (Throwable e) {
            Log.w(TAG, "Firestore settings already applied or init issue: " + e.getMessage());
        }

        // 7. Session guard
        try {
            com.harvana.app.auth.SessionGuard.install(this);
        } catch (Throwable t) {
            Log.e(TAG, "SessionGuard install failed", t);
        }

        // 8. One-time AJ0x -> HV0x wallet address migration (idempotent)
        try {
            com.harvana.app.wallet.AddressMigration.run(this);
        } catch (Throwable t) {
            Log.e(TAG, "AddressMigration run failed", t);
        }

        // 9. Coin ticker / info from node
        try {
            CoinInfo.init(this);
            CoinInfo.refresh(this);
        } catch (Throwable t) {
            Log.e(TAG, "CoinInfo init/refresh failed", t);
        }

        // 10. WorkManager is started here, not by the manifest. The manifest entry was crashing
        //     release builds after minify removed WorkManagerInitializer.
        try {
            androidx.work.WorkManager.initialize(this, new androidx.work.Configuration.Builder().build());
        } catch (IllegalStateException already) {
            Log.d(TAG, "WorkManager was already started");
        } catch (Throwable t) {
            Log.e(TAG, "WorkManager start failed", t);
        }
        try {
            com.harvana.app.notify.NotificationWorker.schedule(this);
            com.harvana.app.notify.WalletWatchService.start(this);
        } catch (Throwable e) {
            Log.w(TAG, "Could not schedule background notifications: " + e.getMessage());
        }

        // 11. Send any pending telemetry crashes from previous runs
        try {
            com.harvana.app.telemetry.CrashReporter.sendPending(this);
        } catch (Throwable t) {
            Log.w(TAG, "CrashReporter.sendPending failed: " + t.getMessage());
        }
    }
}
