import com.harvana.app.wallet.PiKeys;
import com.harvana.app.wallet.PiTx;
/** Prints envelopes for fixed inputs; pi_tx_check.py builds the same with the official Stellar SDK and compares. */
public class PiTxCheck { public static void main(String[] a) throws Exception {
  String m = a[0]; byte[] seed = PiKeys.piSeed(m);
  String dest = "GC3MMSXBWHL6CPOAVERSJITX7BH76YU252WGLUOM5CJX3E7UCYZBTPJQ";
  System.out.println(PiTx.envelope(seed, 1234567890123L, dest, PiTx.toStroops("12.5"), 100000, 1800000000L, "", false));
  System.out.println(PiTx.envelope(seed, 1234567890123L, dest, PiTx.toStroops("3"), 100000, 1800000000L, "harvana test", false));
  System.out.println(PiTx.envelope(seed, 77L, dest, PiTx.toStroops("1.0000001"), 100000, 0L, "", true));
} }
