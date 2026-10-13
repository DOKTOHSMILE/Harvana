package com.harvana.app.scan;

/**
 * A payment code for any coin the wallet holds. The scanner reads what Harvana prints and what other wallets print,
 * and Harvana's own code carries the coin so a scan opens the right one.
 */
public final class WalletQr {
    private WalletQr() {}

    public static final class Hit {
        public final String key;
        public final String address;
        public Hit(String key, String address) { this.key = key; this.address = address; }
    }

    /** The text to put in that coin's QR. A Harvana address stays plain so older codes still scan. */
    public static String payload(String key, String address) {
        if (address == null) return "";
        String a = address.trim();
        if (key == null) return a;
        switch (key) {
            case "BTC": return "bitcoin:" + a;
            case "LTC": return "litecoin:" + a;
            case "DOGE": return "dogecoin:" + a;
            case "ETH": return "ethereum:" + a;
            case "BNB": return "ethereum:" + a + "@56";
            case "POL": return "ethereum:" + a + "@137";
            case "BASE": return "ethereum:" + a + "@8453";
            case "TRX": return "tron:" + a;
            case "SOL": return "solana:" + a;
            case "PI": return "pi:" + a;
            default: return a;
        }
    }

    /** The coin and the plain address inside a scanned code, or null when it is not a wallet. */
    public static Hit parse(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.isEmpty()) return null;
        int schemeAt = t.indexOf(':');
        if (schemeAt > 0 && schemeAt < 16 && !t.startsWith("http")) {
            String scheme = t.substring(0, schemeAt).toLowerCase(java.util.Locale.US);
            String rest = t.substring(schemeAt + 1).trim();
            if (rest.startsWith("//")) return null;
            String chain = "";
            int at = rest.indexOf('@');
            if (at > 0) { chain = rest.substring(at + 1); rest = rest.substring(0, at); }
            int q = rest.indexOf('?');
            if (q > 0) rest = rest.substring(0, q);
            rest = rest.trim();
            String key = keyForScheme(scheme, chain);
            if (key == null) return null;
            return looksLike(key, rest) ? new Hit(key, rest) : null;
        }
        return bare(t);
    }

    private static String keyForScheme(String scheme, String chain) {
        switch (scheme) {
            case "bitcoin": return "BTC";
            case "litecoin": return "LTC";
            case "dogecoin": return "DOGE";
            case "tron": return "TRX";
            case "solana": return "SOL";
            case "pi": return "PI";
            case "harvana": return "HARV";
            case "ethereum":
            case "bnb":
            case "polygon":
            case "matic":
            case "base":
                return evm(scheme, chain);
            default: return null;
        }
    }

    private static String evm(String scheme, String chain) {
        String id = chain;
        int q = id.indexOf('?');
        if (q > 0) id = id.substring(0, q);
        id = id.trim();
        if (id.startsWith("eip155:")) id = id.substring(7);
        switch (id) {
            case "56": return "BNB";
            case "137": return "POL";
            case "8453": return "BASE";
            case "1": return "ETH";
            default:
                if ("bnb".equals(scheme)) return "BNB";
                if ("polygon".equals(scheme) || "matic".equals(scheme)) return "POL";
                if ("base".equals(scheme)) return "BASE";
                return "ETH";
        }
    }

    private static Hit bare(String t) {
        if (t.regionMatches(true, 0, "HV0x", 0, 4) && t.length() == 44 && t.substring(4).matches("[0-9a-fA-F]{40}"))
            return new Hit("HARV", "HV0x" + t.substring(4).toLowerCase(java.util.Locale.US));
        if (t.startsWith("bc1") || t.startsWith("BC1") || t.startsWith("tb1") || t.startsWith("TB1")) return looksLike("BTC", t) ? new Hit("BTC", t.toLowerCase(java.util.Locale.US)) : null;
        if (t.startsWith("ltc1") || t.startsWith("LTC1") || t.startsWith("tltc1") || t.startsWith("TLTC1")) return looksLike("LTC", t) ? new Hit("LTC", t.toLowerCase(java.util.Locale.US)) : null;
        if (t.matches("(?i)0x[0-9a-f]{40}")) return new Hit("ETH", t);
        if (t.startsWith("T") && t.matches("^T[1-9A-HJ-NP-Za-km-z]{33}$")) return new Hit("TRX", t);
        if (t.startsWith("G") && t.matches("^G[A-Z2-7]{55}$")) return new Hit("PI", t);
        if (t.matches("^D[1-9A-HJ-NP-Za-km-z]{25,34}$")) return new Hit("DOGE", t);
        if (com.harvana.app.wallet.Net.TEST && t.matches("^[mn2][1-9A-HJ-NP-Za-km-z]{25,34}$")) {
            // Testnet Bitcoin (m…, n…, 2…) and testnet Dogecoin (n…) share letters: the version byte tells them apart.
            int v = base58Version(t);
            if (v == 0x6f || v == 0xc4) return new Hit("BTC", t);
            if (v == 0x71) return new Hit("DOGE", t);
            return null;
        }
        if (t.matches("^[LM][1-9A-HJ-NP-Za-km-z]{25,34}$")) return new Hit("LTC", t);
        if (t.matches("^[13][1-9A-HJ-NP-Za-km-z]{24,34}$")) return new Hit("BTC", t);
        if (t.matches("^[1-9A-HJ-NP-Za-km-z]{32,44}$")) return new Hit("SOL", t);
        return null;
    }

    /** The version byte of a base58 address (its first byte once decoded), or -1. */
    static int base58Version(String a) {
        try {
            java.math.BigInteger v = java.math.BigInteger.ZERO, b = java.math.BigInteger.valueOf(58);
            String alpha = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
            for (char ch : a.toCharArray()) {
                int i = alpha.indexOf(ch);
                if (i < 0) return -1;
                v = v.multiply(b).add(java.math.BigInteger.valueOf(i));
            }
            byte[] raw = v.toByteArray();
            int lead = 0;
            while (lead < a.length() && a.charAt(lead) == '1') lead++;
            if (lead > 0) return 0;                                  // a leading '1' is a zero version byte
            int skip = raw.length > 25 ? raw.length - 25 : 0;       // BigInteger's sign byte
            return raw.length - skip == 25 ? raw[skip] & 0xFF : -1;
        } catch (Exception e) { return -1; }
    }

    private static boolean looksLike(String key, String a) {
        switch (key) {
            case "HARV": return a.regionMatches(true, 0, "HV0x", 0, 4) && a.length() == 44;
            case "BTC": return a.regionMatches(true, 0, "bc1", 0, 3) || a.regionMatches(true, 0, "tb1", 0, 3) || a.matches("^[13][1-9A-HJ-NP-Za-km-z]{24,34}$")
                    || (com.harvana.app.wallet.Net.TEST && (base58Version(a) == 0x6f || base58Version(a) == 0xc4));
            case "LTC": return a.regionMatches(true, 0, "ltc1", 0, 4) || a.regionMatches(true, 0, "tltc1", 0, 5) || a.matches("^[LM][1-9A-HJ-NP-Za-km-z]{25,34}$");
            case "DOGE": return a.matches("^D[1-9A-HJ-NP-Za-km-z]{25,34}$") || (com.harvana.app.wallet.Net.TEST && a.matches("^[n2][1-9A-HJ-NP-Za-km-z]{25,34}$"));
            case "ETH":
            case "BNB":
            case "POL":
            case "BASE": return a.matches("(?i)0x[0-9a-f]{40}");
            case "TRX": return a.matches("^T[1-9A-HJ-NP-Za-km-z]{33}$");
            case "SOL": return a.matches("^[1-9A-HJ-NP-Za-km-z]{32,44}$");
            case "PI": return a.matches("^G[A-Z2-7]{55}$");
            default: return false;
        }
    }
}
