package com.harvana.app.p2p;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import com.harvana.app.R;
import com.harvana.app.firestore.P2PPaymentDetailsManager;
import com.harvana.app.firestore.P2PTradeStatsManager;
import com.harvana.app.firestore.UserProfileManager;
import com.harvana.app.sync.HarvanaNodeClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The open offers of one side and one coin, joined with what the marketplace shows about each poster: name and photo,
 * online, verified / insured / premium, trust score, trades and completion. The node's listing is the truth for amounts
 * and price; Firestore adds the display-only parts.
 */
public final class P2pOffers {

	private P2pOffers() {}

	/** One offer as a row shows it. */
	public static final class Offer {
		public JSONObject raw;
		public String listingId = "";
		/** "SELL": the poster sells the coin (a buyer takes it). "BUY": the poster buys it (a seller takes it). */
		public String side = "SELL";
		public String posterAddress = "";
		public String posterUid = "";
		public String asset = P2pAsset.HARV;
		public double remaining, minOrder, unitPrice = -1;
		public long paymentWindowMinutes;
		public final Set<String> paymentLabels = new LinkedHashSet<>();
		public String displayName = "Trader";
		public String currency = "NGN";
		public String iconUrl = "";
		public String terms = "";
		public boolean online, verified, insured, premium;
		public int trustScore = -1;
		public String trustLabel = "";
		public int completedTrades;
		public double completionPercent = -1;
		public int uniqueCounterparties;

		public double maxOrder() { return remaining; }
	}

	public interface Callback {
		void loaded(List<Offer> offers);
		void failed(String error);
	}

	private static final Handler MAIN = new Handler(Looper.getMainLooper());

	/** Loads every open offer of {@code side} ("SELL" or "BUY") in {@code asset}; callbacks arrive on the main thread. */
	public static void load(Context ctx, String side, String asset, Callback cb) {
		Context app = ctx.getApplicationContext();
		HarvanaNodeClient.getP2POpenListings(0, 100, side, asset, new HarvanaNodeClient.JsonCallback() {
			@Override public void onSuccess(JSONObject result) {
				List<JSONObject> raws = new ArrayList<>();
				JSONArray arr = result.optJSONArray("listings");
				for (int i = 0; arr != null && i < arr.length(); i++) {
					JSONObject l = arr.optJSONObject(i);
					if (l != null) raws.add(l);
				}
				if (raws.isEmpty()) { MAIN.post(() -> cb.loaded(new ArrayList<>())); return; }
				List<Offer> out = Collections.synchronizedList(new ArrayList<>());
				AtomicInteger left = new AtomicInteger(raws.size());
				for (JSONObject raw : raws) {
					enrich(app, raw, o -> {
						out.add(o);
						if (left.decrementAndGet() == 0) {
							List<Offer> copy = new ArrayList<>(out);
							MAIN.post(() -> cb.loaded(copy));
						}
					});
				}
			}
			@Override public void onError(String error) { MAIN.post(() -> cb.failed(error)); }
		});
	}

	public interface OneCallback {
		void loaded(Offer offer);
		void failed(String error);
	}

	/** One offer by id, with everything the marketplace knows about its poster; callbacks on the main thread. */
	public static void loadOne(Context ctx, String listingId, OneCallback cb) {
		Context app = ctx.getApplicationContext();
		HarvanaNodeClient.getP2PListing(listingId, new HarvanaNodeClient.JsonCallback() {
			@Override public void onSuccess(JSONObject raw) { enrich(app, raw, o -> MAIN.post(() -> cb.loaded(o))); }
			@Override public void onError(String error) { MAIN.post(() -> cb.failed(error)); }
		});
	}

	private interface Done { void done(Offer o); }

	private static void enrich(Context app, JSONObject raw, Done done) {
		Offer e = new Offer();
		e.raw = raw;
		e.listingId = raw.optString("listingId", "");
		e.side = "BUY".equals(raw.optString("side", "")) ? "BUY" : "SELL";
		e.posterAddress = "BUY".equals(e.side) ? raw.optString("buyerAddress", "") : raw.optString("sellerAddress", "");
		e.asset = P2pAsset.of(raw);
		e.remaining = P2pAsset.fromRaw(raw.optLong("remainingAmount", 0), e.asset);
		e.minOrder = P2pAsset.fromRaw(raw.optLong("minOrderAmount", 0), e.asset);
		e.paymentWindowMinutes = raw.optLong("paymentWindowMinutes", 0);
		long nodeKobo = raw.optLong("unitPricePerHarvRaw", 0L);   // the price the node signed into the listing
		if (nodeKobo > 0) e.unitPrice = nodeKobo / 100.0;
		if (e.listingId.isEmpty() || e.posterAddress.isEmpty()) { done.done(e); return; }

		P2PPaymentDetailsManager.getInstance(app).get(e.listingId, new P2PPaymentDetailsManager.DocCallback() {
			@Override public void onSuccess(Map<String, Object> data) {
				Object price = data.get("unitPrice");
				if (price instanceof Number && ((Number) price).doubleValue() > 0) e.unitPrice = ((Number) price).doubleValue();
				Object currency = data.get("currency");
				if (currency != null && !String.valueOf(currency).trim().isEmpty()) e.currency = String.valueOf(currency).trim().toUpperCase(Locale.ROOT);
				Object terms = data.get("termsOfTrade");
				if (terms != null) e.terms = String.valueOf(terms).trim();
				Object uid = data.get("sellerUid");
				if (uid == null) uid = data.get("posterUid");
				e.posterUid = uid != null ? String.valueOf(uid) : "";
				Object names = data.get("paymentMethodNames");
				if (names instanceof List) for (Object n : (List<?>) names) {
					if (n != null && !String.valueOf(n).trim().isEmpty()) e.paymentLabels.add(String.valueOf(n).trim());
				}
				Object methods = e.paymentLabels.isEmpty() ? data.get("paymentMethods") : null;
				if (methods instanceof List) for (Object m : (List<?>) methods) {
					if (!(m instanceof Map)) continue;
					Object primary = ((Map<?, ?>) m).get("primaryField");
					if (primary != null && !String.valueOf(primary).trim().isEmpty()) e.paymentLabels.add(String.valueOf(primary).trim());
				}
				if (e.posterUid.isEmpty()) { done.done(e); return; }
				AtomicInteger inner = new AtomicInteger(2);
				Runnable maybe = () -> { if (inner.decrementAndGet() == 0) done.done(e); };
				profile(app, e, maybe);
				trust(app, e, maybe);
			}
			@Override public void onError(String error) { done.done(e); }
		});
	}

	private static void profile(Context app, Offer e, Runnable done) {
		UserProfileManager.getInstance(app).getProfile(e.posterUid, new UserProfileManager.DocCallback() {
			@Override public void onSuccess(Map<String, Object> p) {
				e.displayName = displayName(p.get("first_name"), p.get("last_name"));
				Object icon = p.get("icon");
				e.iconUrl = icon != null ? String.valueOf(icon) : "";
				e.online = UserProfileManager.isRecentlyActive(p);
				done.run();
			}
			@Override public void onError(String error) { done.run(); }
		});
	}

	private static void trust(Context app, Offer e, Runnable done) {
		com.harvana.app.TrustScoreEngine.compute(app, e.posterUid, new com.harvana.app.TrustScoreEngine.Callback() {
			@Override public void onComplete(com.harvana.app.TrustScoreEngine.Result r) {
				e.verified = r.verified;
				int reports = r.reportCount;
				com.harvana.app.trust.NetworkTrust.wallet(e.posterAddress, score -> {
					e.trustScore = score.published ? score.score : -1;
					e.trustLabel = score.label();
					HarvanaNodeClient.getP2PInsuranceStatus(e.posterAddress, new HarvanaNodeClient.JsonCallback() {
						@Override public void onSuccess(JSONObject ins) {
							e.insured = ins.optBoolean("meetsInsuranceThreshold", false) && e.verified && reports == 0;
							stats(app, e, done);
						}
						@Override public void onError(String error) { stats(app, e, done); }
					});
				});
			}
			@Override public void onError(String error) { done.run(); }
		});
	}

	private static void stats(Context app, Offer e, Runnable done) {
		P2PTradeStatsManager.getInstance(app).getFullStats(e.posterUid, new P2PTradeStatsManager.FullStatsCallback() {
			@Override public void onSuccess(int total, int successful, double successRatePercent, int unique) {
				e.completedTrades = total;
				e.completionPercent = total > 0 ? successRatePercent : -1;
				e.uniqueCounterparties = unique;
				e.premium = e.insured && total >= 100;
				done.run();
			}
			@Override public void onError(String error) { done.run(); }
		});
	}

	public static String displayName(Object first, Object last) {
		String f = first != null ? String.valueOf(first).trim() : "";
		String l = last != null ? String.valueOf(last).trim() : "";
		if (f.isEmpty()) return "Trader";
		return l.isEmpty() ? f : f + " " + l.substring(0, 1).toUpperCase(Locale.US) + ".";
	}

	/** The coin's logo: HARV's own, or the stable coin's badge. */
	public static void coinIcon(ImageView v, String asset) {
		if (v == null) return;
		v.setImageTintList(null);
		if (P2pAsset.isStable(asset)) v.setImageDrawable(com.harvana.app.husd.StableIcon.drawable(v.getContext(), P2pAsset.normalize(asset)));
		else v.setImageResource(R.drawable.ic_coin_harv);
	}
}
