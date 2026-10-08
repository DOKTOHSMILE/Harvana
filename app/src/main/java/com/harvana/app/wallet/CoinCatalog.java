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
        return pattern.replace("{address}", a);
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
    }

    private static final List<Coin> ALL;
    static {
        List<Coin> a = new ArrayList<>();
        a.add(new Coin("HARV", "Harvana", "HARV", "Harvana Protocol", "Native coin",
                "The coin this app settles in. Groups, staking and transfers on Harvana use HARV.",
                R.drawable.ic_coin_harv, 8, null, null, true));
        a.add(new Coin("PI", "Pi Network", "PI", Net.label("Pi Network"), "Stellar-style account",
                "Opened by the same recovery phrase. Pi only becomes active after the Pi network creates the account.",
                R.drawable.ic_coin_pi, 7, null, null, false));
        a.add(new Coin("BTC", "Bitcoin", "BTC", Net.label("Bitcoin"), "Native SegWit (BIP-84)",
                "Your Bitcoin address from this recovery phrase. The same words open it in other Bitcoin wallets.",
                R.drawable.ic_coin_btc, 8, ChainKeys.Chain.BTC, null, false));
        a.add(new Coin("LTC", "Litecoin", "LTC", Net.label("Litecoin"), "Native SegWit (BIP-84)",
                "Your Litecoin address from this recovery phrase.",
                R.drawable.ic_coin_ltc, 8, ChainKeys.Chain.LTC, null, false));
        a.add(new Coin("DOGE", "Dogecoin", "DOGE", Net.label("Dogecoin"), "Legacy address (BIP-44)",
                "Your Dogecoin address from this recovery phrase.",
                R.drawable.ic_coin_doge, 8, ChainKeys.Chain.DOGE, null, false));
        a.add(new Coin("ETH", "Ethereum", "ETH", Net.evm("ETH").name, "EVM address (BIP-44)",
                "Your Ethereum address. The same address is also used on other EVM chains, listed separately.",
                R.drawable.ic_coin_eth, 18, ChainKeys.Chain.ETH, "eth", false));
        a.add(new Coin("BNB", "BNB Chain", "BNB", Net.evm("BNB").name, "EVM address (same as Ethereum)",
                "BNB on BNB Smart Chain. It uses the same address as your Ethereum wallet.",
                R.drawable.ic_coin_bnb, 18, ChainKeys.Chain.ETH, "bnb", false));
        a.add(new Coin("POL", "Polygon", "POL", Net.evm("POL").name, "EVM address (same as Ethereum)",
                "POL on Polygon. It uses the same address as your Ethereum wallet.",
                R.drawable.ic_coin_pol, 18, ChainKeys.Chain.ETH, "pol", false));
        a.add(new Coin("BASE", "Ethereum on Base", "ETH", Net.evm("BASE").name, "EVM address (same as Ethereum)",
                "ETH on the Base network. It uses the same address as your Ethereum wallet, but the balance is separate.",
                R.drawable.ic_coin_base, 18, ChainKeys.Chain.ETH, "base", false));
        a.add(new Coin("TRX", "Tron", "TRX", Net.TEST ? "Tron Nile" : "Tron", "TRON address (BIP-44)",
                "Your Tron address from this recovery phrase.",
                R.drawable.ic_coin_trx, 6, ChainKeys.Chain.TRX, null, false));
        a.add(new Coin("SOL", "Solana", "SOL", Net.TEST ? "Solana devnet" : "Solana", "Ed25519 (SLIP-0010)",
                "Your Solana address from this recovery phrase. The same words open it in Phantom and other Solana wallets.",
                R.drawable.ic_coin_sol, 9, ChainKeys.Chain.SOL, null, false));
        ALL = Collections.unmodifiableList(a);
    }

    public static List<Coin> all() { return ALL; }

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
