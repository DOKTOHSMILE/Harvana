package com.harvana.app.tx;

import android.app.Activity;
import android.app.Application;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.ui.PaymentSuccessSheet;
import com.harvana.app.ui.SpinnerDrawable;
import com.harvana.app.wallet.WalletNodeBalanceHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * One place that knows whether a payment is still on its way.
 *
 * While any payment is in flight (being signed and sent, or sent but not yet confirmed on the chain), every
 * payment button in the app (anything marked with {@link #mark(View)} or tagged {@code payment_button} in a
 * layout) is disabled, shows "Pending…" and a rolling ring, so it never looks dead. When the last payment is
 * confirmed (or the wait runs out) the buttons go back to how they were.
 *
 * It also shows the full-screen success sheet after a payment, on whichever screen is in front.
 */
public final class PaymentTracker {
    /** Layout tag that marks a payment button: android:tag="payment_button". */
    public static final String TAG = "payment_button";
    private static final String PENDING_TEXT = "Pending…";
    /** Longest wait for a confirmation before the buttons are released (about 4 minutes). */
    private static final int CONFIRM_ATTEMPTS = 60;

    private PaymentTracker() {}

    /** What the person has just reviewed and authorized: the amount and the review rows (group, to, for). */
    public static final class Review {
        public final String amount; public final List<String[]> rows; final long at;
        Review(String amount, List<String[]> rows) { this.amount = amount; this.rows = rows; this.at = System.currentTimeMillis(); }
    }
    private static Review lastReview;

    public static synchronized void noteReview(String amount, List<String[]> rows) {
        lastReview = new Review(amount == null ? "" : amount, rows == null ? new ArrayList<String[]>() : new ArrayList<>(rows));
    }

    /** The review for the payment that just went through; null if there is none or it is old (two minutes). Read once. */
    public static synchronized Review takeReview() {
        Review r = lastReview;
        lastReview = null;
        return r != null && System.currentTimeMillis() - r.at < 2 * 60_000L ? r : null;
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static int inFlight;
    private static WeakReference<Activity> current = new WeakReference<>(null);
    private static final Map<View, Saved> locked = new WeakHashMap<>();
    private static final List<PaymentSuccessSheet.Info> waiting = new ArrayList<>();
    private static WeakReference<PaymentSuccessSheet> openSheet = new WeakReference<>(null);
    private static boolean ticking;

    private static final class Saved {
        boolean enabled; CharSequence text; Drawable icon; ColorStateList iconTint; float alpha;
        SpinnerDrawable ring;
    }

    /** A payment that has started. Call {@link #end()} exactly once when it is over. */
    public static final class Handle {
        private boolean done;
        public void end() {
            MAIN.post(() -> { if (done) return; done = true; if (inFlight > 0) inFlight--; refresh(); });
        }
    }

    public static void install(Application app) {
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(Activity a) {
                current = new WeakReference<>(a);
                refresh();
                showWaiting();
            }
            @Override public void onActivityPaused(Activity a) { if (current.get() == a) current = new WeakReference<>(null); }
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        });
    }

    public static boolean busy() { return inFlight > 0; }

    /** Marks a button as a payment button (for buttons built in code; layouts use android:tag). */
    public static void mark(View v) {
        if (v == null) return;
        v.setTag(TAG);
        if (inFlight > 0) refresh();
    }

    /** Starts tracking a payment; the buttons lock now. */
    public static Handle begin() {
        Handle h = new Handle();
        MAIN.post(() -> { adoptLastTap(); inFlight++; refresh(); });
        return h;
    }

    // ── the button that started the payment ──────────────────────────────

    private static WeakReference<View> lastTap = new WeakReference<>(null);
    private static long lastTapAt;
    private static final java.util.regex.Pattern PAYISH = java.util.regex.Pattern.compile(
            "pay|send|stake|claim|repay|deposit|contribut|transfer|buy|sell|gift|confirm|save|create|publish|unstake|withdraw|accept|approve|submit");

    /** Called for every finger-up in the app: remembers the button under it, so the button that starts a payment can be locked too. */
    public static void noteTap(android.view.MotionEvent e) {
        Activity a = current.get();
        if (a == null || a.getWindow() == null) return;
        View hit = deepestClickable(a.getWindow().getDecorView(), Math.round(e.getRawX()), Math.round(e.getRawY()));
        if (hit != null) { lastTap = new WeakReference<>(hit); lastTapAt = System.currentTimeMillis(); }
    }

    private static View deepestClickable(View v, int x, int y) {
        if (v.getVisibility() != View.VISIBLE) return null;
        int[] loc = new int[2];
        v.getLocationOnScreen(loc);
        if (x < loc[0] || x > loc[0] + v.getWidth() || y < loc[1] || y > loc[1] + v.getHeight()) return null;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = g.getChildCount() - 1; i >= 0; i--) {
                View hit = deepestClickable(g.getChildAt(i), x, y);
                if (hit != null) return hit;
            }
        }
        return v.isClickable() ? v : null;
    }

    private static void adoptLastTap() {
        View v = lastTap.get();
        if (v == null || System.currentTimeMillis() - lastTapAt > 5 * 60_000L || TAG.equals(v.getTag())) return;
        boolean button = v instanceof android.widget.Button;
        String name = "";
        try { if (v.getId() != View.NO_ID) name = v.getResources().getResourceEntryName(v.getId()).toLowerCase(java.util.Locale.US); } catch (Exception ignored) {}
        if ((button || PAYISH.matcher(name).find()) && v.getTag() == null) v.setTag(TAG);
    }

    /** Ends a payment once the chain has confirmed it (or after the wait runs out, or at once when it failed). */
    public static void endWhenConfirmed(Handle h, String txHash, Runnable onConfirmed) {
        if (txHash == null || txHash.isEmpty() || "ok".equals(txHash)) { h.end(); return; }
        WalletNodeBalanceHelper.watchConfirmation(txHash, CONFIRM_ATTEMPTS, () -> {
            if (onConfirmed != null) onConfirmed.run();
            h.end();
        }, h::end);
    }

    // ── success sheet ────────────────────────────────────────────────────

    /** Shows the success sheet on the screen in front (or the next one if that screen is closing). */
    public static void success(PaymentSuccessSheet.Info info) {
        MAIN.post(() -> {
            waiting.add(info);
            MAIN.postDelayed(PaymentTracker::showWaiting, 350);
        });
    }

    /** Tells an open success sheet the chain has confirmed it. */
    public static void markConfirmed(String txHash) {
        MAIN.post(() -> {
            for (PaymentSuccessSheet.Info i : waiting) if (txHash != null && txHash.equals(i.txHash)) i.confirmed = true;
            PaymentSuccessSheet s = openSheet.get();
            if (s != null && s.isShowing() && txHash != null && txHash.equals(shownHash)) s.setConfirmed(true);
        });
    }

    private static String shownHash = "";

    private static void showWaiting() {
        Activity a = current.get();
        if (a == null || a.isFinishing() || waiting.isEmpty()) return;
        PaymentSuccessSheet existing = openSheet.get();
        if (existing != null && existing.isShowing()) return;
        PaymentSuccessSheet.Info info = waiting.remove(0);
        com.harvana.app.group.TransactionProgressSheet.dismissActive();
        try {
            openSheet = new WeakReference<>(PaymentSuccessSheet.show(a, info));
            shownHash = info.txHash == null ? "" : info.txHash;
        } catch (Throwable t) {
            // a screen that cannot host a sheet (closing); the next resumed screen shows it
            waiting.add(0, info);
        }
    }

    // ── locking the buttons ──────────────────────────────────────────────

    private static void refresh() {
        if (inFlight > 0) {
            lockAll();
            if (!ticking) { ticking = true; MAIN.postDelayed(TICK, 500); }
        } else {
            unlockAll();
        }
    }

    private static final Runnable TICK = new Runnable() {
        @Override public void run() {
            if (inFlight <= 0) { ticking = false; return; }
            lockAll();
            MAIN.postDelayed(this, 500);
        }
    };

    private static void lockAll() {
        Activity a = current.get();
        if (a == null || a.getWindow() == null) return;
        walk(a.getWindow().getDecorView(), true);
    }

    private static void unlockAll() {
        for (Iterator<Map.Entry<View, Saved>> it = locked.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<View, Saved> e = it.next();
            View v = e.getKey();
            Saved s = e.getValue();
            if (v != null) restore(v, s);
            it.remove();
        }
    }

    private static void walk(View v, boolean lock) {
        if (TAG.equals(v.getTag())) { lock(v); return; }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) walk(g.getChildAt(i), lock);
        }
    }

    private static void lock(View v) {
        Saved s = locked.get(v);
        if (s == null) {
            s = new Saved();
            s.enabled = v.isEnabled();
            s.alpha = v.getAlpha();
            locked.put(v, s);
            if (v instanceof MaterialButton) {
                MaterialButton b = (MaterialButton) v;
                s.text = b.getText();
                s.icon = b.getIcon();
                s.iconTint = b.getIconTint();
                s.ring = new SpinnerDrawable(Math.round(18 * v.getResources().getDisplayMetrics().density), b.getCurrentTextColor());
                b.setText(PENDING_TEXT);
                b.setIcon(s.ring);
                b.setIconTint(null);
                b.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
                s.ring.start();
            } else if (v instanceof TextView && !(v instanceof android.widget.EditText)) {
                TextView t = (TextView) v;
                s.text = t.getText();
                s.ring = new SpinnerDrawable(Math.round(16 * v.getResources().getDisplayMetrics().density), t.getCurrentTextColor());
                t.setText(PENDING_TEXT);
                t.setCompoundDrawablesRelativeWithIntrinsicBounds(s.ring, null, null, null);
                t.setCompoundDrawablePadding(Math.round(8 * v.getResources().getDisplayMetrics().density));
                s.ring.start();
            }
        }
        // a screen that repaints its own label (a countdown, "Paying…") must not undo the notice
        if (v instanceof TextView && !(v instanceof android.widget.EditText) && !PENDING_TEXT.contentEquals(((TextView) v).getText())) {
            ((TextView) v).setText(PENDING_TEXT);
        }
        v.setEnabled(false);
        v.setAlpha(0.7f);
    }

    private static void restore(View v, Saved s) {
        if (s.ring != null) s.ring.stop();
        if (v instanceof MaterialButton) {
            MaterialButton b = (MaterialButton) v;
            if (PENDING_TEXT.contentEquals(b.getText())) b.setText(s.text);
            b.setIcon(s.icon);
            if (s.iconTint != null) b.setIconTint(s.iconTint);
        } else if (v instanceof TextView && !(v instanceof android.widget.EditText)) {
            TextView t = (TextView) v;
            if (PENDING_TEXT.contentEquals(t.getText())) t.setText(s.text);
            t.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
        }
        v.setAlpha(s.alpha);
        v.setEnabled(s.enabled);
    }
}
