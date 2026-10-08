package com.harvana.app.scan;

/** Checks that a scanned code becomes the right coin and a plain address. */
public class WalletQrCheck {
    public static void main(String[] args) {
        String eth = "0x2c7536E3605D9C16a7a3D7b1898e529396a65c23";
        String pi = "GAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        check("bitcoin:bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu", "BTC", "bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu");
        check("ethereum:" + eth + "@56", "BNB", eth);
        check("ethereum:" + eth + "@137", "POL", eth);
        check("ethereum:" + eth + "@8453", "BASE", eth);
        check("ethereum:" + eth, "ETH", eth);
        check("HV0x" + "ab".repeat(20), "HARV", "HV0x" + "ab".repeat(20));
        check("pi:" + pi, "PI", pi);
        check("solana:So11111111111111111111111111111111111111112", "SOL", "So11111111111111111111111111111111111111112");
        check("tron:TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t", "TRX", "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t");
        if (WalletQr.parse("harvana://user/ada") != null) fail("profile was treated as a wallet");
        String back = WalletQr.payload("BNB", eth);
        WalletQr.Hit again = WalletQr.parse(back);
        if (again == null || !"BNB".equals(again.key) || !eth.equals(again.address)) fail("bnb round trip " + back);
        System.out.println("ALL PASS");
    }

    private static void check(String raw, String key, String address) {
        WalletQr.Hit hit = WalletQr.parse(raw);
        if (hit == null || !key.equals(hit.key) || !address.equals(hit.address)) fail(raw + " -> " + (hit == null ? "null" : hit.key + " " + hit.address));
        else System.out.println(key + " ok");
    }

    private static void fail(String m) { System.out.println("FAIL " + m); System.exit(1); }
}
