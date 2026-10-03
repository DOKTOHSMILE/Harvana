package com.harvana.app.engine;

/** One thing the signed-in member can do right now in a group. The UI turns these into buttons. */
public final class GroupAction {
    public final String id;
    public final String label;
    public final boolean primary;
    /** Optional ref the action applies to (a line id, a proposal id...). */
    public final String ref;

    public GroupAction(String id, String label, boolean primary, String ref) {
        this.id = id; this.label = label; this.primary = primary; this.ref = ref;
    }

    public GroupAction(String id, String label, boolean primary) { this(id, label, primary, ""); }

    public static final String JOIN = "JOIN", CONTRIBUTE = "CONTRIBUTE", WITHDRAW_BOND = "WITHDRAW_BOND",
        START_LINE = "START_LINE", ACCUM_CONTRIBUTE = "ACCUM_CONTRIBUTE", ACCUM_CONTRIBUTE_MULTI = "ACCUM_CONTRIBUTE_MULTI",
        ACCUM_PACK = "ACCUM_PACK", ACCUM_CLAIM = "ACCUM_CLAIM", ACCUM_APPROVE_PACK = "ACCUM_APPROVE_PACK",
        FIXED_DEPOSIT = "FIXED_DEPOSIT", FIXED_REQUEST_DISBURSEMENT = "FIXED_REQUEST_DISBURSEMENT",
        FIXED_REQUEST_MILESTONE = "FIXED_REQUEST_MILESTONE", FIXED_REQUEST_GRANT = "FIXED_REQUEST_GRANT",
        VOTE_YES = "VOTE_YES", VOTE_NO = "VOTE_NO";
}
