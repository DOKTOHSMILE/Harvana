package com.harvana.app.chain;

/** Signed messages for the node's identity-link and personhood endpoints (timestamp based, not nonce based). */
public final class IdentityCanonical {
    private IdentityCanonical() {}

    /** IdentityLinkService.message(): DOMAIN:address:timestamp */
    public static String identityLink(String address, long timestamp) {
        return "HARVANNA_IDENTITY_LINK:" + address + ":" + timestamp;
    }

    /** Personhood.canonicalMessage() */
    public static String personhoodBind(String address, String deviceAttestationHash, long issuedAt) {
        return "PERSONHOOD_BIND:" + address + ":" + deviceAttestationHash + ":" + issuedAt;
    }

    /** Personhood.vouchMessage() */
    public static String personhoodVouch(String voucher, String subject, long timestamp) {
        return "PERSONHOOD_VOUCH:" + voucher + ":" + subject + ":" + timestamp;
    }
}
