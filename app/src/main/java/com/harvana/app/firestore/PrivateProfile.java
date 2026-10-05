package com.harvana.app.firestore;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Which profile fields are personal data that only the owner may read.
 *
 * USERS/{uid} is readable by every signed-in account, so these fields live in
 * USERS/{uid}/private/kyc instead (owner-only in firestore.rules). Everything
 * not listed here stays in USERS/{uid}: a field this class does not know about
 * keeps its old home, so adding a new profile field never breaks by default.
 *
 * Pure Java on purpose, so PrivateProfileTest can run on the JVM.
 */
public final class PrivateProfile {

    /** Sub-collection and document that hold the owner-only copy. */
    public static final String SUBCOLLECTION = "private";
    public static final String DOCUMENT = "kyc";

    /** Date of birth, ID numbers, contact details, exact location and next of kin. */
    public static final Set<String> FIELDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "dob", "v_type", "bvn", "nin",
            "phone", "email", "address", "occupation",
            "gps_lat", "gps_long", "kin")));

    private PrivateProfile() {}

    public static boolean isPrivate(String field) {
        return field != null && FIELDS.contains(field);
    }

    /** Splits a profile map into [0] the public part and [1] the owner-only part. Neither input nor output is shared. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object>[] split(Map<String, Object> profile) {
        Map<String, Object> pub = new HashMap<>();
        Map<String, Object> priv = new HashMap<>();
        if (profile != null) {
            for (Map.Entry<String, Object> e : profile.entrySet()) {
                (isPrivate(e.getKey()) ? priv : pub).put(e.getKey(), e.getValue());
            }
        }
        return new Map[] {pub, priv};
    }

    /** The owner's full view: the public document with the owner-only fields laid over it. */
    public static Map<String, Object> merge(Map<String, Object> pub, Map<String, Object> priv) {
        Map<String, Object> all = new HashMap<>();
        if (pub != null) all.putAll(pub);
        if (priv != null) all.putAll(priv);
        return all;
    }

    /** Owner-only fields still sitting in the public document (accounts created before the split). */
    public static Set<String> leftoversIn(Map<String, Object> pub) {
        Set<String> out = new HashSet<>();
        if (pub != null) {
            for (String k : pub.keySet()) if (isPrivate(k)) out.add(k);
        }
        return out;
    }
}
