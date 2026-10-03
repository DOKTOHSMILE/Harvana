package com.harvanna.app.group;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;

public class GroupIdentityResolverTest {
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(String.valueOf(kv[i]), kv[i + 1]);
        return m;
    }

    @Test public void firestoreUuidIsNeverTheProtocolId() {
        String uuid = UUID.randomUUID().toString();
        GroupIdentity id = GroupIdentityResolver.resolve(uuid, map("nodeGroupId", "CIRC-abc", "groupId", uuid));
        assertEquals(GroupIdentity.Kind.ON_CHAIN_CIRCLE, id.kind);
        assertEquals("CIRC-abc", id.protocolId());
        assertNotEquals(uuid, id.protocolId());
        assertTrue(GroupKinds.familyOf(uuid).isEmpty());
    }

    @Test public void prefixes() {
        assertEquals("CIRCLE", GroupKinds.familyOf("CIRC-1"));
        assertEquals("ACCUM", GroupKinds.familyOf("ACCUM-1"));
        assertEquals("FIXED", GroupKinds.familyOf("FIXED-1"));
        assertEquals("", GroupKinds.familyOf("550e8400-e29b-41d4-a716-446655440000"));
    }

    @Test public void legacyGroupIdFieldStillMaps() {
        GroupIdentity id = GroupIdentityResolver.resolve("doc", map("groupId", "FIXED-9"));
        assertEquals(GroupIdentity.Kind.ON_CHAIN_FIXED, id.kind);
        assertEquals("FIXED-9", id.protocolId());
    }

    @Test public void missingMappingIsLegacy() {
        GroupIdentity id = GroupIdentityResolver.resolve("doc", map("groupType", "Rotating Pool"));
        assertEquals(GroupIdentity.Kind.LEGACY_OFFCHAIN, id.kind);
        assertEquals("", id.protocolId());
    }

    @Test public void badExplicitIdIsBrokenNotGuessed() {
        GroupIdentity id = GroupIdentityResolver.resolve("doc", map("nodeGroupId", "not-a-node", "groupId", "CIRC-should-not-win"));
        assertEquals(GroupIdentity.Kind.BROKEN_MAPPING, id.kind);
        assertEquals("", id.protocolId());
    }

    @Test public void payoutCapabilityFollowsFamily() {
        GroupIdentity circle = GroupIdentityResolver.resolve("doc", map("nodeGroupId", "CIRC-1"));
        GroupIdentity fixed = GroupIdentityResolver.resolve("doc", map("nodeGroupId", "FIXED-1"));
        GroupIdentity accum = GroupIdentityResolver.resolve("doc", map("nodeGroupId", "ACCUM-1"));
        assertTrue(GroupCapability.payoutFollowsLockedOrder(circle));
        assertFalse(GroupCapability.canRequestDisbursement(circle));
        assertTrue(GroupCapability.canRequestDisbursement(fixed));
        assertTrue(GroupCapability.canClaimLine(accum));
        assertFalse(GroupCapability.canRequestDisbursement(accum));
    }

    @Test public void nodeMemberStateWinsOverAStaleFirestoreRole() {
        GroupIdentity id = GroupIdentityResolver.resolve("doc", map("nodeGroupId", "ACCUM-1", "adminUid", "firebase-user"));
        assertTrue(id.onChain());
        assertEquals("ACCUM-1", id.protocolId());
        assertFalse(id.protocolId().equals("firebase-user"));
    }
}
