package com.harvana.app.group;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AdminSeatTest {
    @Test public void handoffFollowsTheNewCreatorNotTheOldWallet() {
        assertFalse(AdminSeat.current("old", "HV0xOLD", "new", "HV0xOLD", "MEMBER"));
        assertTrue(AdminSeat.current("new", "HV0xNEW", "new", "HV0xOLD", "MEMBER"));
    }

    @Test public void nodeAddressOnlyWhenFirestoreHasNotNamedAnAdmin() {
        assertTrue(AdminSeat.current("old", "HV0xOLD", "", "HV0xOLD", "MEMBER"));
        assertFalse(AdminSeat.current("other", "HV0xOTHER", "", "HV0xOLD", "ADMIN"));
    }
}
