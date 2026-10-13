package com.harvana.app.wallet;

import android.content.Context;

/**
 * Where each coin is read and sent. The choice is saved on the phone.
 * Testnet is the default, so a payment uses test coins, not real money.
 */
public final class Net {
    private Net() {}

    private static final String PREFS = "harv_net";

    /** True while every coin should use its test network. Read this, do not copy it. */
    public static volatile boolean TEST = true;

    public static void load(Context c) {
        if (c == null) return;
        TEST = c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("test", true);
    }

    public static void setTest(Context c, boolean test) {
        TEST = test;
        if (c != null) {
            c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("test", test).apply();
        }
    }

    public static String label(String live) { return TEST ? live + " testnet" : live; }

    /** Bitcoin test network in use: testnet4 or testnet3 ("testnet"); both have the same address format. */
    private static volatile String btcTestNet = "testnet4";
    private static final java.util.Map<String, Long> btcPicked = new java.util.concurrent.ConcurrentHashMap<>();

    public static String btcApi() { return TEST ? "https://mempool.space/" + btcTestNet : "https://mempool.space"; }

    /**
     * The Bitcoin API for this address. On testnet, faucets fund either testnet4 or testnet3, so the one where the
     * address holds coins (else has history) is used, testnet4 first. Checked again every 10 minutes. Blocking.
     */
    public static String btcApi(String address) {
        if (!TEST || address == null || address.isEmpty()) return btcApi();
        Long at = btcPicked.get(address);
        if (at != null && System.currentTimeMillis() - at < 10 * 60_000L) return btcApi();
        long[] t4 = btcStats("testnet4", address), t3 = btcStats("testnet", address);
        if (t4 != null || t3 != null) {
            if (t4 == null) btcTestNet = "testnet";
            else if (t3 == null) btcTestNet = "testnet4";
            else if (t3[0] > 0 && t4[0] <= 0) btcTestNet = "testnet";
            else if (t4[0] > 0) btcTestNet = "testnet4";
            else btcTestNet = t3[1] > 0 && t4[1] == 0 ? "testnet" : "testnet4";
            btcPicked.put(address, System.currentTimeMillis());
        }
        return btcApi();
    }

    /** {balance incl. unconfirmed, tx count} on one Bitcoin test network, or null when it did not answer. */
    private static long[] btcStats(String net, String address) {
        try {
            java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL("https://mempool.space/" + net + "/api/address/" + address).openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(10000);
            if (c.getResponseCode() != 200) return null;
            java.io.InputStream in = c.getInputStream();
            java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            for (int n; (n = in.read(buf)) > 0; ) b.write(buf, 0, n);
            org.json.JSONObject j = new org.json.JSONObject(b.toString("UTF-8"));
            org.json.JSONObject ch = j.optJSONObject("chain_stats"), mp = j.optJSONObject("mempool_stats");
            long bal = (ch == null ? 0 : ch.optLong("funded_txo_sum") - ch.optLong("spent_txo_sum"))
                    + (mp == null ? 0 : mp.optLong("funded_txo_sum") - mp.optLong("spent_txo_sum"));
            long txs = (ch == null ? 0 : ch.optLong("tx_count")) + (mp == null ? 0 : mp.optLong("tx_count"));
            return new long[]{bal, txs};
        } catch (Exception e) {
            return null;
        }
    }

    public static String btcPage(String kind, String id) {
        return (TEST ? "https://mempool.space/" + btcTestNet + "/" : "https://mempool.space/") + kind + "/" + id;
    }

    public static String ltcApi() { return TEST ? "https://litecoinspace.org/testnet" : "https://litecoinspace.org"; }
    public static String ltcPage(String kind, String id) {
        return (TEST ? "https://litecoinspace.org/testnet/" : "https://litecoinspace.org/") + kind + "/" + id;
    }

    public static String dogeTxs(String address) {
        return TEST ? "https://sochain.com/api/v3/transactions/DOGETEST/" + address + "/1"
                : "https://api.blockcypher.com/v1/doge/main/addrs/" + address + "?limit=20";
    }
    public static String dogeTx(String hash) {
        return TEST ? "https://sochain.com/api/v3/transaction/DOGETEST/" + hash
                : "https://api.blockcypher.com/v1/doge/main/txs/" + hash;
    }
    public static String dogeUtxo(String address) {
        return TEST ? "https://sochain.com/api/v3/unspent_outputs/DOGETEST/" + address + "/1"
                : "https://api.blockcypher.com/v1/doge/main/addrs/" + address + "?unspentOnly=true&limit=50";
    }
    public static String dogeBalance(String address) {
        return TEST ? "https://sochain.com/api/v3/balance/DOGETEST/" + address
                : "https://api.blockcypher.com/v1/doge/main/addrs/" + address + "/balance";
    }
    public static String dogeRpc() { return "https://dogecoin-testnet.drpc.org"; }
    public static String dogeAddressPage(String address) {
        return (TEST ? "https://sochain.com/dogetest/address/" : "https://blockchair.com/dogecoin/address/") + address;
    }
    public static String dogeTxPage(String hash) {
        return TEST ? "https://sochain.com/dogetest/tx/" + hash : "https://blockchair.com/dogecoin/transaction/" + hash;
    }

    public static final class Evm {
        public final String rpc, scout, name, txPage, addressPage;
        public final long chainId;
        Evm(String rpc, long chainId, String scout, String name, String txPage, String addressPage) {
            this.rpc = rpc; this.chainId = chainId; this.scout = scout; this.name = name;
            this.txPage = txPage; this.addressPage = addressPage;
        }
    }

    public static Evm evm(String key) {
        if (!TEST) {
            switch (key == null ? "" : key) {
                case "BNB": return new Evm("https://bsc-rpc.publicnode.com", 56, "", "BNB Chain", "https://bscscan.com/tx/", "https://bscscan.com/address/");
                case "POL": return new Evm("https://polygon-bor-rpc.publicnode.com", 137, "https://polygon.blockscout.com", "Polygon", "https://polygonscan.com/tx/", "https://polygonscan.com/address/");
                case "BASE": return new Evm("https://base-rpc.publicnode.com", 8453, "https://base.blockscout.com", "Base", "https://basescan.org/tx/", "https://basescan.org/address/");
                default: return new Evm("https://ethereum-rpc.publicnode.com", 1, "https://eth.blockscout.com", "Ethereum", "https://etherscan.io/tx/", "https://etherscan.io/address/");
            }
        }
        switch (key == null ? "" : key) {
            case "BNB": return new Evm("https://bsc-testnet-rpc.publicnode.com", 97, "", "BNB testnet", "https://testnet.bscscan.com/tx/", "https://testnet.bscscan.com/address/");
            case "POL": return new Evm("https://polygon-amoy-bor-rpc.publicnode.com", 80002, "", "Polygon Amoy", "https://amoy.polygonscan.com/tx/", "https://amoy.polygonscan.com/address/");
            case "BASE": return new Evm("https://base-sepolia-rpc.publicnode.com", 84532, "https://base-sepolia.blockscout.com", "Base Sepolia", "https://sepolia.basescan.org/tx/", "https://sepolia.basescan.org/address/");
            default: return new Evm("https://ethereum-sepolia-rpc.publicnode.com", 11155111, "https://eth-sepolia.blockscout.com", "Ethereum Sepolia", "https://sepolia.etherscan.io/tx/", "https://sepolia.etherscan.io/address/");
        }
    }

    public static String tronApi() { return TEST ? "https://nile.trongrid.io" : "https://api.trongrid.io"; }
    public static String tronScan() { return TEST ? "https://nileapi.tronscan.org" : "https://apilist.tronscanapi.com"; }
    public static String tronAddressPage(String address) {
        return (TEST ? "https://nile.tronscan.org/#/address/" : "https://tronscan.org/#/address/") + address;
    }
    public static String tronTxPage(String hash) {
        return (TEST ? "https://nile.tronscan.org/#/transaction/" : "https://tronscan.org/#/transaction/") + hash;
    }

    public static String solRpc() { return TEST ? "https://api.devnet.solana.com" : "https://api.mainnet-beta.solana.com"; }
    public static String solAddressPage(String address) {
        return TEST ? "https://explorer.solana.com/address/" + address + "?cluster=devnet" : "https://solscan.io/account/" + address;
    }
    public static String solTxPage(String hash) {
        return TEST ? "https://explorer.solana.com/tx/" + hash + "?cluster=devnet" : "https://solscan.io/tx/" + hash;
    }

    public static String piHorizon() { return TEST ? "https://api.testnet.minepi.com" : "https://api.mainnet.minepi.com"; }
    public static String piPassphrase() { return TEST ? "Pi Testnet" : "Pi Network"; }
    public static String piAddressPage(String address) {
        return (TEST ? "https://blockexplorer.minepi.com/testnet/accounts/" : "https://blockexplorer.minepi.com/mainnet/accounts/") + address;
    }
    public static String piTxPage(String hash) {
        return (TEST ? "https://blockexplorer.minepi.com/testnet/transactions/" : "https://blockexplorer.minepi.com/mainnet/transactions/") + hash;
    }
}
