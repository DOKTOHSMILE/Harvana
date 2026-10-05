package com.harvana.app.firestore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class PrivateProfileTest {

    private static Map<String, Object> signupProfile() {
        Map<String, Object> m = new HashMap<>();
        m.put("uid", "u1");
        m.put("pid", "1234567890");
        m.put("first_name", "Ada");
        m.put("last_name", "Obi");
        m.put("icon", "https://x/y.png");
        m.put("tier", "2");
        m.put("city", "Lagos");
        m.put("dob", "1990-01-01");
        m.put("v_type", "nin");
        m.put("nin", "12345678901");
        m.put("bvn", "10987654321");
        m.put("phone", "+2348000000000");
        m.put("email", "a@b.c");
        m.put("address", "1 Road");
        m.put("occupation", "Trader");
        m.put("gps_lat", "6.5");
        m.put("gps_long", "3.4");
        m.put("kin", new HashMap<String, Object>());
        return m;
    }

    @Test
    public void splitKeepsEveryPersonalFieldOutOfThePublicDocument() {
        Map<String, Object>[] parts = PrivateProfile.split(signupProfile());
        for (String f : PrivateProfile.FIELDS) {
            assertFalse(f + " leaked into the public document", parts[0].containsKey(f));
            assertTrue(f + " missing from the private document", parts[1].containsKey(f));
        }
    }

    @Test
    public void splitLeavesPublicAndUnknownFieldsWhereTheyWere() {
        Map<String, Object> p = signupProfile();
        p.put("someNewField", "x");
        Map<String, Object>[] parts = PrivateProfile.split(p);
        for (String f : new String[] {"uid", "pid", "first_name", "last_name", "icon", "tier", "someNewField"}) {
            assertTrue(f, parts[0].containsKey(f));
            assertFalse(f, parts[1].containsKey(f));
        }
    }

    @Test
    public void splitThenMergeRestoresTheOriginal() {
        Map<String, Object> p = signupProfile();
        Map<String, Object>[] parts = PrivateProfile.split(p);
        assertEquals(p, PrivateProfile.merge(parts[0], parts[1]));
    }

    @Test
    public void mergeLetsTheOwnerOnlyCopyWin() {
        Map<String, Object> pub = new HashMap<>();
        pub.put("phone", "old");
        Map<String, Object> priv = new HashMap<>();
        priv.put("phone", "new");
        assertEquals("new", PrivateProfile.merge(pub, priv).get("phone"));
    }

    @Test
    public void leftoversFindsOnlyOldStylePublicDocuments() {
        assertTrue(PrivateProfile.leftoversIn(PrivateProfile.split(signupProfile())[0]).isEmpty());
        assertEquals(PrivateProfile.FIELDS, PrivateProfile.leftoversIn(signupProfile()).stream()
                .filter(PrivateProfile.FIELDS::contains).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    public void nullsAreTolerated() {
        assertTrue(PrivateProfile.split(null)[0].isEmpty());
        assertTrue(PrivateProfile.merge(null, null).isEmpty());
        assertTrue(PrivateProfile.leftoversIn(null).isEmpty());
        assertFalse(PrivateProfile.isPrivate(null));
    }
}
