package com.harvanna.app.group;

import android.util.Log;

import com.harvanna.app.sync.HarvannaNodeClient;

import org.json.JSONObject;

/**
 * Groups must not write CONFIRMED to Firestore unless the node
 * already has this tx. Callers still talk to Firestore themselves;
 * this only answers "is this hash real and confirmed?".
 *
 * Not a substitute for Cloud Functions + lock-down rules. A patched
 * APK can still skip this class. It stops the honest UI and any
 * caller that goes through GroupPaymentHelper / GroupPayoutManager
 * from minting a fake contribution.
 */
public final class GroupOnChainGate {

    private static final String TAG = "GroupOnChainGate";

    public interface Callback {
        void onConfirmed(String txHash, long blockHeight);
        void onError(String error);
    }

    private GroupOnChainGate() {}

    public static void requireConfirmedTx(String txRef, Callback cb) {
        if (txRef == null || txRef.trim().isEmpty()) {
            if (cb != null) cb.onError("Missing on-chain tx hash — cannot mark CONFIRMED");
            return;
        }
        final String hash = txRef.trim();
        HarvannaNodeClient.getTxStatus(hash, new HarvannaNodeClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                String status = json.optString("status", json.optString("state", ""));
                long height = json.optLong("blockHeight", json.optLong("height", -1L));
                if (isConfirmed(status) && height >= 0) {
                    if (cb != null) cb.onConfirmed(hash, height);
                    return;
                }
                if (isConfirmed(status)) {
                    if (cb != null) cb.onConfirmed(hash, height);
                    return;
                }
                Log.w(TAG, "tx " + hash + " status=" + status + " height=" + height);
                if (cb != null) {
                    cb.onError("Tx is not confirmed on-chain yet (" +
                            (status.isEmpty() ? "unknown" : status) + ")");
                }
            }

            @Override
            public void onError(String error) {
                if (cb != null) cb.onError("Could not verify tx on node: " + error);
            }
        });
    }

    public static boolean isConfirmed(String status) {
        if (status == null) return false;
        String s = status.trim().toUpperCase();
        return "CONFIRMED".equals(s) || "MINED".equals(s) || "INCLUDED".equals(s);
    }
}
