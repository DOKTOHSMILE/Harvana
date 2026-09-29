package com.harvanna.app.wallet;

/**
 * A single non-native token balance held by a wallet.
 * Stored as an entry in the wallet document's "assets" array.
 * Native AJC is NOT represented here — it stays in the wallet's
 * top-level "balance_ajc" field, unaffected by this class.
 */
public class WalletAsset {

    private String  contract;   // HV0x... token contract address
    private String  symbol;     // e.g. "USDT"
    private double  balance;    // token balance held
    private int     decimals;   // token decimal precision

    public WalletAsset() {
        // Empty constructor required for Firestore field mapping
    }

    public WalletAsset(String contract, String symbol, double balance, int decimals) {
        this.contract = contract;
        this.symbol   = symbol;
        this.balance  = balance;
        this.decimals = decimals;
    }

    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("contract", contract);
        map.put("symbol", symbol);
        map.put("balance", balance);
        map.put("decimals", decimals);
        return map;
    }

    public static WalletAsset fromMap(java.util.Map<String, Object> map) {
        if (map == null) return null;
        String contract = map.get("contract") != null ? String.valueOf(map.get("contract")) : "";
        String symbol   = map.get("symbol")   != null ? String.valueOf(map.get("symbol"))   : "";
        double balance  = map.get("balance") instanceof Number
                ? ((Number) map.get("balance")).doubleValue() : 0.0;
        int decimals    = map.get("decimals") instanceof Number
                ? ((Number) map.get("decimals")).intValue() : 18;
        return new WalletAsset(contract, symbol, balance, decimals);
    }

    public String getContract()  { return contract; }
    public String getSymbol()    { return symbol; }
    public double getBalance()   { return balance; }
    public int    getDecimals()  { return decimals; }

    public void setBalance(double balance) { this.balance = balance; }
}