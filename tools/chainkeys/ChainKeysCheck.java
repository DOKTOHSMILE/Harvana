/**
 * Prints the addresses ChainKeys derives, to compare with tools/chainkeys/reference.py (eth-account, embit, PyNaCl):
 *   BC=<bcprov-jdk18on jar>; A=app/src/main/java/com/harvana/app
 *   javac -cp $BC -d /tmp/ck $A/wallet/ChainKeys.java $A/wallet/PiKeys.java $A/crypto/Ed25519.java $A/ChainMath.java tools/chainkeys/ChainKeysCheck.java
 *   java -cp /tmp/ck:$BC ChainKeysCheck "<phrase>" > java.txt; python3 tools/chainkeys/reference.py "<phrase>" > ref.txt; diff ref.txt java.txt
 */
import com.harvana.app.wallet.ChainKeys;
public class ChainKeysCheck { public static void main(String[] a) throws Exception {
  for (String m : a) for (ChainKeys.Chain c : ChainKeys.Chain.values()) System.out.println(c.name()+" "+ChainKeys.address(m,c)); } }
