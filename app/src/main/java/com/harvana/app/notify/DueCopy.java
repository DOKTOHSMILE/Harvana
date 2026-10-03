package com.harvana.app.notify;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Wording for a payment-due alert. Pure Java so the phone and the unit test say the same thing. */
public final class DueCopy {
    private DueCopy() {}

    public static String title(String groupName) {
        String name = groupName == null ? "" : groupName.trim();
        return "Payment due: " + (name.isEmpty() ? "Group" : name);
    }

    public static String body(String amountText, String ticker) {
        String coin = ticker == null || ticker.trim().isEmpty() ? "HARV" : ticker.trim();
        String amount = amountText == null || amountText.trim().isEmpty() ? "0" : amountText.trim();
        if (amount.endsWith(" " + coin)) return "Your payment of " + amount + " is due.";
        return "Your payment of " + amount + " " + coin + " is due.";
    }

    public static String key(String groupId, String day) {
        return "due_" + (groupId == null ? "" : groupId) + "_" + (day == null ? "" : day);
    }

    public static String dayKey(long nowMs) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd", Locale.US);
        fmt.setTimeZone(TimeZone.getDefault());
        return fmt.format(new Date(nowMs));
    }
}
