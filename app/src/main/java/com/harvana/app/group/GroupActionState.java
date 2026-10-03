package com.harvana.app.group;

/** Where a signed group action is. Firestore success is not one of these. */
public enum GroupActionState {
    IDLE,
    SIGNING,
    SUBMITTING,
    ACCEPTED,
    PENDING_CONFIRMATION,
    CONFIRMED,
    FAILED,
    REJECTED
}
