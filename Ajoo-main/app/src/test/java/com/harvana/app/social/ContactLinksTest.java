package com.harvana.app.social;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ContactLinksTest {
    @Test public void phoneAndWhatsApp() {
        assertEquals("tel:+2348012345678", ContactLinks.uri("Phone", "+234 801 234 5678"));
        assertEquals("https://wa.me/2348012345678", ContactLinks.uri("WhatsApp", "+234 (801) 234-5678"));
        assertNotNull(ContactLinks.problem("Phone", "123"));
        assertNotNull(ContactLinks.problem("Phone", "call me maybe"));
    }
    @Test public void email() {
        assertEquals("mailto:ada@example.com", ContactLinks.uri("Email", "ada@example.com"));
        assertNotNull(ContactLinks.problem("Email", "not an email"));
    }
    @Test public void handles() {
        assertEquals("https://t.me/ada_o", ContactLinks.uri("Telegram", "@ada_o"));
        assertEquals("https://instagram.com/ada.o", ContactLinks.uri("Instagram", "ada.o"));
        assertEquals("https://tiktok.com/@ada", ContactLinks.uri("TikTok", "@ada"));
        assertNotNull(ContactLinks.problem("X", "bad handle!"));
    }
    @Test public void websitesMustBeHttps() {
        assertEquals("https://example.com/me", ContactLinks.uri("Website", "example.com/me"));
        assertEquals("https://example.com", ContactLinks.uri("Website", "https://example.com"));
        assertNull(ContactLinks.uri("Website", "http://example.com"));
        assertNull(ContactLinks.uri("Website", "javascript:alert(1)"));
        assertNull(ContactLinks.uri("Unknown", "x"));
    }
}
