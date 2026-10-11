package com.harvana.app.banks;

import org.json.JSONObject;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Free, key-less lookups that fill in the bank from the account details:
 *   IBAN → bank name and BIC (openiban.com; Germany, Austria, Switzerland, Netherlands, Belgium, Luxembourg, Liechtenstein)
 *   IFSC → bank and branch (ifsc.razorpay.com; India)
 * Only the IBAN or IFSC is sent, never the account holder's name. Run off the main thread; null when nothing is found.
 */
public final class BankLookup {
    private BankLookup() {}

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build();

    public static final class Result { public String bankName = "", bic = "", branch = ""; }

    public static Result iban(String iban) {
        String c = AccountRules.compact(iban);
        if (!AccountRules.isIban(c)) return null;
        String cc = c.substring(0, 2);
        if (!"DE AT CH NL BE LU LI".contains(cc)) return null;
        JSONObject j = get("https://openiban.com/validate/" + c + "?getBIC=true&validateBankCode=true");
        JSONObject d = j == null ? null : j.optJSONObject("bankData");
        if (d == null || d.optString("name").isEmpty()) return null;
        Result r = new Result();
        r.bankName = d.optString("name");
        r.bic = d.optString("bic");
        r.branch = d.optString("city");
        return r;
    }

    public static Result ifsc(String code) {
        if (!AccountRules.isIfsc(code)) return null;
        JSONObject j = get("https://ifsc.razorpay.com/" + code.trim().toUpperCase(Locale.US));
        if (j == null || j.optString("BANK").isEmpty()) return null;
        Result r = new Result();
        r.bankName = j.optString("BANK");
        r.branch = j.optString("BRANCH");
        r.bic = j.optString("SWIFT", "");
        return r;
    }

    private static JSONObject get(String url) {
        try (Response r = HTTP.newCall(new Request.Builder().url(url).build()).execute()) {
            if (!r.isSuccessful() || r.body() == null) return null;
            return new JSONObject(r.body().string());
        } catch (Exception e) {
            return null;
        }
    }
}
