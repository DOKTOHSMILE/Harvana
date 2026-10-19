package com.harvana.app.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.harvana.app.R;
import com.harvana.app.sync.HarvanaNodeClient;

import org.json.JSONObject;

/**
 * A banner while the network is paused by the guardians or this wallet is frozen (GET /freeze/global-status and
 * /freeze/status/{address}). Before, a send just failed with a code. Shown at the top of Home and Send; removed again
 * once both are clear.
 */
public final class NetworkStatusBanner {
    private NetworkStatusBanner() {}

    private static final String TAG = "network_status_banner";

    /** Checks both and shows or removes the banner as child {@code index} of {@code parent}. */
    public static void refresh(ViewGroup parent, int index, String address) {
        if (parent == null) return;
        HarvanaNodeClient.nodeGet("/api/v1/freeze/global-status", new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject g) {
                boolean paused = g.optBoolean("paused");
                if (address == null || !address.startsWith("HV0x")) { show(parent, index, paused, false); return; }
                HarvanaNodeClient.nodeGet("/api/v1/freeze/status/" + address, new HarvanaNodeClient.JsonCallback() {
                    @Override public void onSuccess(JSONObject a) { show(parent, index, paused, a.optBoolean("frozen")); }
                    @Override public void onError(String e) { show(parent, index, paused, false); }
                });
            }
            @Override public void onError(String e) { }                      // unknown: show nothing rather than a guess
        });
    }

    private static void show(ViewGroup parent, int index, boolean paused, boolean frozen) {
        parent.post(() -> {
            View old = parent.findViewWithTag(TAG);
            if (!paused && !frozen) { if (old != null) parent.removeView(old); return; }
            String text = paused && frozen ? "Network paused · This wallet is frozen" : paused ? "Network paused" : "This wallet is frozen";
            View v = old;
            if (v == null) {
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_status_banner, parent, false);
                v.setTag(TAG);
                parent.addView(v, Math.min(index, parent.getChildCount()));
            }
            ((TextView) v.findViewById(R.id.status_banner_text)).setText(text);
        });
    }
}
