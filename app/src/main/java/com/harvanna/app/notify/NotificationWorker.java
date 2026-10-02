package com.harvanna.app.notify;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Pulls circle news while the app is closed. Android will not run this more often than about every 15 minutes. */
public class NotificationWorker extends Worker {
    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }

    @NonNull @Override
    public Result doWork() {
        final CountDownLatch latch = new CountDownLatch(1);
        NotificationSync.run(getApplicationContext(), latch::countDown);
        try { latch.await(45, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
        return Result.success();
    }

    public static void schedule(Context c) {
        Context app = c.getApplicationContext();
        Constraints cons = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        PeriodicWorkRequest periodic = new PeriodicWorkRequest.Builder(NotificationWorker.class, 15, TimeUnit.MINUTES)
            .setConstraints(cons).build();
        WorkManager wm = WorkManager.getInstance(app);
        wm.enqueueUniquePeriodicWork("harvanna_notification_sync", ExistingPeriodicWorkPolicy.UPDATE, periodic);
        OneTimeWorkRequest now = new OneTimeWorkRequest.Builder(NotificationWorker.class).setConstraints(cons).build();
        wm.enqueueUniqueWork("harvanna_notification_sync_now", ExistingWorkPolicy.KEEP, now);
    }
}
