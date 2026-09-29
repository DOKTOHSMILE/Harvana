package com.harvanna.app.wallet;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import com.harvanna.app.sync.HarvannaNodeClient;

/**
 * Fetches live balance and nonce from the HARV node.
 *
 * SERVER RESPONSE FORMAT (as of node v1.0.0):
 *   { "address": "HV0x...", "balance": <long raw units>, "nonce": <long>, "blockHeight": <long> }
 *
 * "balance" is raw units (long), NOT a "balance_harv" double.
 * Use HarvannaNodeClient.toHarvDisplay() to convert for UI.
 */
public final class WalletNodeBridge {

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private WalletNodeBridge() {}

    public interface BalanceCallback {
        /** @param balanceHarv human-readable HARV amount (raw units / 1e8) */
        void onSuccess(double balanceHarv, long nonce, long blockHeight);
        void onError(String error);
    }

    /**
     * Fetches live confirmed balance from the node.
     * Balance displayed to the user = rawBalance / HARV_UNITS_PER_TOKEN.
     *
     * @param context   not used currently, kept for API compatibility
     * @param address   HV0x wallet address
     * @param callback  delivered on main thread
     */
    public static void fetchLiveBalance(Context context, String address, BalanceCallback callback) {
        if (address == null || address.trim().isEmpty()) {
            callback.onError("Invalid wallet address");
            return;
        }
        if (!address.startsWith("HV0x") || address.length() != 44) {
            callback.onError("Address must be 44 chars starting with HV0x");
            return;
        }

        HarvannaNodeClient.getWallet(address, new HarvannaNodeClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                try {
                    // Server returns raw units as long — NOT "balance_harv" double
                    long rawBalance  = json.optLong("balance", 0L);
                    long nonce       = json.optLong("nonce", 0L);
                    long blockHeight = json.optLong("blockHeight", -1L);

                    // Convert to human-readable HARV for display
                    double balanceHarv = HarvannaNodeClient.toHarvDisplay(rawBalance);

                    MAIN.post(() -> callback.onSuccess(balanceHarv, nonce, blockHeight));
                } catch (Exception e) {
                    MAIN.post(() -> callback.onError("Failed to parse balance: " + e.getMessage()));
                }
            }

            @Override
            public void onError(String error) {
                MAIN.post(() -> callback.onError(error));
            }
        });
    }

    /**
     * Fetches the current nonce for an address.
     * Use this to set the nonce before signing a transaction:
     *   nonce_to_sign = returned_nonce + 1
     */
    public static void fetchNonce(String address, NonceCallback callback) {
        HarvannaNodeClient.getWallet(address, new HarvannaNodeClient.JsonCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                long nonce = json.optLong("nonce", 0L);
                MAIN.post(() -> callback.onSuccess(nonce));
            }

            @Override
            public void onError(String error) {
                MAIN.post(() -> callback.onError(error));
            }
        });
    }

    public interface NonceCallback {
        void onSuccess(long confirmedNonce);
        void onError(String error);
    }
}
