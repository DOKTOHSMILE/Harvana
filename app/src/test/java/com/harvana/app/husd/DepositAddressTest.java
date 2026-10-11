package com.harvana.app.husd;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DepositAddressTest {
    /** Golden vector from DepositForwarderFactory.addressFor on a local chain (node repo, bridge/contracts). */
    @Test public void matchesTheContractsCreate2Address() {
        String factory = "0x06BfbF84b684CAd5DB68aC6eFB9083FFCa0C1334";
        String initCodeHash = "0xc006fe19814548c3b6a2fd55553d3ddc4fcc35a0b555fc5ceb1dede6494e52a4";
        assertEquals("0xF865d0273B6Fac5eFEBB612B22D04D4BCc9E773c",
                DepositAddress.of(factory, initCodeHash, "HV0x" + "cd".repeat(20)));
    }

    @Test public void eip55ChecksumMatchesTheStandardExample() {
        // From EIP-55's own test list.
        assertEquals("0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed",
                DepositAddress.checksum(DepositAddress.hex("0x5aaeb6053f3e94c9b9a09f33669435e7ef1beaed")));
    }
}
