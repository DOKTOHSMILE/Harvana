package com.harvanna.app.group;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.Base64;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.harvanna.app.CoinInfo;
import com.harvanna.app.CreateWalletActivity;
import com.harvanna.app.RestoreWalletActivity;
import com.harvanna.app.crypto.Ed25519;
import com.harvanna.app.utils.UiColors;
import com.harvanna.app.wallet.PinPromptHelper;
import com.harvanna.app.wallet.WalletManager;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Join gate. (1) A member needs a personal wallet with its signing key on this phone. (2) They read a
 * full explanation of the group (generated from the creator's settings and the node's rules) and
 * agree to it by signing the contract hash with their wallet. The result is handed back to the caller
 * so it can be stored with the join request / member record.
 */
public final class GroupWelcomeSheet {

    public interface Listener {
        /** Extra fields (walletAddress, contractHash, contractSig, contractSignedAt) to store with the join. */
        void onSigned(Map<String, Object> ackFields);
    }

    private GroupWelcomeSheet() {}

    /** Returns the wallet address if a usable wallet (with signing key) exists on this phone, else "". */
    public static String usableWallet(Activity a) {
        String addr = WalletManager.getActiveWalletAddress(a);
        if (addr == null || addr.isEmpty()) return "";
        String pk = WalletManager.loadPrivateKey(a, addr);
        return pk == null || pk.isEmpty() ? "" : addr;
    }

    public static void start(Activity a, String groupId, Map<String, Object> group, Listener l) {
        String wallet = usableWallet(a);
        if (wallet.isEmpty()) { showNeedWallet(a); return; }
        showContract(a, groupId, group, wallet, l);
    }

    private static void showNeedWallet(Activity a) {
        new androidx.appcompat.app.AlertDialog.Builder(a)
            .setTitle("You need a wallet first")
            .setMessage("Groups run on the Harvanna network, and every payment is signed by your own wallet. "
                + "Create a wallet (or restore yours) to join a group.")
            .setNegativeButton("Not now", null)
            .setNeutralButton("Restore wallet", (d, w) -> a.startActivity(new Intent(a, RestoreWalletActivity.class)))
            .setPositiveButton("Create wallet", (d, w) -> a.startActivity(new Intent(a, CreateWalletActivity.class)))
            .show();
    }

    private static void showContract(Activity a, String groupId, Map<String, Object> group, String wallet, Listener l) {
        final String ticker = CoinInfo.ticker();
        final String hash = GroupContract.hash(groupId, group, ticker);
        final float d = a.getResources().getDisplayMetrics().density;
        final int pad = (int) (16 * d);

        BottomSheetDialog dlg = new BottomSheetDialog(a);
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(UiColors.surface());

        TextView title = text(a, "Welcome to " + str(group, "groupName", "this group"), 20, true, UiColors.textPrimary());
        root.addView(title);
        root.addView(text(a, "Read how this group works before you join. You will sign it with your wallet.", 13, false, UiColors.textSecondary()));

        ScrollView sv = new ScrollView(a);
        LinearLayout body = new LinearLayout(a);
        body.setOrientation(LinearLayout.VERTICAL);
        sv.addView(body);
        for (GroupContract.Section s : GroupContract.sections(group, ticker)) {
            TextView h = text(a, s.title, 15, true, UiColors.accent());
            h.setPadding(0, (int) (14 * d), 0, (int) (4 * d));
            body.addView(h);
            for (String p : s.points) body.addView(text(a, "•  " + p, 13, false, UiColors.textPrimary()));
        }
        TextView hs = text(a, "Contract fingerprint: " + hash.substring(0, 16) + "…", 11, false, UiColors.textTertiary());
        hs.setPadding(0, (int) (14 * d), 0, 0);
        body.addView(hs);
        LinearLayout.LayoutParams svp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(sv, svp);

        CheckBox agree = new CheckBox(a);
        agree.setText("I have read and understood how this group works and agree to these terms.");
        agree.setTextColor(UiColors.textPrimary());
        root.addView(agree);

        MaterialButton sign = new MaterialButton(a);
        sign.setText("Sign contract and continue");
        sign.setAllCaps(false);
        sign.setEnabled(false);
        sign.setAlpha(0.45f);
        agree.setOnCheckedChangeListener((b, on) -> { sign.setEnabled(on); sign.setAlpha(on ? 1f : 0.45f); });
        sign.setOnClickListener(v -> PinPromptHelper.verifyPinThenProceed(a, () -> {
            try {
                long now = System.currentTimeMillis();
                String seed = WalletManager.loadPrivateKey(a, wallet);
                String msg = GroupContract.signedMessage(groupId, wallet, hash, now);
                byte[] sig = Ed25519.sign(Base64.decode(seed, Base64.NO_WRAP), msg.getBytes(StandardCharsets.UTF_8));
                Map<String, Object> ack = new HashMap<>();
                ack.put("walletAddress", wallet);
                ack.put("contractHash", hash);
                ack.put("contractSig", Base64.encodeToString(sig, Base64.NO_WRAP));
                ack.put("contractSignedAt", now);
                dlg.dismiss();
                l.onSigned(ack);
            } catch (Exception e) {
                Toast.makeText(a, "Couldn't sign: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }));
        root.addView(sign);

        dlg.setContentView(root);
        root.setMinimumHeight((int) (a.getResources().getDisplayMetrics().heightPixels * 0.85f));
        dlg.show();
        dlg.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
    }

    private static TextView text(Activity a, String s, int sp, boolean bold, int color) {
        TextView t = new TextView(a);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setPadding(0, (int) (4 * a.getResources().getDisplayMetrics().density), 0, 0);
        return t;
    }

    private static String str(Map<String, Object> m, String k, String fb) {
        Object o = m.get(k); return o == null || String.valueOf(o).trim().isEmpty() ? fb : String.valueOf(o).trim();
    }
}
