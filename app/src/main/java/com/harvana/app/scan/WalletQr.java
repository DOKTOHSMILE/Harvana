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
        /** Display units the receiver asked for, or empty when the code is only an address. */
        public final String amount;
        /** A note the receiver asked to attach, or empty. */
        public final String memo;
        public Hit(String key, String address) { this(key, address, "", ""); }
        public Hit(String key, String address, String amount, String memo) {
            this.key = key;
            this.address = address;
            this.amount = amount == null ? "" : amount;
            this.memo = memo == null ? "" : memo;
        }
    }

    /** The text to put in that coin's QR. A Harvana address stays plain so older codes still scan. */
    public static String payload(String key, String address) {
        return payload(key, address, "", "");
    }

    /**
     * Same as {@link #payload(String, String)} when amount and memo are empty.
     * Otherwise a payment request: harvana://pay for a Harvana coin, or the coin's own scheme plus amount and memo.
     */
    public static String payload(String key, String address, String amount, String memo) {
        if (address == null) return "";
        String a = address.trim();
        String amt = cleanAmount(amount);
        String note = cleanMemo(memo);
        if (amt.isEmpty() && note.isEmpty()) return plain(key, a);
        boolean harv = key == null || key.isEmpty() || "HARV".equals(key) || com.harvana.app.husd.Stable.isStable(key);
        if (harv) {
            String asset = key == null || key.isEmpty() ? "HARV" : key;
            StringBuilder sb = new StringBuilder("harvana://pay?address=").append(url(a)).append("&asset=").append(url(asset));
            if (!amt.isEmpty()) sb.append("&amount=").append(url(amt));
            if (!note.isEmpty()) sb.append("&memo=").append(url(note));
            return sb.toString();
        }
        String base = plain(key, a);
        StringBuilder sb = new StringBuilder(base);
        sb.append(base.indexOf('?') >= 0 ? '&' : '?');
        boolean any = false;
        if (!amt.isEmpty()) { sb.append("amount=").append(url(amt)); any = true; }
        if (!note.isEmpty()) { if (any) sb.append('&'); sb.append("message=").append(url(note)); }
        return sb.toString();
    }

    private static String plain(String key, String a) {
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
        if (t.regionMatches(true, 0, "harvana://pay", 0, "harvana://pay".length())) return pay(t);
        int schemeAt = t.indexOf(':');
        if (schemeAt > 0 && schemeAt < 16 && !t.startsWith("http")) {
            String scheme = t.substring(0, schemeAt).toLowerCase(java.util.Locale.US);
            String rest = t.substring(schemeAt + 1).trim();
            if (rest.startsWith("//")) return null;
            String amount = "", memo = "";
            int q = rest.indexOf('?');
            if (q > 0) {
                String query = rest.substring(q + 1);
                rest = rest.substring(0, q);
                amount = qparam(query, "amount");
                memo = qparam(query, "memo");
                if (memo.isEmpty()) memo = qparam(query, "message");
            }
            String chain = "";
            int at = rest.indexOf('@');
            if (at > 0) { chain = rest.substring(at + 1); rest = rest.substring(0, at); }
            rest = rest.trim();
            String key = keyForScheme(scheme, chain);
            if (key == null) return null;
            return looksLike(key, rest) ? new Hit(key, rest, cleanAmount(amount), cleanMemo(memo)) : null;
        }
        return bare(t);
    }

    private static Hit pay(String t) {
        int q = t.indexOf('?');
        if (q < 0) return null;
        String query = t.substring(q + 1);
        String address = qparam(query, "address");
        if (!looksLike("HARV", address)) return null;
        String asset = qparam(query, "asset");
        if (asset.isEmpty()) asset = "HARV";
        String key = "HARV".equalsIgnoreCase(asset) || com.harvana.app.husd.Stable.isStable(asset) ? asset.toUpperCase(java.util.Locale.US) : "HARV";
        if ("HARV".equals(key)) key = "HARV";
        String norm = "HV0x" + address.substring(4).toLowerCase(java.util.Locale.US);
        return new Hit(key, norm, cleanAmount(qparam(query, "amount")), cleanMemo(qparam(query, "memo")));
    }

    private static String qparam(String query, String name) {
        if (query == null || query.isEmpty()) return "";
        for (String part : query.split("&")) {
            int eq = part.indexOf('=');
            String k = eq < 0 ? part : part.substring(0, eq);
            if (!name.equalsIgnoreCase(urlDec(k))) continue;
            return eq < 0 ? "" : urlDec(part.substring(eq + 1)).trim();
        }
        return "";
    }

    private static String url(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20"); }
        catch (Exception e) { return s; }
    }

    private static String urlDec(String s) {
        try { return java.net.URLDecoder.decode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }

    /** Digits and one dot. Anything else is dropped so a bad field cannot change the payment. */
    public static String cleanAmount(String amount) {
        if (amount == null) return "";
        String s = amount.trim().replace(",", "");
        if (s.isEmpty()) return "";
        boolean dot = false;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') b.append(c);
            else if (c == '.' && !dot) { dot = true; b.append(c); }
            else return "";
        }
        if (b.length() == 0 || ".".equals(b.toString())) return "";
        return b.toString();
    }

    public static String cleanMemo(String memo) {
        if (memo == null) return "";
        String s = memo.trim().replace('\n', ' ').replace('\r', ' ');
        if (s.length() > com.harvana.app.tx.TransactionRequest.MAX_MEMO_LENGTH) {
            s = s.substring(0, com.harvana.app.tx.TransactionRequest.MAX_MEMO_LENGTH);
        }
        return s;
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
