package com.harvana.app.chain;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class LockTemplatesTest {

    private static final String ME = "HV0x" + "a".repeat(40);
    private static final String ESCROW = "HV0x" + "b".repeat(40);

    private static JSONObject lock() throws Exception {
        return new JSONObject().put("version", 2).put("chainId", "HARVANA-HARV-1").put("type", "FIXED_DEPOSIT")
                .put("from", ME).put("to", ESCROW).put("assetId", "HARV").put("amount", 100_000_000L)
                .put("maxFee", 5).put("gasLimit", 0).put("memo", "FIXED_DEPOSIT:FIXED-1").put("payload", "");
    }

    @Test public void aGoodLockPasses() throws Exception { assertNull(LockTemplates.check(lock(), ME)); }

    @Test public void anotherWalletsLockIsRefused() throws Exception {
        assertNotNull(LockTemplates.check(lock(), "HV0x" + "c".repeat(40)));
    }

    @Test public void typeMustMatchTheMemo() throws Exception {
        assertNotNull(LockTemplates.check(lock().put("type", "P2P_LOCK"), ME));
    }

    @Test public void aPlainTransferMemoIsNotALock() throws Exception {
        assertNotNull(LockTemplates.check(lock().put("memo", "hello"), ME));
    }

    @Test public void otherChainAssetPayloadAndGasAreRefused() throws Exception {
        assertNotNull(LockTemplates.check(lock().put("chainId", "OTHER"), ME));
        assertNotNull(LockTemplates.check(lock().put("assetId", "HUSD"), ME));
        assertNotNull(LockTemplates.check(lock().put("payload", "x"), ME));
        assertNotNull(LockTemplates.check(lock().put("gasLimit", 1), ME));
        assertNotNull(LockTemplates.check(lock().put("amount", 0), ME));
        assertNotNull(LockTemplates.check(lock().put("to", ME), ME));
    }

    @Test public void expiredTemplateIsRefused() throws Exception {
        assertNotNull(LockTemplates.check(lock().put("templateExpiresTimestamp", 1L), ME));
    }

    /** Same bytes as the node's consensus/CanonicalCodec (checked against the node on 19 Oct 2026). */
    @Test public void signingBytesLayout() {
        byte[] b = LockV2.signingBytes(2, "HARVANA-HARV-1", "FIXED_DEPOSIT", ME, ESCROW, "HARV", 1, 2, 0, 3, 4, "m", "");
        // str("HARVANA_TX_V2") = 4-byte length 13 + 13 bytes
        assertEquals(13, b[3]);
        assertEquals('H', b[4]);
        // total: 17 + 4 + (4+14) + (4+13) + (4+44) + (4+44) + (4+4) + 5*8 + (4+1) + 4
        assertEquals(17 + 4 + 18 + 17 + 48 + 48 + 8 + 40 + 5 + 4, b.length);
    }
}
