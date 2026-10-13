package com.harvana.app.wallet;

import android.content.Context;

import com.harvana.app.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every coin the wallet can show. HARV is the native coin. The others come from the same recovery phrase
 * and each one is its own asset: its own row, its own detail page, and its own choice in Send and Receive.
 */
public final class CoinCatalog {
    private CoinCatalog() {}

    public static final String EXTRA = "assetKey";

    /** Harvana block explorer. {address} is replaced with the wallet address. */
    public static final String HARV_EXPLORER = "https://harvannaprotocol.com/explorer/#/address/{address}";

    /** The explorer page for this coin and address, or empty when that site is not linked yet. */
    public static String explorerUrl(Coin coin, String address) {
        if (coin == null) return "";
        String a = address == null ? "" : address.trim();
        String pattern;
        switch (coin.key) {
            case "HARV": pattern = HARV_EXPLORER; break;
            case "PI": pattern = Net.piAddressPage("{address}"); break;
            case "BTC": pattern = Net.btcPage("address", "{address}"); break;
            case "LTC": pattern = Net.ltcPage("address", "{address}"); break;
            case "DOGE": pattern = Net.dogeAddressPage("{address}"); break;
            case "ETH": pattern = Net.evm("ETH").addressPage + "{address}"; break;
            case "BNB": pattern = Net.evm("BNB").addressPage + "{address}"; break;
            case "POL": pattern = Net.evm("POL").addressPage + "{address}"; break;
            case "BASE": pattern = Net.evm("BASE").addressPage + "{address}"; break;
            case "TRX": pattern = Net.tronAddressPage("{address}"); break;
            case "SOL": pattern = Net.solAddressPage("{address}"); break;
            default: pattern = "";
        }
        if (pattern == null || pattern.trim().isEmpty() || a.isEmpty()) return "";
        return pattern.replace("{address}", android.net.Uri.encode(a));
    }

    public static final class Coin {
        public final String key, name, ticker, network, standard, about;
        public final int icon, decimals;
        public final ChainKeys.Chain chain;
        /** When set, this coin uses the Ethereum address but a different chain. */
        public final String evm;
        public final boolean nativeHarv;

        Coin(String key, String name, String ticker, String network, String standard, String about,
             int icon, int decimals, ChainKeys.Chain chain, String evm, boolean nativeHarv) {
            this.key = key; this.name = name; this.ticker = ticker; this.network = network;
            this.standard = standard; this.about = about; this.icon = icon; this.decimals = decimals;
            this.chain = chain; this.evm = evm; this.nativeHarv = nativeHarv;
        }

        /** The name of the smallest unit this coin is counted in. */
        public String smallestUnit() {
            switch (key) {
                case "BTC": return "satoshi · 100,000,000 per BTC";
                case "LTC": return "litoshi · 100,000,000 per LTC";
                case "DOGE": return "koinu · 100,000,000 per DOGE";
                case "ETH":
                case "BASE": return "wei · 10^18 per ETH";
                case "BNB": return "wei · 10^18 per BNB";
                case "POL": return "wei · 10^18 per POL";
                case "TRX": return "sun · 1,000,000 per TRX";
                case "SOL": return "lamport · 1,000,000,000 per SOL";
                case "PI": return "stroop · 10,000,000 per PI";
                default: return "100,000,000 per HARV";
            }
        }

        /** The network name for the choice saved now, not the one from when the app started. */
        public String liveNetwork() {
            if (nativeHarv) return Net.TEST ? "Harvana Protocol testnet" : "Harvana Protocol";
            switch (key) {
                case "PI": return Net.label("Pi Network");
                case "BTC": return Net.label("Bitcoin");
                case "LTC": return Net.label("Litecoin");
                case "DOGE": return Net.label("Dogecoin");
                case "ETH":
                case "BNB":
                case "POL":
                case "BASE": return Net.evm(key).name;
                case "TRX": return Net.TEST ? "Tron Nile" : "Tron";
                case "SOL": return Net.TEST ? "Solana devnet" : "Solana";
                default: return network;
            }
        }
    }

    private static final List<Coin> ALL;
    static {
        List<Coin> a = new ArrayList<>();
        a.add(new Coin("HARV", "Harvana", "HARV", "Harvana Protocol", "Native coin",
                "HARV is the coin of Harvana Protocol. It pays the tFare for every action on the network, rewards miners and stakers, and pairs with every Harvana stable coin in Harvana Swap. Groups save and lend in stable coins; HARV pays their tFare. One HARV is 100,000,000 of the smallest unit.",
                R.drawable.ic_coin_harv, 8, null, null, true));
        a.add(new Coin("PI", "Pi Network", "PI", Net.label("Pi Network"), "Stellar-style account",
                "Pi is a mobile-mined cryptocurrency. An account address starts with G and has 56 characters. A new account stays inactive until the Pi network itself creates it with a minimum balance. One Pi is 10,000,000 stroops.",
                R.drawable.ic_coin_pi, 7, null, null, false));
        a.add(new Coin("BTC", "Bitcoin", "BTC", Net.label("Bitcoin"), "Native SegWit",
                "Bitcoin is a peer-to-peer digital currency with a capped supply of 21 million. This address is native SegWit. One bitcoin is 100,000,000 satoshis, and the network fee is paid in bitcoin.",
                R.drawable.ic_coin_btc, 8, ChainKeys.Chain.BTC, null, false));
        a.add(new Coin("LTC", "Litecoin", "LTC", Net.label("Litecoin"), "Native SegWit",
                "Litecoin is a payment coin based on Bitcoin, with a block about every two and a half minutes and a capped supply of 84 million. This address is native SegWit. One litecoin is 100,000,000 litoshis.",
                R.drawable.ic_coin_ltc, 8, ChainKeys.Chain.LTC, null, false));
        a.add(new Coin("DOGE", "Dogecoin", "DOGE", Net.label("Dogecoin"), "Legacy address",
                "Dogecoin is a payment coin with a new block about every minute and no supply cap. One dogecoin is 100,000,000 koinu, and the network fee is paid in dogecoin.",
                R.drawable.ic_coin_doge, 8, ChainKeys.Chain.DOGE, null, false));
        a.add(new Coin("ETH", "Ethereum", "ETH", Net.evm("ETH").name, "EVM account",
                "Ethereum is a programmable blockchain. Ether pays the gas fee for every transaction. One ether is 10^18 wei. The account is a 42-character address starting with 0x.",
                R.drawable.ic_coin_eth, 18, ChainKeys.Chain.ETH, "eth", false));
        a.add(new Coin("BNB", "BNB Chain", "BNB", Net.evm("BNB").name, "EVM account",
                "BNB is the coin of BNB Smart Chain and pays the gas fee on that network. One BNB is 10^18 of the smallest unit. The balance here is not the BNB you may hold as a token on Ethereum.",
                R.drawable.ic_coin_bnb, 18, ChainKeys.Chain.ETH, "bnb", false));
        a.add(new Coin("POL", "Polygon", "POL", Net.evm("POL").name, "EVM account",
                "POL is the coin of the Polygon network and pays the gas fee there. One POL is 10^18 of the smallest unit. A Polygon balance is separate from an Ethereum balance.",
                R.drawable.ic_coin_pol, 18, ChainKeys.Chain.ETH, "pol", false));
        a.add(new Coin("BASE", "Ethereum on Base", "ETH", Net.evm("BASE").name, "EVM account",
                "Base is a layer-2 network built on Ethereum. The coin is ether, and it pays the gas fee on Base. This balance is not the same as ether held on Ethereum itself.",
                R.drawable.ic_coin_base, 18, ChainKeys.Chain.ETH, "base", false));
        a.add(new Coin("TRX", "Tron", "TRX", Net.TEST ? "Tron Nile" : "Tron", "Tron account",
                "Tron is a blockchain for transfers and tokens. Fees are paid in TRX. One TRX is 1,000,000 sun. An account address starts with T.",
                R.drawable.ic_coin_trx, 6, ChainKeys.Chain.TRX, null, false));
        a.add(new Coin("SOL", "Solana", "SOL", Net.TEST ? "Solana devnet" : "Solana", "Ed25519 account",
                "Solana is a high-throughput blockchain. Fees are paid in SOL. One SOL is 1,000,000,000 lamports. An account address is a base58 string of about 44 characters.",
                R.drawable.ic_coin_sol, 9, ChainKeys.Chain.SOL, null, false));
        ALL = Collections.unmodifiableList(a);
    }

    public static List<Coin> all() { return ALL; }

    /** A Harvana stable coin (HNGN, HUSD, …) as a coin to receive: it lives on the wallet's Harvana address. */
    public static Coin stable(String code) {
        return new Coin(code, com.harvana.app.husd.Stable.name(code), com.harvana.app.husd.Stable.symbol(code), "Harvana Protocol",
                "Stable coin", "", "HUSD".equals(code) ? R.drawable.ic_coin_husd : R.drawable.ic_coin_harv, 6, null, null, true);
    }

    /** Every coin except HARV, in the order they appear under it. */
    public static List<Coin> others() {
        List<Coin> o = new ArrayList<>();
        for (Coin c : ALL) if (!c.nativeHarv) o.add(c);
        return o;
    }

    public static Coin get(String key) {
        if (key == null || key.isEmpty()) return ALL.get(0);
        for (Coin c : ALL) if (c.key.equals(key)) return c;
        return ALL.get(0);
    }

    public static Coin harv() { return ALL.get(0); }

    /**
     * The address already saved for this coin. Does not derive, so it is safe on the main thread.
     * Every screen must use this, not an address it remembered earlier: a remembered one can be from an older derivation.
     */
    public static String savedAddress(Context c, String harvAddress, String key) {
        if (c == null || harvAddress == null || harvAddress.isEmpty() || key == null) return "";
        if ("HARV".equals(key)) return harvAddress;
        if ("PI".equals(key)) return PiWalletStore.get(c, harvAddress);
        Coin coin = null;
        for (Coin x : ALL) if (x.key.equals(key)) coin = x;
        if (coin == null) return "";
        ChainKeys.Chain chain = coin.evm != null ? ChainKeys.Chain.ETH : coin.chain;
        return ChainWalletStore.saved(c, harvAddress, chain);
    }

    /**
     * Addresses for every coin, derived from the recovery phrase when needed.
     * Run off the main thread: the first call can take a moment.
     */
    public static Map<String, String> addresses(Context c, String harvAddress) {
        Map<String, String> out = new LinkedHashMap<>();
        if (c == null || harvAddress == null || harvAddress.isEmpty()) return out;
        out.put("HARV", harvAddress);
        Map<ChainKeys.Chain, String> chains = ChainWalletStore.ensure(c, harvAddress);
        String eth = chains.get(ChainKeys.Chain.ETH);
        String pi = PiWalletStore.get(c, harvAddress);
        for (Coin coin : others()) {
            if ("PI".equals(coin.key)) {
                if (pi != null && !pi.isEmpty()) out.put(coin.key, pi);
            } else if (coin.evm != null) {
                if (eth != null && !eth.isEmpty()) out.put(coin.key, eth);
            } else if (coin.chain != null) {
                String a = chains.get(coin.chain);
                if (a != null && !a.isEmpty()) out.put(coin.key, a);
            }
        }
        return out;
    }
}
