package com.harvana.app.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.harvana.app.MainActivity;
import com.harvana.app.R;

/**
* System tray notifications for wallet transfers.
* Works when the app is in foreground, background, or the screen is off.
*/
public class TransactionNotification {
	
	private static final String CHANNEL_ID = "harvana_transactions";
	private static final String CHANNEL_NAME = "Transactions";
	private static final int BASE_NOTIFICATION_ID = 3001;
	
	/**
* Show a system notification for a sent or received transfer.
*
* @param context   any Context (Activity or Application)
* @param isIncoming true = received, false = sent
* @param amount    amount string e.g. "12.5000"
* @param symbol    token symbol e.g. "HARV"
* @param counterparty short address or name of the other party
* @param txHash    optional tx hash (can be null)
*/	
	public static void show(Context context, boolean isIncoming,
	String amount, String symbol,
	String counterparty, String txHash) {
		if (context == null) return;
		
		Context app = context.getApplicationContext();
		NotificationManager nm =
		(NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
		if (nm == null) return;
		
		createChannel(nm);
		
		String title = isIncoming ? "Received " + amount + " " + symbol
		: "Sent " + amount + " " + symbol;
		
		String body;
		if (isIncoming) {
			body = "From " + shortAddr(counterparty);
		} else {
			body = "To " + shortAddr(counterparty);
		}
		if (txHash != null && txHash.length() > 12) {
			body = body + " · " + txHash.substring(0, 10) + "…";
		}
		
		// Open wallet when user taps the notification
		Intent tapIntent = new Intent(app, MainActivity.class);
		// Add a flag so MainActivity knows to open the Asset tab
		tapIntent.putExtra("open_fragment", "asset"); 
		tapIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		int flags = PendingIntent.FLAG_UPDATE_CURRENT;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			flags |= PendingIntent.FLAG_IMMUTABLE;
		}
		PendingIntent pi = PendingIntent.getActivity(app, 0, tapIntent, flags);
		
		int iconRes = isIncoming
		? R.drawable.icon_call_received_twotone
		: R.drawable.icon_call_made_twotone;
		
		// Fallback if those drawables are missing
		try {
			app.getResources().getDrawable(iconRes, null);
		} catch (Exception e) {
			iconRes = android.R.drawable.ic_dialog_info;
		}
		
		NotificationCompat.Builder builder = new NotificationCompat.Builder(app, CHANNEL_ID)
		.setContentTitle(title)
		.setContentText(body)
		.setSmallIcon(iconRes)
		.setColor(UiColors.accent())
		.setContentIntent(pi)
		.setAutoCancel(true)
		.setOnlyAlertOnce(true)
		.setPriority(NotificationCompat.PRIORITY_HIGH)
		.setCategory(NotificationCompat.CATEGORY_MESSAGE);
		
		// Unique ID so multiple txs don't overwrite each other
		int id = BASE_NOTIFICATION_ID + (int) (System.currentTimeMillis() % 100000);
		nm.notify(id, builder.build());
	}
	
	/** Convenience for outgoing transfers */
	public static void showSent(Context context, String amount, String symbol,
	String recipient, String txHash) {
		show(context, false, amount, symbol, recipient, txHash);
	}
	
	/** Convenience for incoming transfers */
	public static void showReceived(Context context, String amount, String symbol,
	String sender, String txHash) {
		show(context, true, amount, symbol, sender, txHash);
	}
	
	private static void createChannel(NotificationManager nm) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(
			CHANNEL_ID,
			CHANNEL_NAME,
			NotificationManager.IMPORTANCE_HIGH
			);
			channel.setDescription(("Incoming and outgoing " + com.harvana.app.CoinInfo.ticker() + " / token transfers"));
			channel.enableVibration(true);
			channel.setShowBadge(true);
			nm.createNotificationChannel(channel);
		}
	}
	
	private static String shortAddr(String addr) {
		if (addr == null || addr.isEmpty()) return "unknown";
		if (addr.length() <= 12) return addr;
		return addr.substring(0, 8) + "…" + addr.substring(addr.length() - 4);
	}
}
