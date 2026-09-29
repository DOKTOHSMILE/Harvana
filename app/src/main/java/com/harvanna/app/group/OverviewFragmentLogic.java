package com.harvanna.app.group;

import android.content.Context;
import android.view.View;

import com.harvanna.app.firestore.GroupFirestoreManager;
import com.harvanna.app.sync.AjooNodeClient;

import org.json.JSONObject;

import java.util.Map;

/** Logic for OverviewFragmentActivity */
public class OverviewFragmentLogic {

	private final Context ctx;
	private String groupId = "";
	private boolean isAdmin = false;

	public OverviewFragmentLogic(Context ctx) {
		this.ctx = ctx.getApplicationContext();
	}

	public void setGroupId(String id) { this.groupId = id != null ? id : ""; }
	public void setAdmin(boolean admin) { this.isAdmin = admin; }

	public void load(final View root) {
		if (groupId.isEmpty() || root == null) return;

		// Offline-first: render whatever was cached immediately. Reuses the
		// same "group_" + groupId key GroupDetailsLogic writes to — same
		// underlying document, so whichever tab fetches it first warms the
		// cache for the other.
		Map<String, Object> cachedGroup = com.harvanna.app.offline.OfflineCacheManager.load(ctx, "group_" + groupId);
		if (cachedGroup != null && !cachedGroup.isEmpty()) {
			_bindOverview(root, cachedGroup);
		}

		GroupFirestoreManager.getInstance(ctx).getGroup(groupId, new GroupFirestoreManager.DocCallback() {
			@Override
			public void onSuccess(Map<String, Object> data) {
				if (data == null) return;
				com.harvanna.app.offline.OfflineCacheManager.save(ctx, "group_" + groupId, data);
				_bindOverview(root, data);
			}
			@Override
			public void onError(String error) {
				// A genuine connectivity failure leaves whatever cached
				// render already happened above alone.
			}
		});
	}

	private void _bindOverview(final View root, final Map<String, Object> data) {
		root.post(new Runnable() {
			@Override
			public void run() {
				try {
					com.harvanna.app.GroupUiBinder.bindOverview(root, data, 0);
				} catch (Throwable ignored) {}
				GroupTypeUiController.applyOverviewPanels(root, data, isAdmin);
				// Admin panels — inline (no setAdminActionsVisible)
				setVisible(root, "linear_admin_actions", isAdmin);
				setVisible(root, "panel_payout_requests", isAdmin);
				String gw = string(data, "groupWalletAddress", string(data, "group_wallet", ""));
				if (!gw.isEmpty()) loadWalletBalance(root, gw);
			}
		});
	}

	private void loadWalletBalance(final View root, String address) {
		// Offline-first: same "wallet_balance_" + address key convention
		// used everywhere else a wallet balance is cached — a group wallet
		// address is still just an address, safe to share the cache.
		Map<String, Object> cachedBalance = com.harvanna.app.offline.OfflineCacheManager.load(
				ctx, "wallet_balance_" + address);
		if (cachedBalance != null && cachedBalance.get("show") instanceof Number) {
			final double cachedAjc = ((Number) cachedBalance.get("show")).doubleValue();
			root.post(new Runnable() {
				@Override
				public void run() {
					GroupTypeUiController.setGroupWalletBalance(root, cachedAjc);
				}
			});
		}

		AjooNodeClient.getWallet(address, new AjooNodeClient.JsonCallback() {
			@Override
			public void onSuccess(JSONObject json) {
				final double ajc = AjooNodeClient.toAjcDisplay(json.optLong("balance", 0L));
				Map<String, Object> toCache = new java.util.HashMap<>();
				toCache.put("show", ajc);
				com.harvanna.app.offline.OfflineCacheManager.save(ctx, "wallet_balance_" + address, toCache);
				root.post(new Runnable() {
					@Override
					public void run() {
						GroupTypeUiController.setGroupWalletBalance(root, ajc);
					}
				});
			}
			@Override
			public void onError(String error) {
				// A genuine connectivity failure leaves whatever cached
				// balance already rendered above alone.
			}
		});
	}

	private static void setVisible(View root, String idName, boolean visible) {
		int id = root.getResources().getIdentifier(idName, "id", root.getContext().getPackageName());
		if (id == 0) return;
		View v = root.findViewById(id);
		if (v != null) v.setVisibility(visible ? View.VISIBLE : View.GONE);
	}

	private static String string(Map<String, Object> m, String k, String d) {
		Object v = m.get(k);
		return v != null ? String.valueOf(v) : d;
	}
}