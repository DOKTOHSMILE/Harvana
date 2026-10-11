package com.harvana.app.banks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class AccountRulesTest {
    @Test public void ibanUsesTheMod97CheckAndTheCountryLength() {
        assertTrue(AccountRules.isIban("DE89 3704 0044 0532 0130 00"));       // ECBS example
        assertTrue(AccountRules.isIban("GB82WEST12345698765432"));
        assertTrue(AccountRules.isIban("FR1420041010050500013M02606"));
        assertTrue(AccountRules.isIban("AE070331234567890123456"));
        assertFalse(AccountRules.isIban("DE89 3704 0044 0532 0130 01"));      // one digit off
        assertFalse(AccountRules.isIban("DE89 3704 0044 0532 0130"));         // too short for DE
        assertEquals("DE89 3704 0044 0532 0130 00", AccountRules.formatIban("de89370400440532013000"));
    }

    @Test public void bicRoutingIfscClabe() {
        assertTrue(AccountRules.isBic("DEUTDEFF"));
        assertTrue(AccountRules.isBic("cobadeffxxx"));
        assertFalse(AccountRules.isBic("DEUT1EFF"));
        assertTrue(AccountRules.isAbaRouting("021000021"));                  // JPMorgan Chase
        assertTrue(AccountRules.isAbaRouting("011000015"));                  // Federal Reserve Boston
        assertFalse(AccountRules.isAbaRouting("021000022"));
        assertTrue(AccountRules.isIfsc("HDFC0000001"));
        assertFalse(AccountRules.isIfsc("HDFC1000001"));
        assertTrue(AccountRules.isClabe("002010077777777771"));               // Banxico example
        assertFalse(AccountRules.isClabe("002010077777777772"));
        assertEquals("Citibanamex", AccountRules.clabeBank("002010077777777771"));
    }

    @Test public void checkSaysWhatToFixAndComposeFoldsTheDetails() {
        Map<String, String> x = new HashMap<>();
        assertEquals("Enter the routing number (ABA).", AccountRules.check("ABA", "bank", "US", "123456789", x));
        x.put("routing", "021000021");
        assertNull(AccountRules.check("ABA", "bank", "US", "123456789", x));
        assertEquals("Routing 021000021 · Acct 123456789", AccountRules.compose("ABA", "bank", "123456789", x));
        assertNull(AccountRules.check("IBAN", "bank", "DE", "DE89370400440532013000", new HashMap<>()));
        assertTrue(AccountRules.check("IBAN", "bank", "FR", "DE89370400440532013000", new HashMap<>()).contains("another country"));
        Map<String, String> uk = new HashMap<>();
        uk.put("sortCode", "200000");
        assertNull(AccountRules.check("SORT_CODE", "bank", "GB", "12345678", uk));
        assertEquals("Sort code 20-00-00 · Acct 12345678", AccountRules.compose("SORT_CODE", "bank", "12345678", uk));
        assertNull(AccountRules.check("ABA", "fintech", "US", "me@example.com", null));      // PayPal and co: no bank fields
        assertEquals("Nigerian account numbers have 10 digits.", AccountRules.check("NUBAN", "bank", "NG", "12345", null));
    }
}
