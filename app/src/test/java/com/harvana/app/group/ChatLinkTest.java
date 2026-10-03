package com.harvana.app.group;

import static org.junit.Assert.*;

import org.junit.Test;

public class ChatLinkTest {
    @Test public void acceptsKnownChatLinks() {
        assertEquals("WhatsApp", ChatLink.platformOf(ChatLink.normalize("https://chat.whatsapp.com/AbC123")));
        assertEquals("Telegram", ChatLink.platformOf(ChatLink.normalize("t.me/joinchat/xyz")));
        assertEquals("Discord", ChatLink.platformOf(ChatLink.normalize("https://discord.gg/abc")));
        assertEquals("Facebook", ChatLink.platformOf(ChatLink.normalize("https://www.facebook.com/groups/12345")));
    }

    @Test public void rejectsEverythingElse() {
        assertNull(ChatLink.normalize("http://chat.whatsapp.com/abc"));
        assertNull(ChatLink.normalize("https://evil.example/chat.whatsapp.com/abc"));
        assertNull(ChatLink.normalize("https://chat.whatsapp.com.evil.com/abc"));
        assertNull(ChatLink.normalize("javascript:alert(1)"));
        assertNull(ChatLink.normalize("https://user:pw@t.me/abc"));
        assertNull(ChatLink.normalize("https://t.me/"));
        assertNull(ChatLink.normalize("hello world"));
        assertNull(ChatLink.normalize(""));
    }
}
