package com.harvanna.wallet.p2p;

import java.util.ArrayList;
import java.util.List;

/**
 * GROUND-UP (Sept 17 2026) — the marketplace's currency picker previously
 * only updated its own local label with no way to reach the Buy/Sell
 * fragments living inside the same host's ViewPager/fragment-swap. This
 * is the simplest thing that actually closes that gap: a tiny static
 * broadcaster, matching this codebase's existing pattern of static
 * utility holders rather than introducing a new architecture (ViewModel/
 * LiveData) used nowhere else in the app.
 *
 * Not persisted across process death by design — a currency filter is
 * session-scoped UI state, not something that needs surviving an app
 * restart.
 */
public final class P2pCurrencyFilterState {

    public interface Listener { void onCurrencyChanged(String currencyCode); }

    private static String currentCurrency = "NGN";
    private static final List<Listener> listeners = new ArrayList<>();

    private P2pCurrencyFilterState() { }

    public static String getCurrency() {
        return currentCurrency;
    }

    public static void setCurrency(String code) {
        if (code == null || code.trim().isEmpty()) return;
        currentCurrency = code.trim().toUpperCase();
        // Copy before iterating — a listener's own callback may
        // register/unregister (e.g. a fragment tearing down mid-notify),
        // and iterating the live list directly would risk a
        // ConcurrentModificationException in that case.
        for (Listener l : new ArrayList<>(listeners)) l.onCurrencyChanged(currentCurrency);
    }

    public static void addListener(Listener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public static void removeListener(Listener l) {
        listeners.remove(l);
    }
}
