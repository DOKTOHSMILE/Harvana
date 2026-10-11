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
 * Banks for every user. Nigeria: every bank, microfinance bank and payment app (ng_banks.json). Elsewhere: the main
 * banks and payment apps of 43 more countries with their logos (world_banks.json, AccountRules for how accounts are
 * written there). The Nigerian list is: every Nigerian bank, microfinance bank and payment app, with its official name, short name, transfer code, USSD
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
        public String logoAsset = "", logoUrl = "", color = "", domain = "";
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
                case "mobile_money": return "Mobile money";
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

    /** A country: how accounts are written there (AccountRules scheme) and its main banks and payment apps. */
    public static final class Country {
        public String iso = "", name = "", currency = "", scheme = "ACCOUNT_SWIFT";
        public final List<Bank> banks = new ArrayList<>();

        /** The flag emoji for the ISO code; a globe for "Other country". */
        public String flag() {
            if (iso.length() != 2) return "\uD83C\uDF0D";
            int a = Character.codePointAt(iso.toUpperCase(Locale.US), 0) - 'A' + 0x1F1E6;
            int b = Character.codePointAt(iso.toUpperCase(Locale.US), 1) - 'A' + 0x1F1E6;
            return new String(Character.toChars(a)) + new String(Character.toChars(b));
        }
    }

    private static List<Country> countries;

    /** Every country the app knows, Nigeria and Africa first, then the rest; "Other country" last. */
    public static synchronized List<Country> countries(Context c) {
        if (countries != null) return countries;
        List<Country> out = new ArrayList<>();
        try (InputStream in = c.getAssets().open("banks/world_banks.json")) {
            JSONArray arr = new JSONObject(readAll(in)).optJSONArray("countries");
            if (arr != null) for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                Country k = new Country();
                k.iso = o.optString("iso");
                k.name = o.optString("name");
                k.currency = o.optString("currency");
                k.scheme = o.optString("scheme", "ACCOUNT_SWIFT");
                JSONArray bs = o.optJSONArray("banks");
                if (bs != null) for (int j = 0; j < bs.length(); j++) {
                    Bank b = parse(bs.optJSONObject(j));
                    b.country = k.iso;
                    k.banks.add(b);
                }
                out.add(k);
            }
        } catch (Exception ignored) { }
        countries = out;
        return out;
    }

    public static Country country(Context c, String iso) {
        for (Country k : countries(c)) if (k.iso.equalsIgnoreCase(iso == null ? "" : iso)) return k;
        for (Country k : countries(c)) if ("OTHER".equals(k.iso)) return k;
        Country k = new Country();
        k.iso = "OTHER"; k.name = "Other country";
        return k;
    }

    /** The country to open on: the last one picked, else the phone's region, else the P2P currency's, else Nigeria. */
    public static String defaultCountry(Context c) {
        String saved = prefs(c).getString("country", "");
        if (!saved.isEmpty()) return saved;
        String region = Locale.getDefault().getCountry();
        for (Country k : countries(c)) if (k.iso.equalsIgnoreCase(region)) return k.iso;
        return "NG";
    }

    public static void rememberCountry(Context c, String iso) { prefs(c).edit().putString("country", iso).apply(); }

    /** A country's banks: Nigeria's full list, or the country's main banks; payment apps used there come after. */
    public static List<Bank> forCountry(Context c, String iso) {
        Country k = country(c, iso);
        List<Bank> out = new ArrayList<>();
        if ("NG".equalsIgnoreCase(k.iso)) out.addAll(all(c));
        out.addAll(k.banks);
        return out;
    }

    /** A picker's rows for one country: the popular ones first when the box is empty, matches otherwise. */
    public static List<Bank> search(Context c, String iso, String typed) {
        if ("NG".equalsIgnoreCase(iso)) {
            List<Bank> out = search(c, typed);
            for (Bank b : country(c, "NG").banks) if (hits(b, norm(typed == null ? "" : typed))) out.add(b);
            return out;
        }
        String q = norm(typed == null ? "" : typed);
        List<Bank> out = new ArrayList<>();
        for (Bank b : forCountry(c, iso)) if (hits(b, q)) out.add(b);
        if (!q.isEmpty() && out.isEmpty()) {
            // Not in this country's list: look everywhere (a Ghanaian with a UK account, say).
            for (Country k : countries(c)) for (Bank b : k.banks) if (hits(b, q) && !containsName(out, b)) out.add(b);
        }
        return out;
    }

    /** By name in any country (for saved methods, whose country may be unknown): Nigeria first, then the world list. */
    public static Bank matchAnywhere(Context c, String typed) {
        Bank b = match(c, typed);
        if (b != null) return b;
        String q = norm(typed);
        if (q.isEmpty()) return null;
        for (Country k : countries(c)) for (Bank x : k.banks) if (norm(x.name).equals(q) || norm(x.shortName).equals(q)) return x;
        return null;
    }

    /** The bank a typed name means within one country, or null. */
    public static Bank match(Context c, String iso, String typed) {
        if ("NG".equalsIgnoreCase(iso)) {
            Bank b = match(c, typed);
            if (b != null) return b;
        }
        String q = norm(typed);
        if (q.isEmpty()) return null;
        Bank only = null;
        for (Bank b : country(c, iso).banks) {
            if (norm(b.name).equals(q) || norm(b.shortName).equals(q)) return b;
            if (norm(b.name).startsWith(q) || norm(b.shortName).startsWith(q)) { if (only != null) return null; only = b; }
        }
        if (only != null || q.length() < 3) return only;
        for (Bank b : country(c, iso).banks) {
            // "momo" → MTN MoMo: inside the name, when only one bank has it.
            if (norm(b.name).contains(q) || norm(b.shortName).contains(q)) { if (only != null) return null; only = b; }
        }
        return only;
    }

    private static boolean hits(Bank b, String q) {
        if (q.isEmpty()) return true;
        if (norm(b.name).contains(q) || norm(b.shortName).contains(q) || b.code.startsWith(q)) return true;
        for (String a : b.aliases) if (norm(a).contains(q)) return true;
        return false;
    }

    private static boolean containsName(List<Bank> list, Bank b) {
        for (Bank x : list) if (x.name.equals(b.name)) return true;
        return false;
    }

    private static String readAll(InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        return bo.toString("UTF-8");
    }

    private static List<Bank> cache;

    /** All banks, the bundled list merged with the last refresh. Safe on the main thread after the first call (small JSON). */
    public static synchronized List<Bank> all(Context c) {
        if (cache != null) return cache;
        List<Bank> out = new ArrayList<>();
        try (InputStream in = c.getAssets().open("banks/ng_banks.json")) {
            JSONArray arr = new JSONObject(readAll(in)).optJSONArray("banks");
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
        b.domain = o.optString("domain");
        b.shortName = o.has("short") ? o.optString("short") : b.shortName;
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
