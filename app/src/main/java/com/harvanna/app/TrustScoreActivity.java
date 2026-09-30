package com.harvanna.app;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.harvanna.app.firestore.GroupFirestoreManager;
import com.harvanna.app.firestore.UserProfileManager;
import com.harvanna.app.sync.NodeIdentityClient;
import com.harvanna.app.utils.StatusBarUtil;
import com.harvanna.app.utils.UiColors;
import com.harvanna.app.wallet.WalletManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Explains the trust score: the overall number, what it is made of, the network's own view of the
 * wallet, and the recorded actions (payments, loans, joining groups) that feed it.
 */
public class TrustScoreActivity extends AppCompatActivity {
    private LinearLayout root;

    private static final class Ev {
        long at; String title, detail, feeds;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(UiColors.background());
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        root.setPadding(p, p, p, p);
        sv.addView(root);
        com.harvanna.app.utils.AppBar.setContent(this, sv, "Trust score", "Built from what you do in your groups");
        root.addView(tv("Calculating your score…", 14, false, UiColors.textSecondary()));
    }

    @Override protected void onResume() { super.onResume(); load(); }
    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    private void load() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        TrustScoreEngine.compute(this, uid, new TrustScoreEngine.Callback() {
            @Override public void onComplete(TrustScoreEngine.Result r) {
                events(uid, evs -> runOnUiThread(() -> render(r, evs)));
            }
            @Override public void onError(String e) {
                runOnUiThread(() -> { root.removeAllViews(); root.addView(tv("Couldn't load your score: " + e, 14, false, UiColors.error())); });
            }
        });
    }

    private interface EvCb { void done(List<Ev> evs); }

    private void nodeEvents(String uid, List<Ev> out, Runnable done) {
        String wallet = WalletManager.getActiveWalletAddress(this);
        com.harvanna.app.sync.NodePortfolio.load(this, uid, wallet, p -> {
            synchronized (p.entries) {
                for (com.harvanna.app.sync.NodePortfolio.Entry e : p.entries) {
                    Ev ev = new Ev();
                    ev.at = e.position.joinedMs;
                    ev.title = "On the network · " + e.name;
                    ev.detail = fmt(e.position.confirmedPayments) + " of " + fmt(e.position.expectedPayments) + " payments made; "
                        + fmt(e.position.savedHarv) + " " + CoinInfo.ticker() + " put in"
                        + (e.position.outstandingHarv() > 0 ? "; " + fmt(e.position.outstandingHarv()) + " " + CoinInfo.ticker() + " still owed" : "")
                        + (e.position.paymentGaps > 0 ? "; late or missed payments recorded" : "");
                    ev.feeds = "Payment history, consistency and loyalty (read from the network)";
                    out.add(ev);
                }
            }
            done.run();
        });
    }

    @SuppressWarnings("unchecked")
    private void events(String uid, EvCb cb) {
        UserProfileManager.getInstance(this).getProfile(uid, new UserProfileManager.DocCallback() {
            @Override public void onSuccess(Map<String, Object> profile) {
                Object g = profile.get("groups");
                List<String> ids = new ArrayList<>();
                if (g instanceof List) for (Object o : (List<Object>) g) if (o != null && !String.valueOf(o).trim().isEmpty()) ids.add(String.valueOf(o).trim());
                final List<Ev> out = Collections.synchronizedList(new ArrayList<>());
                nodeEvents(uid, out, () -> {
                    if (ids.isEmpty()) {
                        List<Ev> copy; synchronized (out) { copy = new ArrayList<>(out); }
                        Collections.sort(copy, (a, b) -> Long.compare(b.at, a.at));
                        cb.done(copy);
                        return;
                    }
                    final AtomicInteger left = new AtomicInteger(ids.size());
                    for (String gid : ids) groupEvents(uid, gid, out, left, cb);
                });
            }
            @Override public void onError(String e) { cb.done(new ArrayList<>()); }
        });
    }

    private void groupEvents(String uid, String gid, List<Ev> out, AtomicInteger left, EvCb cb) {
        final GroupFirestoreManager m = GroupFirestoreManager.getInstance(this);
        m.getGroup(gid, new GroupFirestoreManager.DocCallback() {
            @Override public void onSuccess(Map<String, Object> grp) { gather(m, uid, gid, s(grp.get("groupName")), out, left, cb); }
            @Override public void onError(String e) { gather(m, uid, gid, "Group", out, left, cb); }
        });
    }

    private void gather(GroupFirestoreManager m, String uid, String gid, String name, List<Ev> out, AtomicInteger left, EvCb cb) {
        final AtomicInteger parts = new AtomicInteger(3);
        final Runnable part = () -> {
            if (parts.decrementAndGet() == 0 && left.decrementAndGet() == 0) {
                List<Ev> copy;
                synchronized (out) { copy = new ArrayList<>(out); }
                Collections.sort(copy, (a, b) -> Long.compare(b.at, a.at));
                cb.done(copy);
            }
        };
        m.getMembers(gid, new GroupFirestoreManager.ListCallback() {
            @Override public void onSuccess(List<Map<String, Object>> items) {
                for (Map<String, Object> x : items) if (uid.equals(s(x.get("memberUid")))) {
                    Ev e = new Ev(); e.at = t(x, "joinedAt"); e.title = "Joined " + name;
                    e.detail = "Membership status: " + s(x.get("status")); e.feeds = "Tenure and loyalty";
                    out.add(e);
                }
                part.run();
            }
            @Override public void onError(String err) { part.run(); }
        });
        m.getPayments(gid, new GroupFirestoreManager.ListCallback() {
            @Override public void onSuccess(List<Map<String, Object>> items) {
                for (Map<String, Object> x : items) if (uid.equals(s(x.get("memberUid")))) {
                    String st = s(x.get("status")).toUpperCase(Locale.US);
                    boolean ok = st.equals("CONFIRMED") || st.equals("PAID");
                    Ev e = new Ev(); e.at = t(x, "timestamp"); if (e.at == 0) e.at = t(x, "createdAt");
                    e.title = (ok ? "Payment confirmed" : "Payment " + st.toLowerCase(Locale.US)) + " · " + name;
                    e.detail = fmt(n(x.get("amountHarv"))) + " " + CoinInfo.ticker();
                    e.feeds = ok ? "Payment history and consistency" : "Payment history (not counted until confirmed)";
                    out.add(e);
                }
                part.run();
            }
            @Override public void onError(String err) { part.run(); }
        });
        m.getLoans(gid, new GroupFirestoreManager.ListCallback() {
            @Override public void onSuccess(List<Map<String, Object>> items) {
                for (Map<String, Object> x : items) if (uid.equals(s(x.get("borrowerUid")))) {
                    Ev e = new Ev(); e.at = t(x, "createdAt");
                    e.title = "Loan " + s(x.get("status")).toLowerCase(Locale.US) + " · " + name;
                    double p = n(x.get("principalHarv")) > 0 ? n(x.get("principalHarv")) : n(x.get("amountHarv"));
                    e.detail = "Borrowed " + fmt(p) + " " + CoinInfo.ticker() + ", repaid " + fmt(n(x.get("repaidAmountHarv")));
                    e.feeds = "Borrowing record";
                    out.add(e);
                }
                part.run();
            }
            @Override public void onError(String err) { part.run(); }
        });
    }

    private void render(TrustScoreEngine.Result r, List<Ev> evs) {
        root.removeAllViews();
        TextView big = tv(r.overallScore + " / 100", 34, true, UiColors.accent());
        root.addView(big);
        root.addView(tv(r.tier + " · " + tier(r.overallScore), 14, true, UiColors.textPrimary()));
        root.addView(tv("Your score grows when you pay on time, keep paying regularly, stay in your groups, and repay what you borrow.", 13, false, UiColors.textSecondary()));

        root.addView(head("What your score is made of"));
        root.addView(bar("Payment history (30%)", r.paymentHistoryPercent));
        root.addView(bar("Consistency (20%)", r.consistencyPercent));
        root.addView(bar("Group loyalty (15%)", r.groupLoyaltyPercent));
        root.addView(bar("Time on Harvanna (15%)", r.tenurePercent));
        root.addView(bar("Verified profile (10%)", r.verified ? Math.max(50, r.profileCompletenessPercent) : r.profileCompletenessPercent / 2));
        root.addView(tv("Reports against you (10%): " + r.reportCount + (r.reportCount == 0 ? " · none" : ""), 13, false, UiColors.textSecondary()));

        root.addView(head("The network's view of your wallet"));
        final TextView net = tv("Checking…", 13, false, UiColors.textSecondary());
        root.addView(net);
        String addr = WalletManager.getActiveWalletAddress(this);
        if (addr == null || addr.isEmpty()) net.setText("No wallet yet. Create one so the network can record your trust.");
        else NodeIdentityClient.lookup(addr, new NodeIdentityClient.Callback() {
            @Override public void onSuccess(org.json.JSONObject j) {
                runOnUiThread(() -> net.setText("Network trust " + j.optInt("trustScore", 0) + " / 26 · "
                    + (j.optBoolean("bound") ? "verified person" : "not verified yet") + " · " + j.optInt("effectiveVouchCount", 0)
                    + " vouch(es). The network records this separately from the score above."));
            }
            @Override public void onError(String e) { runOnUiThread(() -> net.setText("Network trust unavailable right now.")); }
        });

        root.addView(head("Actions recorded on your account"));
        if (evs.isEmpty()) {
            root.addView(tv("Nothing recorded yet. Join a group and make your first payment to start building your score.", 13, false, UiColors.textSecondary()));
            return;
        }
        int shown = 0;
        for (Ev e : evs) {
            if (shown++ >= 60) break;
            root.addView(eventRow(e));
        }
    }

    private View eventRow(Ev e) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackgroundColor(UiColors.surface());
        int p = dp(12);
        c.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        c.setLayoutParams(lp);
        c.addView(tv(e.title, 14, true, UiColors.textPrimary()));
        c.addView(tv(e.detail, 13, false, UiColors.textSecondary()));
        String when = e.at > 0 ? DateUtils.getRelativeTimeSpanString(e.at, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString() : "date unknown";
        c.addView(tv(when + " · counts toward: " + e.feeds, 11, false, UiColors.textTertiary()));
        return c;
    }

    private View bar(String label, int pct) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.addView(tv(label + " · " + Math.max(0, Math.min(100, pct)) + "%", 13, false, UiColors.textPrimary()));
        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        pb.setProgress(Math.max(0, Math.min(100, pct)));
        pb.setProgressTintList(android.content.res.ColorStateList.valueOf(UiColors.accent()));
        c.addView(pb, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(10)));
        return c;
    }

    private static String tier(int s) {
        if (s >= 75) return "strong reputation";
        if (s >= 50) return "good standing";
        return "keep building";
    }
    private TextView head(String t) {
        TextView v = tv(t, 16, true, UiColors.textPrimary());
        v.setPadding(0, dp(20), 0, dp(6));
        return v;
    }
    private static long t(Map<String, Object> m, String k) {
        Object o = m.get(k);
        if (o instanceof Number) return ((Number) o).longValue();
        try { return o == null ? 0 : Long.parseLong(String.valueOf(o)); } catch (Exception e) { return 0; }
    }
    private static String fmt(double v) { return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.US, "%.2f", v); }
    private static double n(Object o) { return o instanceof Number ? ((Number) o).doubleValue() : 0; }
    private static String s(Object o) { return o == null ? "" : String.valueOf(o).trim(); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private TextView tv(String t, int sp, boolean bold, int color) {
        TextView v = new TextView(this);
        v.setText(t); v.setTextSize(sp); v.setTextColor(color);
        if (bold) v.setTypeface(null, Typeface.BOLD);
        v.setPadding(0, dp(3), 0, dp(3));
        return v;
    }
}
