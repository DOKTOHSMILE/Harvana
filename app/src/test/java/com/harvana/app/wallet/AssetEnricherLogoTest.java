package com.harvana.app.wallet;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/** Which logo addresses the app can draw, and IPFS pictures tried through more than one gateway. */
public class AssetEnricherLogoTest {

    @Test
    public void picturesTheAppCanDraw() {
        assertTrue(AssetEnricher.drawable("https://cdn.example.com/logo.png"));
        assertTrue(AssetEnricher.drawable("data:image/png;base64,iVBORw0KGgo="));
        assertFalse(AssetEnricher.drawable("data:image/svg+xml;base64,PHN2Zz4="));
        assertFalse(AssetEnricher.drawable("https://example.com/logo.svg"));
        assertFalse(AssetEnricher.drawable("https://example.com/logo.svg?v=2"));
        assertFalse(AssetEnricher.drawable(""));
        assertFalse(AssetEnricher.drawable("ipfs://bafy"));
    }

    @Test
    public void ipfsLinksGoThroughSeveralGateways() {
        assertEquals("https://ipfs.io/ipfs/bafy", AssetEnricher.https("ipfs://bafy"));
        List<String> g = AssetEnricher.gateways("https://ipfs.io/ipfs/bafy");
        assertEquals(3, g.size());
        assertEquals("https://gateway.pinata.cloud/ipfs/bafy", g.get(0));
        assertEquals(List.of("https://cdn.example.com/a.png"), AssetEnricher.gateways("https://cdn.example.com/a.png"));
    }
}
