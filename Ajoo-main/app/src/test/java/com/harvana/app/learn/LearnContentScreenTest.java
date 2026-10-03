package com.harvana.app.learn;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LearnContentScreenTest {
    private static boolean blocked(String t, String d) { return LearnContentScreen.check(t, d, "https://x.com/a.png").blocked(); }

    @Test public void normalArticlePasses() {
        assertFalse(blocked("How rotating savings circles work",
            "A circle is a group that saves together. Each round one member receives the pot. Read the rules carefully before you join."));
    }

    @Test public void guaranteedReturnsBlocked() {
        assertTrue(blocked("Easy money", "We guarantee daily profit on every deposit."));
        assertTrue(blocked("Double your money in 24 hours", "Join today"));
        assertTrue(blocked("Safe", "100% profit with no risk"));
    }

    @Test public void secretRequestsBlocked() {
        assertTrue(blocked("Verify your wallet", "Please share your seed phrase so we can verify it."));
        assertTrue(blocked("Support", "Send me your OTP to continue"));
    }

    @Test public void sendMoneyPitchBlocked() {
        assertTrue(blocked("Invest", "Transfer your coins to my wallet and I will trade for you"));
        assertTrue(blocked("Invest", "DM me to invest"));
    }

    @Test public void impersonationBlocked() {
        assertTrue(blocked("Notice", "I am from Harvana support and will fix your account"));
    }

    @Test public void shortLinksAndChatInvitesBlocked() {
        assertTrue(blocked("Read", "See https://bit.ly/abc123 for details"));
        assertTrue(blocked("Join", "Our group: https://t.me/somegroup"));
        assertFalse(blocked("Read", "Full guide at https://harvannaprotocol.com/learn"));
    }

    @Test public void insecureImageBlocked() {
        assertTrue(LearnContentScreen.check("Title", "A fine description of savings.", "http://x.com/a.png").blocked());
    }

    @Test public void shoutingBlocked() {
        assertTrue(blocked("HOW TO SAVE", "THIS IS THE BEST WAY TO SAVE YOUR MONEY EVERY SINGLE MONTH FOR EVERYONE"));
    }
}
