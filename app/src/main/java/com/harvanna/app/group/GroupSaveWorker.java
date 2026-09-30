package com.harvanna.app.group;

import android.content.Context;

import androidx.annotation.NonNull;
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
        if (GroupSaver.pendingCount(c) == 0) { attempt(c, 0); return Result.success(); }
        int n = attempt(c, -1) + 1;
        attempt(c, n);
        scheduleIn(c, delayMinutes(n - 1), ExistingWorkPolicy.APPEND_OR_REPLACE);
        return Result.success();
    }

    /** Fixed retry times after a failed save: 1, 5, 15 and 30 minutes, then every hour. */
    public static int delayMinutes(int attempt) {
        int[] steps = {1, 5, 15, 30};
        return attempt < steps.length ? steps[attempt] : 60;
    }

    private static int attempt(Context c, int set) {
        android.content.SharedPreferences sp = c.getSharedPreferences("pending_group_saves_meta", Context.MODE_PRIVATE);
        if (set >= 0) sp.edit().putInt("attempt", set).apply();
        return sp.getInt("attempt", 0);
    }

    public static void schedule(Context c) {
        attempt(c, 0);
        scheduleIn(c, 0, ExistingWorkPolicy.KEEP);
    }

    private static void scheduleIn(Context c, int minutes, ExistingWorkPolicy policy) {
        Constraints cons = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(GroupSaveWorker.class)
            .setConstraints(cons).setInitialDelay(minutes, TimeUnit.MINUTES).build();
        WorkManager.getInstance(c.getApplicationContext()).enqueueUniqueWork("harvanna_group_save", policy, req);
    }
}
