package com.harvana.app.group;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class GroupKindsTest {
    private Map<String, Object> g(String type, String circle) {
        Map<String, Object> m = new HashMap<>();
        m.put("groupType", type);
        if (circle != null) m.put("circleId", circle);
        return m;
    }

    @Test public void legacyRotatingIsRotatingWithoutCircle() {
        assertTrue(GroupKinds.isLegacyRotating(g("Rotating Pool", null)));
        assertFalse(GroupKinds.isLegacyRotating(g("Rotating Pool", "CIRC-1")));
        assertFalse(GroupKinds.isLegacyRotating(g("Fixed Contribution", null)));
        assertFalse(GroupKinds.isLegacyRotating(null));
    }

    @Test public void onChain() {
        assertTrue(GroupKinds.isOnChain(g("Rotating Pool", "CIRC-1")));
        assertFalse(GroupKinds.isOnChain(g("Rotating Pool", " ")));
    }
}
