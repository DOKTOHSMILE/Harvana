package com.harvana.app.chain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IdentityCanonicalTest {
    @Test public void formatsMatchNodeSource() {
        assertEquals("HARVANA_IDENTITY_LINK:HV0xabc:1700000000000", IdentityCanonical.identityLink("HV0xabc", 1700000000000L));
        assertEquals("PERSONHOOD_BIND:HV0xabc:deadbeef:5", IdentityCanonical.personhoodBind("HV0xabc", "deadbeef", 5L));
        assertEquals("PERSONHOOD_VOUCH:HV0xa:HV0xb:9", IdentityCanonical.personhoodVouch("HV0xa", "HV0xb", 9L));
    }
}
