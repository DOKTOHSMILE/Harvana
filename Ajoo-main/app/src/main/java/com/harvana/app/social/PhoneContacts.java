package com.harvana.app.social;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads the phone book (needs READ_CONTACTS). Only ever used to compute scrambled keys and to label matches; nothing is uploaded. */
public final class PhoneContacts {
    private PhoneContacts() {}

    public static final class Entry {
        public final String name;
        public final List<String> phones = new ArrayList<>();
        public final List<String> emails = new ArrayList<>();
        Entry(String name) { this.name = name; }
    }

    private static final int MAX = 600;

    /** Up to 600 contacts with their numbers and emails. Call off the main thread. */
    public static List<Entry> read(Context c) {
        ContentResolver cr = c.getContentResolver();
        Map<String, Entry> byId = new LinkedHashMap<>();
        try (Cursor p = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            new String[]{ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER},
            null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")) {
            while (p != null && p.moveToNext() && byId.size() < MAX) {
                String id = p.getString(0);
                Entry e = byId.get(id);
                if (e == null) { e = new Entry(p.getString(1) == null ? "" : p.getString(1)); byId.put(id, e); }
                String n = p.getString(2);
                if (n != null && !e.phones.contains(n)) e.phones.add(n);
            }
        } catch (Exception ignored) { }
        try (Cursor m = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            new String[]{ContactsContract.CommonDataKinds.Email.CONTACT_ID, ContactsContract.CommonDataKinds.Email.DISPLAY_NAME, ContactsContract.CommonDataKinds.Email.ADDRESS},
            null, null, null)) {
            while (m != null && m.moveToNext()) {
                String id = m.getString(0);
                Entry e = byId.get(id);
                if (e == null) { if (byId.size() >= MAX) continue; e = new Entry(m.getString(1) == null ? "" : m.getString(1)); byId.put(id, e); }
                String a = m.getString(2);
                if (a != null && !e.emails.contains(a)) e.emails.add(a);
            }
        } catch (Exception ignored) { }
        return new ArrayList<>(byId.values());
    }
}
