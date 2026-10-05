package com.harvana.app.auth;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

/**
 * Who has to confirm their email before using the app. Only accounts that sign in with an email and a password: Google
 * accounts arrive with the address already confirmed by Google.
 */
public final class EmailVerification {

    /** Seconds the Resend button waits after a send, so the same inbox is not flooded. */
    public static final int RESEND_COOLDOWN_SECONDS = 60;

    private EmailVerification() {}

    public static boolean needsVerification(FirebaseUser user) {
        return user != null && usesPassword(user) && !user.isEmailVerified();
    }

    public static boolean usesPassword(FirebaseUser user) {
        if (user == null) return false;
        for (UserInfo info : user.getProviderData()) {
            if ("password".equals(info.getProviderId())) return true;
        }
        return false;
    }
}
