package com.harvana.app.banks;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Every Nigerian bank, microfinance bank and payment app, with its official name, short name, transfer code, USSD
 * and logo. The list ships with the app (assets/banks/ng_banks.json, logos for the most used ones under
 * assets/banks/logos) so it works offline. Once a week it is refreshed from the free public directory at
 * nigerianbanks.xyz, which adds newly licensed banks and their logos; the bundled names, short names and aliases
 * stay on top of it.
 *
 * Use {@link #match(Context, String)} to turn whatever a user typed ("gtb", "Opay", "first bank") into the
 * official bank, and {@link #search(Context, String)} for a picker.
 */
public final class BankDirectory {
    private BankDirectory() {}

    public static final String REMOTE = "https://nigerianbanks.xyz";
    private static final String PREFS = "bank_directory";
    private static final long REFRESH_MS = 7L * 24 * 60 * 60 * 1000;

    public static final class Bank {
        public String name = "", shortName = "", slug = "", code = "", ussd = "", kind = "bank", country = "NG";
        public String logoAsset = "", logoUrl = "", color = "";
        public int accountLength = 10;
        public List<String> aliases = new ArrayList<>();

        /** The name to show in lists: the short name people use ("GTBank"), else the official one. */
        public String display() { return shortName.isEmpty() ? name : shortName; }

        /** "Commercial bank", "Microfinance bank", "Payment app"… */
        public String kindLabel() {
            switch (kind) {
                case "microfinance": return "Microfinance bank";
                case "mortgage": return "Mortgage bank";
                case "psb": return "Payment service bank";
                case "fintech": return "Payment app";
                default: return "Bank";
            }
        }

        public String initials() {
            String[] w = display().replaceAll("[^A-Za-z0-9 ]", " ").trim().split("\\s+");
            if (w.length == 0 || w[0].isEmpty()) return "?";
            if (w.length == 1) return w[0].substring(0, Math.min(2, w[0].length())).toUpperCase(Locale.US);
            return (w[0].substring(0, 1) + w[1].substring(0, 1)).toUpperCase(Locale.US);
        }
    }

    private static List<Bank> cache;

    /** All banks, the bundled list merged with the last refresh. Safe on the main thread after the first call (small JSON). */
    public static synchronized List<Bank> all(Context c) {
        if (cache != null) return cache;
        List<Bank> out = new ArrayList<>();
        try (InputStream in = c.getAssets().open("banks/ng_banks.json")) {
            byte[] buf = new byte[in.available()];
            int off = 0, n;
            while (off < buf.length && (n = in.read(buf, off, buf.length - off)) > 0) off += n;
            JSONArray arr = new JSONObject(new String(buf, 0, off, "UTF-8")).optJSONArray("banks");
            if (arr != null) for (int i = 0; i < arr.length(); i++) out.add(parse(arr.optJSONObject(i)));
        } catch (Exception ignored) { }
        String remote = prefs(c).getString("remote", "");
        if (!remote.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(remote);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null || find(out, o.optString("slug"), o.optString("code")) != null) continue;
                    Bank b = new Bank();
                    b.name = o.optString("name");
                    b.slug = o.optString("slug");
                    b.code = o.optString("code");
                    b.ussd = o.optString("ussd");
                    b.logoUrl = o.optString("logo");
                    String l = b.name.toLowerCase(Locale.US);
                    b.kind = l.contains("mortgage") ? "mortgage" : (l.contains("mfb") || l.contains("microfinance")) ? "microfinance" : l.contains("psb") ? "psb" : "bank";
                    if (!b.name.isEmpty()) out.add(b);
                }
            } catch (Exception ignored) { }
        }
        Collections.sort(out, (a, b) -> a.display().compareToIgnoreCase(b.display()));
        cache = out;
        return out;
    }

    /** The bank a typed name means, or null. Exact names, short names and aliases win; then a unique "starts with". */
    public static Bank match(Context c, String typed) {
        if (typed == null) return null;
        String q = norm(typed);
        if (q.isEmpty()) return null;
        List<Bank> banks = all(c);
        for (Bank b : banks) if (norm(b.name).equals(q) || norm(b.shortName).equals(q) || b.code.equalsIgnoreCase(typed.trim())) return b;
        for (Bank b : banks) for (String a : b.aliases) if (norm(a).equals(q)) return b;
        Bank only = null;
        for (Bank b : banks) {
            if (norm(b.name).startsWith(q) || (!b.shortName.isEmpty() && norm(b.shortName).startsWith(q))) {
                if (only != null) return popular(only, b);
                only = b;
            }
        }
        return only;
    }

    /** For a picker: popular banks first when the box is empty, otherwise every bank whose name, short name or alias contains the text. */
    public static List<Bank> search(Context c, String typed) {
        String q = norm(typed == null ? "" : typed);
        List<Bank> out = new ArrayList<>();
        List<Bank> rest = new ArrayList<>();
        for (Bank b : all(c)) {
            boolean hit = q.isEmpty() || norm(b.name).contains(q) || norm(b.shortName).contains(q) || b.code.startsWith(q);
            if (!hit) for (String a : b.aliases) if (norm(a).contains(q)) { hit = true; break; }
            if (!hit) continue;
            if (!b.logoAsset.isEmpty()) out.add(b); else rest.add(b);   // the well-known ones (bundled logos) first
        }
        if (q.isEmpty()) {
            // An empty box opens on the banks most people use, in that order.
            List<Bank> top = new ArrayList<>();
            for (String slug : POPULAR) for (Bank b : out) if (slug.equals(b.slug)) { top.add(b); break; }
            out.removeAll(top);
            top.addAll(out);
            out = top;
        }
        out.addAll(rest);
        return out;
    }

    private static final String[] POPULAR = {"paycom", "moniepoint-mfb-ng", "guaranty-trust-bank", "access-bank", "zenith-bank",
            "first-bank-of-nigeria", "united-bank-for-africa", "kuda-bank", "palmpay", "first-city-monument-bank", "fidelity-bank",
            "wema-bank", "sterling-bank", "union-bank-of-nigeria", "stanbic-ibtc-bank", "ecobank-nigeria", "polaris-bank"};

    /**
     * NUBAN check digit (CBN standard) for banks with a 3-digit code. Returns true when it passes or cannot be checked
     * (payment apps, microfinance banks and phone-number accounts use other schemes), so it only ever warns.
     */
    public static boolean nubanLooksValid(Bank bank, String account) {
        if (bank == null || account == null) return true;
        String a = account.replaceAll("\\D", "");
        if (a.length() != 10 || bank.code == null || !bank.code.matches("\\d{3}")) return true;
        String s = bank.code + a.substring(0, 9);
        int[] w = {3, 7, 3, 3, 7, 3, 3, 7, 3, 3, 7, 3};
        int sum = 0;
        for (int i = 0; i < 12; i++) sum += (s.charAt(i) - '0') * w[i];
        int check = (10 - sum % 10) % 10;
        return check == a.charAt(9) - '0';
    }

    /** Refreshes the list from the public directory at most once a week. Run off the main thread. */
    public static void refreshIfStale(Context c) {
        SharedPreferences p = prefs(c);
        if (System.currentTimeMillis() - p.getLong("at", 0) < REFRESH_MS) return;
        try {
            OkHttpClient http = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build();
            try (Response r = http.newCall(new Request.Builder().url(REMOTE).build()).execute()) {
                if (!r.isSuccessful() || r.body() == null) return;
                String body = r.body().string();
                JSONArray arr = new JSONArray(body);                // must be the list, or nothing is saved
                if (arr.length() < 50) return;
                p.edit().putString("remote", body).putLong("at", System.currentTimeMillis()).apply();
                synchronized (BankDirectory.class) { cache = null; }
            }
        } catch (Exception ignored) { }
    }

    private static Bank parse(JSONObject o) {
        Bank b = new Bank();
        if (o == null) return b;
        b.name = o.optString("name");
        b.shortName = o.optString("short");
        b.slug = o.optString("slug");
        b.code = o.optString("code");
        b.ussd = o.optString("ussd");
        b.kind = o.optString("kind", "bank");
        b.country = o.optString("country", "NG");
        b.accountLength = o.optInt("accountLength", 10);
        b.logoAsset = o.optString("logoAsset");
        b.logoUrl = o.optString("logoUrl");
        b.color = o.optString("color");
        JSONArray al = o.optJSONArray("aliases");
        if (al != null) for (int i = 0; i < al.length(); i++) b.aliases.add(al.optString(i));
        return b;
    }

    private static Bank find(List<Bank> list, String slug, String code) {
        for (Bank b : list) if ((!slug.isEmpty() && slug.equals(b.slug)) || (!code.isEmpty() && code.equals(b.code))) return b;
        return null;
    }

    /** Between two "starts with" hits, the one people use more (it has a bundled logo); null when that doesn't settle it. */
    private static Bank popular(Bank a, Bank b) {
        boolean pa = !a.logoAsset.isEmpty(), pb = !b.logoAsset.isEmpty();
        return pa == pb ? null : (pa ? a : b);
    }

    static String norm(String s) {
        return s == null ? "" : s.toLowerCase(Locale.US).replace("plc", "").replace("limited", "").replace("ltd", "")
                .replace("nigeria", "").replaceAll("[^a-z0-9]", "");
    }

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
}
