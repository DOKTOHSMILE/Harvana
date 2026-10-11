package com.harvana.app.banks;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * How a bank account is written in each country, and the checks the bank itself would make. Pure Java (no Android),
 * so it is tested on the JVM (AccountRulesTest).
 *
 *   NUBAN       Nigeria: 10-digit account (CBN check digit tested in BankDirectory)
 *   IBAN        Europe, Gulf, Türkiye, Egypt, Pakistan: IBAN (ISO 13616 mod-97, per-country length) + optional BIC
 *   SORT_CODE   UK: 6-digit sort code + 8-digit account
 *   ABA         US: 9-digit routing number (ABA checksum) + account
 *   CA_TRANSIT  Canada: 3-digit institution + 5-digit transit + account
 *   BSB         Australia: 6-digit BSB + account
 *   IFSC        India: IFSC (AAAA0XXXXXX) + account
 *   CLABE       Mexico: 18-digit CLABE (weights 3,7,1 check digit)
 *   PIX         Brazil: Pix key (CPF, CNPJ, e-mail, phone or random key)
 *   ACCOUNT_BRANCH / ACCOUNT_SWIFT   elsewhere: account number, optional branch code and SWIFT/BIC
 * Payment apps (PayPal, Wise…) take an e-mail, phone or @tag; mobile money takes a phone number.
 */
public final class AccountRules {
    private AccountRules() {}

    /** One extra box the form shows under the account number. */
    public static final class Field {
        public final String key, label, hint;
        public final boolean digits, required;
        Field(String key, String label, String hint, boolean digits, boolean required) {
            this.key = key; this.label = label; this.hint = hint; this.digits = digits; this.required = required;
        }
    }

    /** What the main box asks for, and which extra boxes follow it. */
    public static final class Layout {
        public String mainLabel = "ACCOUNT NUMBER", mainHint = "Account number";
        public boolean mainDigits = true;
        public final List<Field> extras = new ArrayList<>();
    }

    public static Layout layout(String scheme, String kind) {
        Layout l = new Layout();
        if ("fintech".equals(kind) && !"NUBAN".equals(scheme) && !"PIX".equals(scheme)) {
            l.mainLabel = "E-MAIL, PHONE OR @TAG"; l.mainHint = "The address you receive with in this app"; l.mainDigits = false;
            return l;
        }
        if ("mobile_money".equals(kind)) {
            l.mainLabel = "PHONE NUMBER"; l.mainHint = "Phone number with country code, e.g. +233…"; l.mainDigits = false;
            return l;
        }
        switch (scheme == null ? "" : scheme) {
            case "NUBAN": l.mainHint = "10-digit account number"; break;
            case "IBAN":
                l.mainLabel = "IBAN"; l.mainHint = "e.g. DE89 3704 0044 0532 0130 00"; l.mainDigits = false;
                l.extras.add(new Field("bic", "BIC / SWIFT (OPTIONAL)", "8 or 11 characters", false, false));
                break;
            case "SORT_CODE":
                l.mainHint = "8-digit account number";
                l.extras.add(new Field("sortCode", "SORT CODE", "6 digits, e.g. 20-00-00", true, true));
                break;
            case "ABA":
                l.extras.add(new Field("routing", "ROUTING NUMBER (ABA)", "9 digits", true, true));
                break;
            case "CA_TRANSIT":
                l.extras.add(new Field("institution", "INSTITUTION NUMBER", "3 digits", true, true));
                l.extras.add(new Field("transit", "TRANSIT NUMBER", "5 digits", true, true));
                break;
            case "BSB":
                l.extras.add(new Field("bsb", "BSB", "6 digits, e.g. 062-000", true, true));
                break;
            case "IFSC":
                l.extras.add(new Field("ifsc", "IFSC", "11 characters, e.g. HDFC0000001", false, true));
                break;
            case "CLABE":
                l.mainLabel = "CLABE"; l.mainHint = "18 digits";
                break;
            case "PIX":
                l.mainLabel = "PIX KEY"; l.mainHint = "CPF, CNPJ, e-mail, phone or random key"; l.mainDigits = false;
                break;
            case "ACCOUNT_BRANCH":
                l.mainDigits = false;
                l.extras.add(new Field("branch", "BRANCH CODE (OPTIONAL)", "If your bank uses one", false, false));
                l.extras.add(new Field("bic", "SWIFT / BIC (OPTIONAL)", "8 or 11 characters", false, false));
                break;
            default:
                l.mainDigits = false;
                l.extras.add(new Field("bic", "SWIFT / BIC (OPTIONAL)", "8 or 11 characters", false, false));
        }
        return l;
    }

    /** Null when the account is fine, otherwise one plain sentence saying what to fix. */
    public static String check(String scheme, String kind, String country, String main, Map<String, String> extras) {
        String m = main == null ? "" : main.trim();
        if (m.isEmpty()) return "Enter the account details.";
        if ("fintech".equals(kind) || "mobile_money".equals(kind)) {
            if ("mobile_money".equals(kind) && digits(m).length() < 7) return "Enter the full phone number.";
            return null;
        }
        Map<String, String> x = extras == null ? new HashMap<>() : extras;
        for (Field f : layout(scheme, kind).extras) {
            String v = x.get(f.key) == null ? "" : x.get(f.key).trim();
            if (f.required && v.isEmpty()) return "Enter the " + sentence(f.label) + ".";
        }
        String bic = x.get("bic") == null ? "" : x.get("bic").trim();
        if (!bic.isEmpty() && !isBic(bic)) return "A SWIFT/BIC code has 8 or 11 letters and digits, like DEUTDEFF.";
        switch (scheme == null ? "" : scheme) {
            case "NUBAN":
                return digits(m).length() == 10 ? null : "Nigerian account numbers have 10 digits.";
            case "IBAN": {
                String iban = compact(m);
                if (!isIban(iban)) return "That IBAN doesn't check out. Look for a mistyped or missing character.";
                if (country != null && country.length() == 2 && !iban.startsWith(country.toUpperCase(Locale.US)))
                    return "That IBAN belongs to another country (" + iban.substring(0, 2) + "). Change the country or the IBAN.";
                return null;
            }
            case "SORT_CODE":
                if (digits(x.get("sortCode")).length() != 6) return "A sort code has 6 digits.";
                return digits(m).length() == 8 ? null : "UK account numbers have 8 digits.";
            case "ABA":
                if (!isAbaRouting(digits(x.get("routing")))) return "That routing number doesn't check out. It has 9 digits; check each one.";
                return digits(m).length() >= 4 && digits(m).length() <= 17 ? null : "US account numbers have 4 to 17 digits.";
            case "CA_TRANSIT":
                if (digits(x.get("institution")).length() != 3) return "The institution number has 3 digits.";
                if (digits(x.get("transit")).length() != 5) return "The transit number has 5 digits.";
                return digits(m).length() >= 7 && digits(m).length() <= 12 ? null : "Canadian account numbers have 7 to 12 digits.";
            case "BSB":
                if (digits(x.get("bsb")).length() != 6) return "A BSB has 6 digits.";
                return digits(m).length() >= 6 && digits(m).length() <= 10 ? null : "Australian account numbers have 6 to 10 digits.";
            case "IFSC":
                if (!isIfsc(x.get("ifsc"))) return "An IFSC has 11 characters: 4 letters, a zero, then 6 letters or digits.";
                return digits(m).length() >= 9 && digits(m).length() <= 18 ? null : "Indian account numbers have 9 to 18 digits.";
            case "CLABE":
                return isClabe(digits(m)) ? null : "That CLABE doesn't check out. It has 18 digits; check each one.";
            default:
                return m.replaceAll("[\\s-]", "").length() >= 5 ? null : "That account number looks too short.";
        }
    }

    /** The details folded into one line, which is what the P2P listing sends as the account number. */
    public static String compose(String scheme, String kind, String main, Map<String, String> extras) {
        String m = main == null ? "" : main.trim();
        Map<String, String> x = extras == null ? new HashMap<>() : extras;
        if ("fintech".equals(kind) || "mobile_money".equals(kind)) return m;
        StringBuilder sb = new StringBuilder();
        switch (scheme == null ? "" : scheme) {
            case "IBAN": sb.append(formatIban(compact(m))); break;
            case "SORT_CODE": sb.append("Sort code ").append(sortCode(digits(x.get("sortCode")))).append(" · Acct ").append(digits(m)); break;
            case "ABA": sb.append("Routing ").append(digits(x.get("routing"))).append(" · Acct ").append(digits(m)); break;
            case "CA_TRANSIT": sb.append("Inst ").append(digits(x.get("institution"))).append(" · Transit ").append(digits(x.get("transit")))
                    .append(" · Acct ").append(digits(m)); break;
            case "BSB": { String b = digits(x.get("bsb")); sb.append("BSB ").append(b.length() == 6 ? b.substring(0, 3) + "-" + b.substring(3) : b)
                    .append(" · Acct ").append(digits(m)); break; }
            case "IFSC": sb.append("IFSC ").append(x.get("ifsc") == null ? "" : x.get("ifsc").trim().toUpperCase(Locale.US)).append(" · Acct ").append(digits(m)); break;
            case "CLABE": sb.append("CLABE ").append(digits(m)); break;
            case "PIX": sb.append("Pix ").append(m); break;
            case "NUBAN": sb.append(digits(m)); break;
            default:
                sb.append(m);
                String br = x.get("branch") == null ? "" : x.get("branch").trim();
                if (!br.isEmpty()) sb.append(" · Branch ").append(br);
        }
        String bic = x.get("bic") == null ? "" : x.get("bic").trim().toUpperCase(Locale.US);
        if (!bic.isEmpty()) sb.append(" · BIC ").append(bic);
        return sb.toString();
    }

    // ── The checks ──

    private static final Map<String, Integer> IBAN_LENGTH = new LinkedHashMap<>();
    static {
        String t = "AD24 AE23 AL28 AT20 AZ28 BA20 BE16 BG22 BH22 BR29 BY28 CH21 CR22 CY28 CZ24 DE22 DK18 DO28 EE20 EG29 ES24 FI18 FO18 "
                + "FR27 GB22 GE22 GI23 GL18 GR27 GT28 HR21 HU28 IE22 IL23 IQ23 IS26 IT27 JO30 KW30 KZ20 LB28 LC32 LI21 LT20 LU20 LV21 "
                + "MC27 MD24 ME22 MK19 MR27 MT31 MU30 NL18 NO15 PK24 PL28 PS29 PT25 QA29 RO24 RS22 SA24 SC31 SE24 SI19 SK24 SM27 ST25 "
                + "SV28 TL23 TN24 TR26 UA29 VA22 VG24 XK20";
        for (String p : t.split(" ")) IBAN_LENGTH.put(p.substring(0, 2), Integer.parseInt(p.substring(2)));
    }

    /** ISO 13616: the country's exact length and the mod-97 check equal to 1. */
    public static boolean isIban(String raw) {
        String iban = compact(raw);
        if (iban.length() < 15 || !iban.matches("[A-Z]{2}\\d{2}[A-Z0-9]+")) return false;
        Integer len = IBAN_LENGTH.get(iban.substring(0, 2));
        if (len == null || len != iban.length()) return false;
        String moved = iban.substring(4) + iban.substring(0, 4);
        StringBuilder num = new StringBuilder();
        for (char ch : moved.toCharArray()) num.append(Character.isDigit(ch) ? String.valueOf(ch) : String.valueOf(ch - 'A' + 10));
        return new BigInteger(num.toString()).mod(BigInteger.valueOf(97)).intValue() == 1;
    }

    public static boolean isBic(String raw) {
        String b = raw == null ? "" : raw.trim().toUpperCase(Locale.US).replace(" ", "");
        return b.matches("[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?");
    }

    /** ABA: 3·(d1+d4+d7) + 7·(d2+d5+d8) + (d3+d6+d9) is a multiple of 10. */
    public static boolean isAbaRouting(String d) {
        if (d == null || !d.matches("\\d{9}")) return false;
        int[] w = {3, 7, 1, 3, 7, 1, 3, 7, 1};
        int sum = 0;
        for (int i = 0; i < 9; i++) sum += (d.charAt(i) - '0') * w[i];
        return sum % 10 == 0;
    }

    public static boolean isIfsc(String raw) {
        return raw != null && raw.trim().toUpperCase(Locale.US).matches("[A-Z]{4}0[A-Z0-9]{6}");
    }

    /** CLABE: weights 3,7,1 over the first 17 digits; the 18th is (10 − sum mod 10) mod 10. */
    public static boolean isClabe(String d) {
        if (d == null || !d.matches("\\d{18}")) return false;
        int[] w = {3, 7, 1};
        int sum = 0;
        for (int i = 0; i < 17; i++) sum += ((d.charAt(i) - '0') * w[i % 3]) % 10;
        return (10 - sum % 10) % 10 == d.charAt(17) - '0';
    }

    /** The Mexican bank a CLABE belongs to (its first three digits), or "". */
    public static String clabeBank(String d) {
        if (d == null || d.length() < 3) return "";
        switch (d.substring(0, 3)) {
            case "002": return "Citibanamex";
            case "012": return "BBVA México";
            case "014": return "Santander México";
            case "021": return "HSBC México";
            case "072": return "Banorte";
            case "127": return "Banco Azteca";
            case "722": return "Mercado Pago";
            default: return "";
        }
    }

    public static String formatIban(String iban) {
        String c = compact(iban);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < c.length(); i++) { if (i > 0 && i % 4 == 0) sb.append(' '); sb.append(c.charAt(i)); }
        return sb.toString();
    }

    static String compact(String s) { return s == null ? "" : s.replaceAll("[\\s-]", "").toUpperCase(Locale.US); }
    static String digits(String s) { return s == null ? "" : s.replaceAll("\\D", ""); }
    private static String sortCode(String d) { return d.length() == 6 ? d.substring(0, 2) + "-" + d.substring(2, 4) + "-" + d.substring(4) : d; }
    private static String sentence(String label) {
        String l = label.replace(" (OPTIONAL)", "").toLowerCase(Locale.US);
        return l.replace("aba", "ABA").replace("bsb", "BSB").replace("ifsc", "IFSC");
    }
}
