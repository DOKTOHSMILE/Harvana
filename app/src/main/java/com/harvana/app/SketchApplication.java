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

        // Price every coin the wallet shows before any screen opens, so Home's total
        // and the Assets list are not waiting on the first time that list is drawn.
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener(auth -> {
                if (auth.getCurrentUser() != null) com.harvana.app.support.SupportDesk.watchReplies(this);
            });
        } catch (Throwable t) {
            Log.e(TAG, "Support watch failed", t);
        }
        try {
            com.harvana.app.wallet.AssetPrices.load(this, null);
            com.harvana.app.wallet.FxRates.load(this, () -> {
                String addr = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(SketchApplication.this);
                if (addr != null && !addr.isEmpty()) {
                    com.harvana.app.wallet.WalletTotal.refresh(SketchApplication.this, addr);
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "Price prefetch failed", t);
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
            com.harvana.app.web3.Web3Bubble.install(this);              // floating Web3 button while the browser is minimized
            com.harvana.app.walletconnect.WalletConnect.install(this);   // reconnects connected dApps; prompts wait for a screen
            // v2 member locks: a payment that waited for a screen, or whose answer was lost, is offered on the next signed-in screen.
            registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
                @Override public void onActivityResumed(android.app.Activity a) {
                    String n = a.getClass().getSimpleName();
                    if (n.equals("SplashScreenActivity") || n.equals("WelcomeActivity") || n.equals("LoginActivity") || n.equals("DebugActivity")) return;
                    com.harvana.app.sync.LockSigner.onScreenOpened(a);
                }
                @Override public void onActivityCreated(android.app.Activity a, android.os.Bundle b) { }
                @Override public void onActivityStarted(android.app.Activity a) { }
                @Override public void onActivityPaused(android.app.Activity a) { }
                @Override public void onActivityStopped(android.app.Activity a) { }
                @Override public void onActivitySaveInstanceState(android.app.Activity a, android.os.Bundle b) { }
                @Override public void onActivityDestroyed(android.app.Activity a) { }
            });
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

        // 10. Start WorkManager by calling its initializer class. A keep rule is not enough on
        //     CodeAssist: R8 still deleted the class when nothing in Java used it, and the
        //     manifest then crashed the app looking the class up by name.
        try {
            new androidx.work.WorkManagerInitializer().create(this);
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

        // 12. Startup self-check. Runs once after init; never throws; the result is
        //     uploaded to CRASH_LOGS so the operator sees misconfigurations in the
        //     moderator dashboard instead of the app dying silently on a user device.
        try {
            com.harvana.app.utils.AppSelfCheck.Result r = new com.harvana.app.utils.AppSelfCheck(this).run();
            if (!r.ok()) {
                Log.w(TAG, "AppSelfCheck failed: " + r.failed);
                com.harvana.app.telemetry.CrashReportManager.sendStartupCheck(this, r);
            }
        } catch (Throwable t) {
            Log.w(TAG, "AppSelfCheck threw: " + t.getMessage());
        }
    }
}
