package com.harvana.app.husd;

import android.content.Context;
import android.util.Base64;

import com.harvana.app.crypto.Ed25519;
import com.harvana.app.sync.HarvanaNodeClient;
import com.harvana.app.wallet.WalletManager;
import com.harvana.app.wallet.WalletNodeBridge;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * hUSD, the dollar token on the Harvana chain (node: docs/HUSD.md). 1 hUSD = 1,000,000 units, like USDC.
 * Sending is a normal signed transfer with mintId "HUSD", which the v1 signature covers:
 * from + to + amount + nonce + memo + "HUSD".
 */
public final class HusdClient {
    private HusdClient() {}

    public static final String ASSET = "HUSD";
    public static final long UNIT = 1_000_000L;

    public interface Cb { void ok(JSONObject r); void fail(String error); }

    private static HarvanaNodeClient.JsonCallback wrap(Cb cb) {
        return new HarvanaNodeClient.JsonCallback() {
            @Override public void onSuccess(JSONObject r) { cb.ok(r); }
            @Override public void onError(String e) { cb.fail(e); }
        };
    }

    public static void info(Cb cb) { HarvanaNodeClient.nodeGet("/api/v1/husd/info", wrap(cb)); }

    public static void balance(String address, Cb cb) { HarvanaNodeClient.nodeGet("/api/v1/husd/balance/" + address, wrap(cb)); }

    public static void withdrawals(String address, Cb cb) {
        HarvanaNodeClient.nodeGet("/api/v1/husd/withdrawals?address=" + address + "&limit=20", wrap(cb));
    }

    /** "I sent USDC to my deposit address on this chain": the relayer forwards it into the vault, free to the member. */
    public static void requestSweep(String chain, String address, Cb cb) {
        try {
            JSONObject b = new JSONObject().put("chain", chain).put("address", address);
            HarvanaNodeClient.nodePost("/api/v1/husd/sweep-request", b.toString(), wrap(cb));
        } catch (Exception e) { cb.fail(String.valueOf(e.getMessage())); }
    }

    /** "12.5" → 12,500,000 units; null for anything that isn't a positive amount with at most 6 decimals. */
    public static Long parse(String text) {
        try {
            BigDecimal v = new BigDecimal(text.trim().replace(",", ""));
            if (v.signum() <= 0 || v.scale() > 6) return null;
            return v.movePointRight(6).longValueExact();
        } catch (Exception e) { return null; }
    }

    /** 12,500,000 units → "12.50"; keeps up to 6 decimals, at least 2. */
    public static String format(long units) {
        BigDecimal v = BigDecimal.valueOf(units, 6).stripTrailingZeros();
        if (v.scale() < 2) v = v.setScale(2);
        return v.toPlainString();
    }

    /**
     * Signs and sends hUSD. The node picks the fee: HARV gas if the wallet has it, otherwise a fee in hUSD (the first
     * few are free). {@code memo} is null for a transfer, or "HUSD_WITHDRAW:CHAIN:0x…" for a withdrawal.
     */
    public static void send(Context c, String from, String to, long amountUnits, String memo, Cb cb) {
        WalletNodeBridge.fetchLiveBalance(c, from, new WalletNodeBridge.BalanceCallback() {
            @Override public void onSuccess(double harv, long confirmedNonce, long height) {
                HarvanaNodeClient.getPending(from, new HarvanaNodeClient.JsonCallback() {
                    @Override public void onSuccess(JSONObject p) {
                        int pending = 0;
                        org.json.JSONArray arr = p.optJSONArray("pending");
                        if (arr != null) for (int i = 0; i < arr.length(); i++) {
                            JSONObject t = arr.optJSONObject(i);
                            if (t != null && from.equalsIgnoreCase(t.optString("from"))) pending++;
                        }
                        signAndPost(c, from, to, amountUnits, memo, confirmedNonce + 1 + pending, cb);
                    }
                    @Override public void onError(String e) { signAndPost(c, from, to, amountUnits, memo, confirmedNonce + 1, cb); }
                });
            }
            @Override public void onError(String e) { cb.fail(e); }
        });
    }

    private static void signAndPost(Context c, String from, String to, long amount, String memo, long nonce, Cb cb) {
        try {
            String seedB64 = WalletManager.loadPrivateKey(c, from);
            String pub = WalletManager.getPublicKey(c, from);
            if (seedB64 == null || pub == null) { cb.fail("This wallet's key is not on this phone."); return; }
            String m = memo == null ? "" : memo;
            String canonical = from + to + amount + nonce + m + ASSET;     // the node's v1 canonical message
            byte[] sig = Ed25519.sign(Base64.decode(seedB64, Base64.NO_WRAP), canonical.getBytes(StandardCharsets.UTF_8));
            JSONObject body = new JSONObject();
            body.put("from", from).put("to", to).put("amount", amount).put("gas", 0).put("nonce", nonce)
                    .put("mintId", ASSET).put("publicKey", pub).put("signature", Base64.encodeToString(sig, Base64.NO_WRAP));
            if (!m.isEmpty()) body.put("memo", m);
            HarvanaNodeClient.nodePost("/api/v1/tx/send", body.toString(), wrap(cb));
        } catch (Exception e) {
            cb.fail("Signing failed: " + e.getMessage());
        }
    }

    /** The member's deposit address on a chain (see DepositAddress). */
    public static String depositAddress(String factory, String initCodeHash, String harvanaAddress) {
        return DepositAddress.of(factory, initCodeHash, harvanaAddress);
    }

    public static boolean isEvmAddress(String a) { return a != null && a.matches("^0x[0-9a-fA-F]{40}$"); }
}
