package com.harvana.app.engine;

/** One thing the signed-in member can do right now in a group. The UI turns these into buttons. */
public final class GroupAction {
    public final String id;
    public final String label;
    public final boolean primary;
    /** Optional ref the action applies to (a line id, a proposal id...). */
    public final String ref;
    /** False when the member must see the action but must not send it. The label is the reason. */
    public final boolean enabled;

    public GroupAction(String id, String label, boolean primary, String ref) {
        this(id, label, primary, ref, true);
    }

    public GroupAction(String id, String label, boolean primary, String ref, boolean enabled) {
        this.id = id; this.label = label; this.primary = primary; this.ref = ref; this.enabled = enabled;
    }

    public GroupAction(String id, String label, boolean primary) { this(id, label, primary, ""); }

    /** A visible, inert button. Used when paying now would be rejected by the node. */
    public static GroupAction closed(String notice) {
        return new GroupAction("CLOSED", notice, false, "", false);
    }

    public static final String JOIN = "JOIN", CONTRIBUTE = "CONTRIBUTE", WITHDRAW_BOND = "WITHDRAW_BOND",
        START_LINE = "START_LINE", ACCUM_CONTRIBUTE = "ACCUM_CONTRIBUTE", ACCUM_CONTRIBUTE_MULTI = "ACCUM_CONTRIBUTE_MULTI",
        ACCUM_PACK = "ACCUM_PACK", ACCUM_CLAIM = "ACCUM_CLAIM", ACCUM_APPROVE_PACK = "ACCUM_APPROVE_PACK",
        FIXED_DEPOSIT = "FIXED_DEPOSIT", FIXED_REQUEST_DISBURSEMENT = "FIXED_REQUEST_DISBURSEMENT",
        FIXED_REQUEST_MILESTONE = "FIXED_REQUEST_MILESTONE", FIXED_REQUEST_GRANT = "FIXED_REQUEST_GRANT",
        VOTE_YES = "VOTE_YES", VOTE_NO = "VOTE_NO";
}
