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

    /**
     * Harvana block explorer. Leave this empty until the website exists, then paste the address page
     * with {address} where the wallet address goes, for example https://explorer.example/address/{address}.
     * The other coins already have public explorers. This is the one line to change for Harvana.
     */
    public static final String HARV_EXPLORER = "";

    /** The explorer page for this coin and address, or empty when that site is not linked yet. */
    public static String explorerUrl(Coin coin, String address) {
        if (coin == null) return "";
        String a = address == null ? "" : address.trim();
        String pattern;
        switch (coin.key) {
            case "HARV": pattern = HARV_EXPLORER; break;
            case "PI": pattern = "https://blockexplorer.minepi.com/mainnet/accounts/{address}"; break;
            case "BTC": pattern = "https://mempool.space/address/{address}"; break;
            case "LTC": pattern = "https://litecoinspace.org/address/{address}"; break;
            case "DOGE": pattern = "https://blockchair.com/dogecoin/address/{address}"; break;
            case "ETH": pattern = "https://etherscan.io/address/{address}"; break;
            case "BNB": pattern = "https://bscscan.com/address/{address}"; break;
            case "POL": pattern = "https://polygonscan.com/address/{address}"; break;
            case "BASE": pattern = "https://basescan.org/address/{address}"; break;
            case "TRX": pattern = "https://tronscan.org/#/address/{address}"; break;
            case "SOL": pattern = "https://solscan.io/account/{address}"; break;
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
        a.add(new Coin("PI", "Pi Network", "PI", "Pi Network", "Stellar-style account",
                "Opened by the same recovery phrase. Pi only becomes active after the Pi network creates the account.",
                R.drawable.ic_coin_pi, 7, null, null, false));
        a.add(new Coin("BTC", "Bitcoin", "BTC", "Bitcoin", "Native SegWit (BIP-84)",
                "Your Bitcoin address from this recovery phrase. The same words open it in other Bitcoin wallets.",
                R.drawable.ic_coin_btc, 8, ChainKeys.Chain.BTC, null, false));
        a.add(new Coin("LTC", "Litecoin", "LTC", "Litecoin", "Native SegWit (BIP-84)",
                "Your Litecoin address from this recovery phrase.",
                R.drawable.ic_coin_ltc, 8, ChainKeys.Chain.LTC, null, false));
        a.add(new Coin("DOGE", "Dogecoin", "DOGE", "Dogecoin", "Legacy address (BIP-44)",
                "Your Dogecoin address from this recovery phrase.",
                R.drawable.ic_coin_doge, 8, ChainKeys.Chain.DOGE, null, false));
        a.add(new Coin("ETH", "Ethereum", "ETH", "Ethereum", "EVM address (BIP-44)",
                "Your Ethereum address. The same address is also used on other EVM chains, listed separately.",
                R.drawable.ic_coin_eth, 18, ChainKeys.Chain.ETH, "eth", false));
        a.add(new Coin("BNB", "BNB Chain", "BNB", "BNB Smart Chain", "EVM address (same as Ethereum)",
                "BNB on BNB Smart Chain. It uses the same address as your Ethereum wallet.",
                R.drawable.ic_coin_bnb, 18, ChainKeys.Chain.ETH, "bnb", false));
        a.add(new Coin("POL", "Polygon", "POL", "Polygon", "EVM address (same as Ethereum)",
                "POL on Polygon. It uses the same address as your Ethereum wallet.",
                R.drawable.ic_coin_pol, 18, ChainKeys.Chain.ETH, "pol", false));
        a.add(new Coin("BASE", "Ethereum on Base", "ETH", "Base", "EVM address (same as Ethereum)",
                "ETH on the Base network. It uses the same address as your Ethereum wallet, but the balance is separate.",
                R.drawable.ic_coin_base, 18, ChainKeys.Chain.ETH, "base", false));
        a.add(new Coin("TRX", "Tron", "TRX", "Tron", "TRON address (BIP-44)",
                "Your Tron address from this recovery phrase.",
                R.drawable.ic_coin_trx, 6, ChainKeys.Chain.TRX, null, false));
        a.add(new Coin("SOL", "Solana", "SOL", "Solana", "Ed25519 (SLIP-0010)",
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
