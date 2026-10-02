package com.harvanna.app.group;

/**
 * Who may see every member's payment card.
 * After an admin handoff the Firestore creator id is the seat.
 * The node's original creator address is used only when Firestore has not named anyone,
 * so a former admin whose wallet is still on the old node field cannot keep the cards.
 */
public final class AdminSeat {
    private AdminSeat() {}

    public static boolean current(String myUid, String myWallet, String creatorId, String nodeAdmin, String myRole) {
        if (creatorId != null && !creatorId.trim().isEmpty()) {
            return myUid != null && myUid.equals(creatorId.trim());
        }
        if (nodeAdmin != null && !nodeAdmin.trim().isEmpty()) {
            return myWallet != null && myWallet.equalsIgnoreCase(nodeAdmin.trim());
        }
        if (myRole == null) return false;
        return "ADMIN".equalsIgnoreCase(myRole) || "CREATOR".equalsIgnoreCase(myRole);
    }
}
