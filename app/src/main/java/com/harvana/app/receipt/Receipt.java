package com.harvana.app.receipt;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything a receipt says, independent of how it is drawn or shared. One of these feeds the PDF, the image and
 * the share sheet, so every receipt in the app reads the same.
 */
public final class Receipt {

    public static final class Section {
        public final String title;
        public final List<String[]> rows = new ArrayList<>();
        public Section(String title) { this.title = title; }
        /** Adds a row; empty values are left out. */
        public Section row(String label, String value) {
            if (value != null && !value.trim().isEmpty()) rows.add(new String[] {label, value.trim()});
            return this;
        }
        /** Same, for long machine values (hashes) that are wrapped in monospace. */
        public Section mono(String label, String value) {
            if (value != null && !value.trim().isEmpty()) rows.add(new String[] {label, "\u0001" + value.trim()});
            return this;
        }
    }

    /** "Payment receipt", "Transfer receipt", "Staking receipt"… */
    public String title = "Payment receipt";
    /** CONFIRMED, PENDING or FAILED (anything else is shown as typed). */
    public String status = "CONFIRMED";
    /** The headline amount without sign, e.g. "1,250.00". */
    public String amount = "";
    public String symbol = "HARV";
    /** +1 money in, -1 money out, 0 neither (shown without a sign). */
    public int direction;
    /** Milliseconds; 0 means "not stamped yet". */
    public long timestamp;
    /** The transaction hash. Drives the receipt number and the QR code. */
    public String reference = "";
    public final List<Section> sections = new ArrayList<>();

    public Section section(String title) {
        Section s = new Section(title);
        sections.add(s);
        return s;
    }

    /** A short receipt number people can quote: HV-3F9A21C07B. */
    public String number() {
        String ref = reference == null ? "" : reference.replaceAll("[^0-9A-Za-z]", "");
        if (ref.length() < 10) return "HV-" + String.format(java.util.Locale.US, "%010d", Math.abs((long) title.hashCode() * 31 + timestamp) % 10_000_000_000L);
        return "HV-" + ref.substring(0, 10).toUpperCase(java.util.Locale.US);
    }
}
