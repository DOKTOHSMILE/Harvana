package com.harvana.app.notify;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Background check for new notifications (about every 15 minutes, only with a network connection). */
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
        Constraints cons = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(NotificationWorker.class, 15, TimeUnit.MINUTES)
            .setConstraints(cons).build();
        WorkManager.getInstance(c.getApplicationContext())
            .enqueueUniquePeriodicWork("harvana_notification_sync", ExistingPeriodicWorkPolicy.KEEP, req);
    }
}
