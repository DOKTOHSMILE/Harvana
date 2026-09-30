package com.harvanna.app.group;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

/** Saves queued group records in the background as soon as a connection exists, retrying with growing delays until done. */
public class GroupSaveWorker extends Worker {
    public GroupSaveWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }

    @NonNull @Override
    public Result doWork() {
        Context c = getApplicationContext();
        if (GroupSaver.pendingCount(c) == 0) return Result.success();
        GroupSaver.retryAll(c, null);
        // Wait for the saves to finish (up to ~40 s), then report whether anything is still queued.
        for (int i = 0; i < 40 && GroupSaver.pendingCount(c) > 0; i++) {
            try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
        }
        return GroupSaver.pendingCount(c) == 0 ? Result.success() : Result.retry();
    }

    public static void schedule(Context c) {
        Constraints cons = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(GroupSaveWorker.class)
            .setConstraints(cons).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build();
        WorkManager.getInstance(c.getApplicationContext())
            .enqueueUniqueWork("harvanna_group_save", ExistingWorkPolicy.KEEP, req);
    }
}
