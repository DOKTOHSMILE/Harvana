package com.harvana.app.wallet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** The access card's QR carries its tag after '#'; restoring names the wallet with it. */
public class CardTagTest {

    @Test
    public void theTagIsReadBackAndOldCardsHaveNone() throws Exception {
        String qr = HarvanaCardCrypto.FORMAT_PREFIX + "AbC+/d==" + "#" + java.net.URLEncoder.encode("My main wallet", "UTF-8");
        assertEquals("My main wallet", HarvanaCardCrypto.tagOf(qr));
        assertEquals("", HarvanaCardCrypto.tagOf(HarvanaCardCrypto.FORMAT_PREFIX + "AbC+/d=="));
        assertEquals("", HarvanaCardCrypto.tagOf(null));
    }

    @Test
    public void theCardNumberIsTwelveDigitsFromTheAddress() {
        String n = HarvanaCardRenderer.cardNumber("HV0x7076d8590d8202e0ca0d34b8f2e4e18fd6e57516");
        assertEquals(14, n.length());
        assertEquals(n, HarvanaCardRenderer.cardNumber("HV0X7076D8590D8202E0CA0D34B8F2E4E18FD6E57516"));
    }
}
