package com.harvana.app;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.junit.Assert.*;

/** The receipt checks a transaction's Merkle proof with the node's rule (consensus/MerkleProof). */
public class ReceiptProofTest {

    static String d(String s) throws Exception {
        MessageDigest m = MessageDigest.getInstance("SHA-256");
        byte[] b = m.digest(m.digest(s.getBytes(StandardCharsets.UTF_8)));
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    @Test
    public void aThreeLeafTreeProvesItsLastLeaf() throws Exception {
        String a = d("a"), b = d("b"), c = d("c");
        String ab = d(a + b), cc = d(c + c), root = d(ab + cc);
        JSONArray proof = new JSONArray()
                .put(new JSONObject().put("hash", c).put("side", "right"))      // the unpaired last leaf pairs with itself
                .put(new JSONObject().put("hash", ab).put("side", "left"));
        assertTrue(ReceiptActivity.proofHolds(c, proof, root));
        assertFalse(ReceiptActivity.proofHolds(a, proof, root));
        assertFalse(ReceiptActivity.proofHolds(c, proof, d("other")));
    }
}
