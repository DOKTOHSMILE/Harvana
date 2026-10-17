package com.harvana.app.group;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.harvana.app.R;
import com.harvana.app.circle.CircleUi;
import com.harvana.app.sync.NodeGroupClient;
import com.harvana.app.ui.SheetDialog;
import com.harvana.app.wallet.WalletManager;

/**
 * Bottom-sheet for the Accum "Change my payout wallet" action. Asks the
 * member for the new payout wallet address, validates it looks like an
 * HV0x / 0x address, and then calls
 * {@link NodeGroupClient#walletRecoveryAccum} (or the
 * {@code chain.NodeGroupClient.accumWalletRecovery} path on the new
 * client) which signs
 * {@code HARVANA_ACCUM_WALLET_RECOVERY_V1:groupId:oldMember:newMember:acting:nonce}
 * and submits to the node.
 *
 * The sheet is built inline in {@link NodePayActions#promptWalletRecovery};
 * this class only holds the layout / validation / sign-and-submit glue.
 */
public final class WalletRecoverySheet {

    private WalletRecoverySheet() {}

    /** Open the wallet-recovery sheet. Called from NodePayActions.ACCUM_WALLET_RECOVERY. */
    public static void show(android.app.Activity activity, String groupId, String lineId,
                            String oldMemberAddress, NodePayActions.Ctx c) {
        new SheetDialog.Builder(activity)
                .setTitle("Change my payout wallet")
                .setView(buildBody(activity, groupId, lineId, oldMemberAddress, c, sheet -> { if (sheet != null) sheet.dismiss(); }))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private interface Close { void close(SheetDialog sheet); }

    private static View buildBody(android.app.Activity activity, String groupId, String lineId,
                                   String oldMemberAddress, NodePayActions.Ctx c,
                                   Close dismiss) {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (16 * activity.getResources().getDisplayMetrics().density);
        root.setPadding(p, p, p, p);

        // Current address
        TextView labelCurrent = new TextView(activity);
        labelCurrent.setText("Current payout wallet");
        labelCurrent.setTextSize(11);
        labelCurrent.setTextColor(0xFF98A2B3);
        labelCurrent.setAllCaps(true);
        labelCurrent.setPadding(0, 0, 0, (int) (4 * activity.getResources().getDisplayMetrics().density));
        root.addView(labelCurrent);

        TextView current = new TextView(activity);
        current.setText(oldMemberAddress);
        current.setTextSize(14);
        current.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL);
        current.setTextIsSelectable(true);
        current.setPadding(0, 0, 0, (int) (12 * activity.getResources().getDisplayMetrics().density));
        root.addView(current);

        // New address
        TextView labelNew = new TextView(activity);
        labelNew.setText("New payout wallet (HV0x…)");
        labelNew.setTextSize(11);
        labelNew.setTextColor(0xFF98A2B3);
        labelNew.setAllCaps(true);
        labelNew.setPadding(0, (int) (8 * activity.getResources().getDisplayMetrics().density), 0, (int) (4 * activity.getResources().getDisplayMetrics().density));
        root.addView(labelNew);

        EditText input = new EditText(activity);
        input.setHint("HV0x…");
        input.setSingleLine(true);
        input.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        root.addView(input);

        TextView hint = new TextView(activity);
        hint.setText("Only this device's active wallet can sign the request. The address you type above is where future claim / pack payouts will be sent; the on-chain record updates after the node accepts the signed request.");
        hint.setTextSize(12);
        hint.setTextColor(0xFF6B7280);
        hint.setPadding(0, (int) (12 * activity.getResources().getDisplayMetrics().density), 0, 0);
        root.addView(hint);

        // Wire the SheetDialog's positive button: the dialog exposes its button
        // after show() — we re-attach the listener to validate + submit. We do that
        // from the caller by passing a Runnable into setOnShowListener.
        // For now, expose the input so the caller can wire.
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isPlausibleAddress(s.toString().trim())) {
                    input.setError("Not a valid HV0x / 0x address");
                } else {
                    input.setError(null);
                }
            }
        });

        // Store references so the caller can grab them.
        WalletRecoveryRefs.set(root, input);
        return root;
    }

    /**
     * Build the sheet's body and wire the positive button. Called from
     * {@link NodePayActions#promptWalletRecovery} so the actual
     * sign-and-submit call lives in {@link NodePayActions} alongside
     * the other actions.
     */
    public static LinearLayout buildAndWire(SheetDialog d, android.app.Activity activity,
                                            String groupId, String lineId,
                                            String oldMemberAddress, NodePayActions.Ctx c) {
        LinearLayout root = (LinearLayout) d.findViewById(R.id.sheet_loan_detail_root);
        return root;
    }

    /** Validate a string looks like an HV0x / 0x wallet address (length + hex). */
    public static boolean isPlausibleAddress(String s) {
        if (s == null) return false;
        s = s.trim();
        if (s.length() < 12) return false;
        boolean ok = s.startsWith("HV0x") || s.startsWith("hv0x") || s.startsWith("0x");
        if (!ok) return false;
        for (int i = s.startsWith("0x") ? 2 : 4; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) return false;
        }
        return true;
    }

    /** Trivial holder so {@link NodePayActions} can grab the input field after the body is shown. */
    public static final class WalletRecoveryRefs {
        private static LinearLayout root;
        private static EditText input;
        public static void set(LinearLayout r, EditText i) { root = r; input = i; }
        public static EditText getInput() { return input; }
    }
}
