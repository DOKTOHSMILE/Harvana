package com.harvana.app.wallet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * The TRC-20 token standard on Tron: any contract, read with constant calls; a transfer is transfer(address,uint256)
 * built by the Tron API, signed with the wallet's Tron key and broadcast. Run off the main thread.
 */
public final class Trc20 {
    private Trc20() {}

    public static final String FAMILY = "TRON";
    /** The most TRX a token transfer may burn for energy (100 TRX; a plain transfer usually burns far less). */
    private static final long FEE_LIMIT_SUN = 100_000_000L;

    /** A Tron address (T…) as the 20 bytes the contract sees, hex. */
    static String hex20(String address) {
        byte[] raw = ChainSend.b58(address);
        if (raw.length != 25 || raw[0] != 0x41) throw new IllegalArgumentException("Enter a Tron address. It starts with T.");
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < 21; i++) sb.append(String.format("%02x", raw[i] & 0xFF));
        return sb.toString();
    }

    private static String word(String hex) { return String.format("%64s", hex).replace(' ', '0'); }

    /** The contract's answer to a read-only call, hex ("" when none). */
    private static String constant(String contract, String selector, String param) throws Exception {
        JSONObject r = ChainSend.tronPost("/wallet/triggerconstantcontract", new JSONObject().put("owner_address", contract)
                .put("contract_address", contract).put("function_selector", selector).put("parameter", param).put("visible", true).toString());
        JSONArray out = r.optJSONArray("constant_result");
        return out == null || out.length() == 0 ? "" : out.optString(0, "");
    }

    /** {name, symbol, decimals} of a TRC-20 contract; null when the address is not one. */
    public static String[] read(String contract) {
        try {
            hex20(contract);
            String symbol = CustomAssets.abiString("0x" + constant(contract, "symbol()", ""));
            if (symbol.isEmpty()) return null;
            String name = CustomAssets.abiString("0x" + constant(contract, "name()", ""));
            String dec = constant(contract, "decimals()", "");
            int decimals = dec.isEmpty() ? 18 : new BigInteger(dec, 16).intValue();
            return new String[]{name.isEmpty() ? symbol : name, symbol, String.valueOf(decimals)};
        } catch (Exception e) {
            return null;
        }
    }

    public static String balance(CustomAssets.Asset token, String owner) throws Exception {
        String r = constant(token.contract, "balanceOf(address)", word(hex20(owner)));
        return ChainBalances.amount(r.isEmpty() ? BigInteger.ZERO : new BigInteger(r, 16), token.decimals);
    }

    /** Sends the token; the network fee (energy) is paid in TRX. Returns the transaction id. */
    public static String send(CustomAssets.Asset token, String mnemonic, String from, String to, String amount) throws Exception {
        byte[] priv = ChainKeys.privateKey(mnemonic, ChainKeys.Chain.TRX);
        try {
            if (!ChainKeys.address(mnemonic, ChainKeys.Chain.TRX).equals(from)) throw new IllegalArgumentException("This phone's key does not match the address on screen. Open the coin again.");
            BigDecimal d;
            try { d = new BigDecimal(amount != null ? amount.trim().replace(",", ".") : ""); }
            catch (Exception e) { throw new IllegalArgumentException("Enter a valid amount."); }
            if (d.signum() <= 0) throw new IllegalArgumentException("Enter an amount above zero.");
            BigInteger value;
            try { value = d.movePointRight(token.decimals).toBigIntegerExact(); }
            catch (ArithmeticException e) { throw new IllegalArgumentException("Too many decimal places for " + token.symbol + " (maximum " + token.decimals + ")."); }
            if (new BigDecimal(balance(token, from)).movePointRight(token.decimals).toBigInteger().compareTo(value) < 0)
                throw new IllegalArgumentException("Not enough " + token.symbol + ".");
            JSONObject built = ChainSend.tronPost("/wallet/triggersmartcontract", new JSONObject().put("owner_address", from)
                    .put("contract_address", token.contract).put("function_selector", "transfer(address,uint256)")
                    .put("parameter", word(hex20(to)) + word(value.toString(16))).put("fee_limit", FEE_LIMIT_SUN).put("visible", true).toString());
            JSONObject tx = built.optJSONObject("transaction");
            if (tx == null || !tx.has("txID")) {
                JSONObject res = built.optJSONObject("result");
                throw new IllegalArgumentException(res == null ? "Tron could not build the transfer." : res.optString("message", "Tron could not build the transfer."));
            }
            return ChainSend.tronBroadcast(priv, tx);
        } finally {
            java.util.Arrays.fill(priv, (byte) 0);
        }
    }
}
