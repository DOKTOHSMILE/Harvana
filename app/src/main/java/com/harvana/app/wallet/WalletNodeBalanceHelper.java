package com.harvana.app.wallet;

import android.os.Handler;
import android.os.Looper;

import com.harvana.app.sync.HarvanaNodeClient;

import org.json.JSONArray;
import org.json.JSONObject;

public final class WalletNodeBalanceHelper {

	public interface Callback {
		void onResult(double balanceHarv, double availableHarv,
		              double pendingInHarv, double pendingOutHarv,
		              JSONArray pendingTxs, String error);
	}

	private WalletNodeBalanceHelper() {}

	public static void load(String address, Callback cb) {
		if (cb == null) return;
		if (address == null || !address.startsWith("HV0x")) {
			cb.onResult(0, 0, 0, 0, new JSONArray(), "bad address");
			return;
		}
		HarvanaNodeClient.getWallet(address, new HarvanaNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject json) {
				long rawBal = json.optLong("balance", 0L);
				long rawAvail = json.has("available") ? json.optLong("available", rawBal) : rawBal;
				long rawIn = json.optLong("pendingIncoming", 0L);
				long rawOut = json.optLong("pendingOutgoing", 0L);
				final double bal = HarvanaNodeClient.toHarvDisplay(rawBal);
				final double avail = HarvanaNodeClient.toHarvDisplay(rawAvail);
				final double pin = HarvanaNodeClient.toHarvDisplay(rawIn);
				final double pout = HarvanaNodeClient.toHarvDisplay(rawOut);

				HarvanaNodeClient.getPending(address, new HarvanaNodeClient.JsonCallback() {
					@Override
					public void onSuccess(JSONObject pjson) {
						JSONArray arr = pjson.optJSONArray("pending");
						if (arr == null) arr = new JSONArray();
						cb.onResult(bal, avail, pin, pout, arr, null);
					}
					@Override
					public void onError(String error) {
						cb.onResult(bal, avail, pin, pout, new JSONArray(), null);
					}
				});
			}
			@Override
			public void onError(String error) {
				cb.onResult(0, 0, 0, 0, new JSONArray(), error);
			}
		});
	}

	public static void watchConfirmation(String txHash, int maxAttempts,
	                                     Runnable onConfirmed, Runnable onGiveUp) {
		if (txHash == null || txHash.isEmpty()) {
			if (onGiveUp != null) onGiveUp.run();
			return;
		}
		final int[] n = {0};
		final Handler h = new Handler(Looper.getMainLooper());
		final Runnable[] tickRef = new Runnable[1];
		tickRef[0] = new Runnable() {
			@Override
			public void run() {
				n[0]++;
				HarvanaNodeClient.getTxStatus(txHash, new HarvanaNodeClient.JsonCallback() {
					@Override
					public void onSuccess(JSONObject json) {
						String st = json.optString("status", "");
						int conf = json.optInt("confirmations", 0);
						if ("CONFIRMED".equalsIgnoreCase(st) || conf >= 1) {
							if (onConfirmed != null) onConfirmed.run();
						} else if (n[0] >= maxAttempts) {
							if (onGiveUp != null) onGiveUp.run();
						} else {
							h.postDelayed(tickRef[0], 4000);
						}
					}
					@Override
					public void onError(String error) {
						if (n[0] >= maxAttempts) {
							if (onGiveUp != null) onGiveUp.run();
						} else {
							h.postDelayed(tickRef[0], 4000);
						}
					}
				});
			}
		};
		h.post(tickRef[0]);
	}
}