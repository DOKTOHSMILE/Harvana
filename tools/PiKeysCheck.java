import com.harvana.app.wallet.PiKeys;
/**
 * Checks PiKeys against the published SEP-0005 test vectors (Stellar's derivation, which Pi uses with coin 314159).
 * Run from the repo root:
 *   javac -d /tmp/pk app/src/main/java/com/harvana/app/wallet/PiKeys.java app/src/main/java/com/harvana/app/crypto/Ed25519.java \
 *         app/src/main/java/com/harvana/app/ChainMath.java tools/PiKeysCheck.java && java -cp /tmp/pk PiKeysCheck
 */
public class PiKeysCheck {
  public static void main(String[] a) throws Exception {
    // SEP-0005 test vector 1 (12 words, Stellar coin 148)
    String m1="illness spike retreat truth genius clock brain pass fit cave bargain toe";
    check(PiKeys.address(m1,148,0),"GDRXE2BQUC3AZNPVFSCEZ76NJ3WWL25FYFK6RGZGIEKWE4SOOHSUJUJ6");
    check(PiKeys.address(m1,148,1),"GBAW5XGWORWVFE2XTJYDTLDHXTY2Q2MO73HYCGB3XMFMQ562Q2W2GJQX");
    // SEP-0005 test vector 2 (24 words)
    String m2="bench hurt jump file august wise shallow faculty impulse spring exact slush thunder author capable act festival slice deposit sauce coconut afford frown better";
    check(PiKeys.address(m2,148,0),"GC3MMSXBWHL6CPOAVERSJITX7BH76YU252WGLUOM5CJX3E7UCYZBTPJQ");
    String pi=PiKeys.piAddress(m2);
    System.out.println("pi address for vector 2: "+pi+" valid="+PiKeys.isValidAddress(pi));
    System.out.println("bad checksum valid? "+PiKeys.isValidAddress("GC3MMSXBWHL6CPOAVERSJITX7BH76YU252WGLUOM5CJX3E7UCYZBTPJA"));
  }
  static void check(String got,String want){ System.out.println((got.equals(want)?"OK   ":"FAIL ")+got+(got.equals(want)?"":" want "+want)); }
}
