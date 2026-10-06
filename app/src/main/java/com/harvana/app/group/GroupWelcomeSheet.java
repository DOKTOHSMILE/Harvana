package com.harvana.app.group;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.util.Base64;
import android.view.ViewGroup;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.harvana.app.CoinInfo;
import com.harvana.app.CreateWalletActivity;
import com.harvana.app.RestoreWalletActivity;
import com.harvana.app.crypto.Ed25519;
import com.harvana.app.utils.UiColors;
import com.harvana.app.wallet.PinPromptHelper;
import com.harvana.app.wallet.WalletManager;

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
        new com.harvana.app.ui.SheetDialog.Builder(a)
            .setTitle("You need a wallet first")
            .setMessage("Groups run on the Harvana network, and every payment is signed by your own wallet. "
                + "Create a wallet (or restore yours) to join a group.")
            .setNegativeButton("Not now", null)
            .setNeutralButton("Restore wallet", (d, w) -> a.startActivity(new Intent(a, RestoreWalletActivity.class)))
            .setPositiveButton("Create wallet", (d, w) -> a.startActivity(new Intent(a, CreateWalletActivity.class)))
            .show();
    }

    /**
     * The contract is read one page at a time, in bold, and cannot be skipped: swiping is off, a page's box can be ticked only after its
     * text has been scrolled to the end, and Next stays off until it is ticked. The last page shows the fingerprint and signs.
     */
    private static void showContract(Activity a, String groupId, Map<String, Object> group, String wallet, Listener l) {
        final String ticker = CoinInfo.ticker();
        final String hash = GroupContract.hash(groupId, group, ticker);
        final java.util.List<GroupContract.Section> sections = GroupContract.sections(group, ticker);
        final int pages = sections.size() + 1;                       // the last page signs
        final float d = a.getResources().getDisplayMetrics().density;
        final int pad = (int) (16 * d);
        final boolean[] seenEnd = new boolean[pages];
        final boolean[] agreed = new boolean[pages];

        final BottomSheetDialog dlg = new com.harvana.app.ui.AppSheet(a);
        dlg.setCancelable(false);                                    // leaving is the Cancel button, never a stray swipe
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(UiColors.surface());

        LinearLayout head = new LinearLayout(a);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.ImageView face = new android.widget.ImageView(a);
        com.harvana.app.utils.GroupRing.show(face, str(group, "groupType", ""), str(group, "groupPhoto", ""), com.harvana.app.R.drawable.ic_coin_harv);
        head.addView(face, new LinearLayout.LayoutParams((int) (44 * d), (int) (44 * d)));
        TextView title = text(a, "Contract: " + str(group, "groupName", "this group"), 18, true, UiColors.textPrimary());
        title.setPadding((int) (12 * d), 0, 0, 0);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(head);
        final TextView step = text(a, "", 12, true, UiColors.accent());
        root.addView(step);
        final android.widget.ProgressBar bar = new android.widget.ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(pages);
        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (6 * d)));

        final CheckBox agree = new com.google.android.material.checkbox.MaterialCheckBox(a);
        final MaterialButton next = new MaterialButton(a);
        final MaterialButton back = new MaterialButton(a, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        final androidx.viewpager2.widget.ViewPager2 pager = new androidx.viewpager2.widget.ViewPager2(a);
        pager.setUserInputEnabled(false);                            // no swiping past a page that has not been agreed to

        pager.setAdapter(new androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            @Override public int getItemCount() { return pages; }
            @Override public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
                ScrollView sv = new ScrollView(a);
                sv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                sv.setFillViewport(true);
                return new androidx.recyclerview.widget.RecyclerView.ViewHolder(sv) { };
            }
            @Override public void onBindViewHolder(androidx.recyclerview.widget.RecyclerView.ViewHolder h, int position) {
                final ScrollView sv = (ScrollView) h.itemView;
                sv.removeAllViews();
                LinearLayout body = new LinearLayout(a);
                body.setOrientation(LinearLayout.VERTICAL);
                body.setPadding(0, (int) (10 * d), 0, (int) (10 * d));
                if (position < sections.size()) {
                    GroupContract.Section sec = sections.get(position);
                    TextView h1 = text(a, sec.title, 22, true, UiColors.accent());
                    h1.setPadding(0, 0, 0, (int) (8 * d));
                    body.addView(h1);
                    for (String p : sec.points) {
                        TextView t = text(a, p, 16, true, UiColors.textPrimary());
                        t.setLineSpacing(0, 1.15f);
                        t.setPadding(0, (int) (8 * d), 0, (int) (8 * d));
                        body.addView(t);
                    }
                } else {
                    body.addView(text(a, "Sign this contract", 22, true, UiColors.accent()));
                    body.addView(text(a, "You have read every page and agreed to each one. Signing writes your agreement with your wallet. It is not a payment and no money moves.", 16, true, UiColors.textPrimary()));
                    TextView hs = text(a, "Contract fingerprint: " + hash.substring(0, 16) + "\u2026", 12, false, UiColors.textTertiary());
                    hs.setPadding(0, (int) (14 * d), 0, 0);
                    body.addView(hs);
                }
                sv.addView(body);
                final int at = position;
                final Runnable check = () -> {
                    View c = sv.getChildAt(0);
                    if (c == null) return;
                    if (sv.getScrollY() + sv.getHeight() >= c.getBottom() - (int) (8 * d)) {
                        seenEnd[at] = true;
                        if (pager.getCurrentItem() == at) paint(agree, next, seenEnd, agreed, at);
                    }
                };
                sv.setOnScrollChangeListener((v, x, y, ox, oy) -> check.run());
                sv.post(check);                                      // a short page counts as read at once
            }
        });

        root.addView(pager, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        agree.setTextColor(UiColors.textPrimary());
        agree.setTypeface(null, Typeface.BOLD);
        root.addView(agree);
        LinearLayout nav = new LinearLayout(a);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        back.setText("Back");
        back.setAllCaps(false);
        MaterialButton cancel = new MaterialButton(a, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        cancel.setText("Cancel");
        cancel.setAllCaps(false);
        cancel.setOnClickListener(v -> dlg.dismiss());
        next.setAllCaps(false);
        for (MaterialButton b : new MaterialButton[]{cancel, back, next}) {
            b.setSingleLine(true);
            b.setMaxLines(1);
            b.setEllipsize(android.text.TextUtils.TruncateAt.END);
            b.setTextSize(13);
            b.setLetterSpacing(0f);
            b.setInsetTop(0); b.setInsetBottom(0);
            b.setMinWidth(0); b.setMinimumWidth(0);
            b.setPadding((int) (4 * d), 0, (int) (4 * d), 0);
        }
        LinearLayout.LayoutParams bpL = new LinearLayout.LayoutParams(0, (int) (50 * d), 1f);
        bpL.setMargins(0, 0, (int) (4 * d), 0);
        LinearLayout.LayoutParams bpM = new LinearLayout.LayoutParams(0, (int) (50 * d), 1f);
        bpM.setMargins((int) (4 * d), 0, (int) (4 * d), 0);
        LinearLayout.LayoutParams bpR = new LinearLayout.LayoutParams(0, (int) (50 * d), 1f);
        bpR.setMargins((int) (4 * d), 0, 0, 0);
        nav.addView(cancel, bpL);
        nav.addView(back, bpM);
        nav.addView(next, bpR);
        root.addView(nav);

        final Runnable refresh = () -> {
            int at = pager.getCurrentItem();
            boolean last = at == pages - 1;
            step.setText(last ? "LAST STEP" : "PAGE " + (at + 1) + " OF " + (pages - 1) + "  \u00b7  read to the end, tick the box, then Next");
            bar.setProgress(at + 1);
            back.setEnabled(at > 0);
            back.setAlpha(at > 0 ? 1f : 0.4f);
            agree.setText(last ? "I have read every page and I fully understand and agree to this contract."
                    : "I have read this page, I fully understand it and I agree.");
            agree.setOnCheckedChangeListener(null);
            agree.setChecked(agreed[at]);
            agree.setOnCheckedChangeListener((b2, on) -> { agreed[pager.getCurrentItem()] = on; paint(agree, next, seenEnd, agreed, pager.getCurrentItem()); });
            next.setText(last ? "Sign contract" : "Next");
            paint(agree, next, seenEnd, agreed, at);
        };
        pager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) { refresh.run(); }
        });
        back.setOnClickListener(v -> { if (pager.getCurrentItem() > 0) pager.setCurrentItem(pager.getCurrentItem() - 1, true); });
        next.setOnClickListener(v -> {
            int at = pager.getCurrentItem();
            if (!seenEnd[at] || !agreed[at]) return;
            if (at < pages - 1) { pager.setCurrentItem(at + 1, true); return; }
            PinPromptHelper.authorize(a, "Sign this group contract", () -> {
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
            });
        });

        dlg.setContentView(root);
        root.setMinimumHeight((int) (a.getResources().getDisplayMetrics().heightPixels * 0.92f));
        dlg.show();
        dlg.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
        dlg.getBehavior().setDraggable(false);
        refresh.run();
    }

    /** The box can be ticked only once the page is read to its end; Next works only when the box is ticked. */
    private static void paint(CheckBox agree, MaterialButton next, boolean[] seenEnd, boolean[] agreed, int at) {
        agree.setEnabled(seenEnd[at]);
        agree.setAlpha(seenEnd[at] ? 1f : 0.45f);
        boolean ok = seenEnd[at] && agreed[at];
        next.setEnabled(ok);
        next.setAlpha(ok ? 1f : 0.45f);
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
