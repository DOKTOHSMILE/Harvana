package com.harvana.app.tx;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Wallets the person saved with a name, on this phone. Each entry keeps the address shape it belongs to ("HV0x...",
 * "0x...", "T...", …), so the Send screen offers only addresses the picked coin can go to.
 */
public final class AddressBook {
    private AddressBook() {}

    private static final String PREFS = "address_book";
    private static final String KEY = "contacts";
    public static final int MAX = 200;

    public static final class Contact {
        public final String name, address, family;
        public final long savedAt;

        Contact(String name, String address, String family, long savedAt) {
            this.name = name; this.address = address; this.family = family; this.savedAt = savedAt;
        }
    }

    public static synchronized List<Contact> all(Context c) {
        List<Contact> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString(KEY, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o != null) out.add(new Contact(o.optString("name"), o.optString("address"), o.optString("family"), o.optLong("at")));
            }
        } catch (Exception ignored) { }
        return out;
    }

    /** Saved wallets for one address shape, by name. */
    public static List<Contact> forFamily(Context c, String family) {
        List<Contact> out = new ArrayList<>();
        for (Contact x : all(c)) if (x.family.equals(family)) out.add(x);
        java.util.Collections.sort(out, (p, q) -> p.name.compareToIgnoreCase(q.name));
        return out;
    }

    /** The saved name of an address, or null. */
    public static String nameOf(Context c, String address) {
        if (address == null) return null;
        for (Contact x : all(c)) if (x.address.equalsIgnoreCase(address.trim())) return x.name;
        return null;
    }

    /** Saves (or renames) an address. */
    public static synchronized void save(Context c, String name, String address, String family) {
        String a = address == null ? "" : address.trim();
        String n = name == null ? "" : name.trim();
        if (a.isEmpty() || n.isEmpty()) return;
        List<Contact> list = all(c);
        JSONArray out = new JSONArray();
        try {
            out.put(new JSONObject().put("name", n).put("address", a).put("family", family).put("at", System.currentTimeMillis()));
            for (Contact x : list) {
                if (x.address.equalsIgnoreCase(a) || out.length() >= MAX) continue;
                out.put(new JSONObject().put("name", x.name).put("address", x.address).put("family", x.family).put("at", x.savedAt));
            }
        } catch (Exception ignored) { }
        prefs(c).edit().putString(KEY, out.toString()).apply();
    }

    public static synchronized void remove(Context c, String address) {
        JSONArray out = new JSONArray();
        try {
            for (Contact x : all(c)) {
                if (x.address.equalsIgnoreCase(address)) continue;
                out.put(new JSONObject().put("name", x.name).put("address", x.address).put("family", x.family).put("at", x.savedAt));
            }
        } catch (Exception ignored) { }
        prefs(c).edit().putString(KEY, out.toString()).apply();
    }

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
}
