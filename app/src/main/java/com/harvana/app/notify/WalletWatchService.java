package com.harvana.app.notify;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.harvana.app.wallet.WalletManager;
import com.harvana.app.ws.WalletSocketClient;

/**
 * Holds the wallet socket while the app is not on screen, so a payment can notify as soon as the node
 * confirms it. Android only allows that if the listener is a foreground service, so a silent ongoing
 * notice stays up. There is no push server. The first check of a wallet still announces nothing.
 */
public class WalletWatchService extends Service {
    private static final String CHANNEL = "harv_wallet_watch";
    private static final int NOTE_ID = 7104;

    private WalletSocketClient socket;
    private String watching = "";

    public static void start(Context c) {
        if (c == null) return;
        String addr = WalletManager.getActiveWalletAddress(c.getApplicationContext());
        if (addr == null || addr.length() != 44 || !addr.startsWith("HV0x")) return;
        Intent i = new Intent(c.getApplicationContext(), WalletWatchService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) c.getApplicationContext().startForegroundService(i);
            else c.getApplicationContext().startService(i);
        } catch (Exception ignored) {
            // Android refused a background start. The 15-minute check still runs.
        }
    }

    public static void stop(Context c) {
        if (c == null) return;
        try { c.getApplicationContext().stopService(new Intent(c.getApplicationContext(), WalletWatchService.class)); }
        catch (Exception ignored) { }
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!enterForeground()) {
            stopSelf();
            return START_NOT_STICKY;
        }
        String addr = WalletManager.getActiveWalletAddress(this);
        if (addr == null || addr.length() != 44 || !addr.startsWith("HV0x")) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!addr.equals(watching)) {
            watching = addr;
            if (socket != null) socket.shutdown();
            socket = new WalletSocketClient(addr, new WalletSocketClient.WalletSocketListener() {
                @Override public void onBalanceUpdate(long rawBalance, long nonce, long blockHeight) { }
                @Override public void onTxConfirmed(String txHash, long blockHeight, int confirmations) {
                    WalletTxNotifier.run(getApplicationContext(), null);
                }
                @Override public void onConnected() { }
                @Override public void onDisconnected(String reason) { }
                @Override public void onError(String error) { }
            });
            socket.connect();
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        if (socket != null) socket.shutdown();
        socket = null;
        watching = "";
        super.onDestroy();
    }

    private boolean enterForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Payment listener", NotificationManager.IMPORTANCE_MIN);
            ch.setSound(null, null);
            ch.enableVibration(false);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        Notification n = new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(com.harvana.app.R.drawable.ic_stat_notify)
                .setContentTitle("Harvana")
                .setContentText("Listening for payments")
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(NOTE_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            else startForeground(NOTE_ID, n);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
