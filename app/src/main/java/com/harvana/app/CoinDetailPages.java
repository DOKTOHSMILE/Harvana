package com.harvana.app;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.harvana.app.wallet.CoinCatalog;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/** Overview, history and network as three pages you can swipe. Each page reads the coin this screen was opened for. */
public final class CoinDetailPages {
    private CoinDetailPages() {}

    public static ViewPager2 install(CoinDetailActivity activity) {
        View scroll = activity.findViewById(R.id.vscroll2);
        if (scroll == null || !(scroll.getParent() instanceof LinearLayout)) return null;
        LinearLayout column = (LinearLayout) scroll.getParent();
        if (scroll instanceof androidx.core.widget.NestedScrollView) ((androidx.core.widget.NestedScrollView) scroll).setFillViewport(false);
        LinearLayout.LayoutParams head = (LinearLayout.LayoutParams) scroll.getLayoutParams();
        head.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        head.weight = 0;
        scroll.setLayoutParams(head);

        float d = activity.getResources().getDisplayMetrics().density;
        ViewPager2 pager = new ViewPager2(activity);
        pager.setId(View.generateViewId());
        pager.setOffscreenPageLimit(2);
        pager.setAdapter(new FragmentStateAdapter(activity) {
            @Override public int getItemCount() { return 3; }
            @NonNull @Override public Fragment createFragment(int position) { return Page.newInstance(position); }
        });

        com.google.android.material.tabs.TabLayout tabs =
                (com.google.android.material.tabs.TabLayout) activity.getLayoutInflater().inflate(R.layout.view_toggle_tabs, column, false);
        com.harvana.app.utils.Toggles.pill(tabs);
        String[] names = {"Overview", "History", "Network"};
        new com.google.android.material.tabs.TabLayoutMediator(tabs, pager, (tab, position) -> tab.setText(names[position])).attach();

        LinearLayout host = new LinearLayout(activity);
        host.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        barLp.topMargin = (int) (8 * d);
        barLp.leftMargin = (int) (24 * d);
        barLp.rightMargin = (int) (24 * d);
        host.addView(tabs, barLp);
        host.addView(pager, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        int at = column.indexOfChild(scroll) + 1;
        column.addView(host, at, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return pager;
    }

    public static final class Page extends Fragment {
        static Page newInstance(int position) {
            Page p = new Page();
            Bundle b = new Bundle();
            b.putInt("page", position);
            p.setArguments(b);
            return p;
        }

        private int page;
        private LinearLayout historyBox;
        private TextView historyEmpty;

        @Nullable @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            page = getArguments() == null ? 0 : getArguments().getInt("page");
            if (page == 1 && harv()) {
                FrameLayout host = new FrameLayout(requireContext());
                host.setId(View.generateViewId());
                return host;
            }
            NestedScrollView scroll = new NestedScrollView(requireContext());
            scroll.setFillViewport(true);
            LinearLayout body = new LinearLayout(requireContext());
            body.setOrientation(LinearLayout.VERTICAL);
            int p = dp(16);
            body.setPadding(p, p, p, p);
            scroll.addView(body, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            if (page == 0) fillOverview(body);
            else if (page == 1) fillHistory(body);
            else fillNetwork(body);
            return scroll;
        }

        @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            if (page == 1 && harv() && view instanceof FrameLayout && getChildFragmentManager().findFragmentById(view.getId()) == null) {
                WalletActivityFragmentActivity history = new WalletActivityFragmentActivity();
                Bundle only = new Bundle();
                only.putBoolean("harvOnly", true);
                String st = stable();
                only.putString("onlySymbol", st != null ? com.harvana.app.husd.Stable.symbol(st) : com.harvana.app.CoinInfo.ticker());
                history.setArguments(only);
                getChildFragmentManager().beginTransaction().replace(view.getId(), history).commitAllowingStateLoss();
            }
        }

        @Override public void onResume() {
            super.onResume();
            if (page == 1 && !harv()) loadHistory();
        }

        private boolean harv() {
            CoinCatalog.Coin coin = coin();
            return coin == null || coin.nativeHarv;
        }

        /** The stable coin this page shows ("HUSD", …), or null. */
        private String stable() {
            return getActivity() instanceof CoinDetailActivity ? ((CoinDetailActivity) getActivity()).stableCode() : null;
        }

        private CoinCatalog.Coin coin() {
            return getActivity() instanceof CoinDetailActivity ? ((CoinDetailActivity) getActivity()).openCoin() : null;
        }

        private String address() {
            if (!(getActivity() instanceof CoinDetailActivity)) return "";
            String a = ((CoinDetailActivity) getActivity()).shownAddress();
            return a == null ? "" : a;
        }

        private void fillOverview(LinearLayout body) {
            CoinCatalog.Coin coin = coin();
            String st = stable();
            String name = st != null ? com.harvana.app.husd.Stable.symbol(st) : coin == null ? "Harvana coin" : coin.name;
            body.addView(label("WALLET ADDRESS"));
            TextView addr = new TextView(requireContext());
            addr.setText(address().isEmpty() ? "Address not ready yet" : address());
            addr.setTextIsSelectable(true);
            addr.setTextSize(14);
            addr.setTextColor(com.harvana.app.utils.UiColors.textSecondary());
            addr.setTypeface(android.graphics.Typeface.MONOSPACE);
            body.addView(card(addr));
            body.addView(label("ABOUT " + name.toUpperCase(Locale.US)));
            TextView about = new TextView(requireContext());
            about.setText(st != null
                    ? com.harvana.app.husd.Stable.symbol(st) + " is the " + com.harvana.app.husd.Stable.name(st) + " on Harvana Protocol. 1 "
                      + com.harvana.app.husd.Stable.symbol(st) + " = 1 " + com.harvana.app.husd.Stable.name(st) + ", backed by "
                      + com.harvana.app.husd.Stable.reserve(st, null) + ". One " + com.harvana.app.husd.Stable.symbol(st) + " is 1,000,000 of the smallest unit."
                    : coin == null
                    ? "HARV is the native currency of Harvana Protocol, powering group savings, lending, and transfers across every Harvana group."
                    : coin.about);
            about.setTextSize(13);
            about.setTextColor(com.harvana.app.utils.UiColors.textSecondary());
            about.setLineSpacing(dp(3), 1f);
            body.addView(card(about));
        }

        private void fillHistory(LinearLayout body) {
            historyBox = new LinearLayout(requireContext());
            historyBox.setOrientation(LinearLayout.VERTICAL);
            body.addView(historyBox);
            historyEmpty = new TextView(requireContext());
            historyEmpty.setText("Loading…");
            historyEmpty.setTextColor(com.harvana.app.utils.UiColors.textSecondary());
            historyEmpty.setPadding(dp(8), dp(12), dp(8), dp(12));
            body.addView(historyEmpty);
        }

        private void loadHistory() {
            if (historyBox == null || getContext() == null) return;
            CoinCatalog.Coin coin = coin();
            if (coin == null) return;
            final String key = coin.key;
            final String harv = com.harvana.app.wallet.WalletManager.getActiveWalletAddress(getContext());
            new Thread(() -> {
                List<HashMap<String, Object>> rows = com.harvana.app.wallet.ChainHistory.one(getContext(), harv, key, address());
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> { if (isAdded()) paintHistory(coin, rows); });
            }).start();
        }

        private void paintHistory(CoinCatalog.Coin coin, List<HashMap<String, Object>> rows) {
            if (historyBox == null) return;
            historyBox.removeAllViews();
            if (rows == null || rows.isEmpty()) {
                historyEmpty.setText("No " + coin.ticker + " transactions yet.");
                historyEmpty.setVisibility(View.VISIBLE);
                return;
            }
            historyEmpty.setVisibility(View.GONE);
            LayoutInflater inf = LayoutInflater.from(requireContext());
            for (HashMap<String, Object> row : rows) {
                View item = inf.inflate(R.layout.item_transaction, historyBox, false);
                boolean in = Boolean.TRUE.equals(row.get("incoming"));
                TextView type = item.findViewById(R.id.tv_tx_type);
                TextView who = item.findViewById(R.id.tv_tx_address);
                TextView amount = item.findViewById(R.id.tv_tx_amount);
                TextView date = item.findViewById(R.id.tv_tx_date);
                android.widget.ImageView icon = item.findViewById(R.id.iv_tx_icon);
                if (type != null) type.setText((in ? "Received " : "Sent ") + coin.ticker);
                String other = String.valueOf(in ? row.get("senderAddress") : row.get("recipientAddress"));
                if (who != null) who.setText((in ? "From: " : "To: ") + shortAddr(other));
                if (amount != null) {
                    Object v = row.get("value");
                    amount.setText((in ? "+ " : "− ") + (v == null ? "0" : v) + " " + coin.ticker);
                    amount.setTextColor(in ? com.harvana.app.utils.UiColors.accent() : com.harvana.app.utils.UiColors.error());
                }
                if (date != null) date.setText(ago(row.get("timestamp")));
                if (icon != null) {
                    icon.setImageResource(in ? R.drawable.ic_action_receive : R.drawable.ic_action_send);
                    icon.setColorFilter(in ? com.harvana.app.utils.UiColors.accent() : com.harvana.app.utils.UiColors.error());
                }
                item.setOnClickListener(v -> {
                    android.content.Intent i = new android.content.Intent(requireContext(), ReceiptActivity.class);
                    i.putExtra("txHash", String.valueOf(row.get("txHash")));
                    i.putExtra("status", String.valueOf(row.get("status")));
                    i.putExtra("senderAddress", String.valueOf(row.get("senderAddress")));
                    i.putExtra("recipientAddress", String.valueOf(row.get("recipientAddress")));
                    i.putExtra("tokenSymbol", coin.ticker);
                    i.putExtra("assetKey", coin.key);
                    Object val = row.get("value");
                    i.putExtra("value", val instanceof Number ? ((Number) val).doubleValue() : 0);
                    Object ts = row.get("timestamp");
                    i.putExtra("timestamp", ts instanceof Number ? ((Number) ts).longValue() : System.currentTimeMillis());
                    i.putExtra("chain", String.valueOf(row.get("chain")));
                    i.putExtra("explorerUrl", String.valueOf(row.get("explorerUrl")));
                    i.putExtra("incoming", in);
                    Object fee = row.get("harvFee");
                    Object block = row.get("blockNumber");
                    Object conf = row.get("confirmations");
                    Object nonce = row.get("nonce");
                    if (fee instanceof Number) i.putExtra("harvFee", ((Number) fee).doubleValue());
                    if (block instanceof Number) i.putExtra("blockNumber", ((Number) block).longValue());
                    if (conf instanceof Number) i.putExtra("confirmations", ((Number) conf).longValue());
                    if (nonce instanceof Number) i.putExtra("nonce", ((Number) nonce).longValue());
                    startActivity(i);
                });
                historyBox.addView(item);
            }
        }

        private void fillNetwork(LinearLayout body) {
            CoinCatalog.Coin coin = coin();
            String st = stable();
            if (st != null) {                                  // a stable coin: the same rows the other coins show
                String sym = com.harvana.app.husd.Stable.symbol(st);
                LinearLayout box = new LinearLayout(requireContext());
                box.setOrientation(LinearLayout.VERTICAL);
                box.addView(stat("Network", com.harvana.app.wallet.CoinCatalog.harv().liveNetwork()));
                box.addView(stat("Standard", "Harvana stable coin"));
                LinearLayout priceRow = stat("Price", "…");
                if (getActivity() instanceof CoinDetailActivity) ((CoinDetailActivity) getActivity()).chainPriceLine = valueOf(priceRow);
                box.addView(priceRow);
                box.addView(stat("Value", "1 " + sym + " = 1 " + com.harvana.app.husd.Stable.name(st)));
                box.addView(stat("Backed by", com.harvana.app.husd.Stable.reserve(st, null)));
                box.addView(stat("Decimals", "6"));
                box.addView(stat("Smallest unit", "0.000001 " + sym));
                box.addView(stat("tFare paid in", "HARV or " + sym));
                body.addView(card(box));
                return;
            }
            if (coin == null || coin.nativeHarv) {
                View card = take(R.id.cardview8);
                View note = take(R.id.tv_coin_info_note);
                if (card != null) body.addView(card);
                if (note != null) body.addView(note);
                return;
            }
            LinearLayout box = new LinearLayout(requireContext());
            box.setOrientation(LinearLayout.VERTICAL);
            box.addView(stat("Network", coin.liveNetwork()));
            box.addView(stat("Standard", coin.standard));
            LinearLayout priceRow = stat("Price", "…");
            TextView price = valueOf(priceRow);
            if (getActivity() instanceof CoinDetailActivity) ((CoinDetailActivity) getActivity()).chainPriceLine = price;
            box.addView(priceRow);
            box.addView(stat("Decimals", String.valueOf(coin.decimals)));
            box.addView(stat("Smallest unit", coin.smallestUnit()));
            box.addView(stat("Fee paid in", "BASE".equals(coin.key) ? "ETH" : coin.ticker));
            TextView addr = new TextView(requireContext());
            addr.setText(address().isEmpty() ? "Address not ready yet" : address());
            addr.setTextIsSelectable(true);
            addr.setTypeface(android.graphics.Typeface.MONOSPACE);
            addr.setTextSize(13);
            addr.setTextColor(com.harvana.app.utils.UiColors.textPrimary());
            TextView cap = new TextView(requireContext());
            cap.setText("ADDRESS");
            cap.setTextSize(11);
            cap.setTypeface(null, android.graphics.Typeface.BOLD);
            cap.setTextColor(com.harvana.app.utils.UiColors.textTertiary());
            cap.setPadding(dp(4), dp(12), dp(4), dp(6));
            box.addView(cap);
            addr.setPadding(dp(4), 0, dp(4), dp(4));
            box.addView(addr);
            body.addView(card(box));
            if (getActivity() instanceof CoinDetailActivity) ((CoinDetailActivity) getActivity())._showMarketPrice(coin);
        }

        private View take(int id) {
            if (getActivity() == null) return null;
            View v = getActivity().findViewById(id);
            if (v == null) return null;
            if (v.getParent() instanceof ViewGroup) ((ViewGroup) v.getParent()).removeView(v);
            v.setVisibility(View.VISIBLE);
            return v;
        }

        private TextView label(String text) {
            TextView t = new TextView(requireContext());
            t.setText(text);
            t.setTextSize(11);
            t.setTypeface(null, android.graphics.Typeface.BOLD);
            t.setLetterSpacing(0.06f);
            t.setTextColor(com.harvana.app.utils.UiColors.textTertiary());
            t.setPadding(dp(4), dp(8), dp(4), dp(8));
            return t;
        }

        private View card(View child) {
            com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(requireContext());
            card.setCardBackgroundColor(com.harvana.app.utils.UiColors.surface());
            card.setRadius(dp(16));
            card.setStrokeWidth(dp(1));
            card.setStrokeColor(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.colorCardStroke)));
            card.setCardElevation(0);
            child.setPadding(dp(16), dp(16), dp(16), dp(16));
            card.addView(child);
            return card;
        }

        private LinearLayout stat(String name, String value) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(4), dp(8), dp(4), dp(8));
            TextView l = new TextView(requireContext());
            l.setText(name);
            l.setTextSize(14);
            l.setTextColor(com.harvana.app.utils.UiColors.textTertiary());
            TextView r = new TextView(requireContext());
            r.setText(value);
            r.setTextSize(14);
            r.setTypeface(null, android.graphics.Typeface.BOLD);
            r.setGravity(Gravity.END);
            r.setTextColor(com.harvana.app.utils.UiColors.textPrimary());
            row.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(r, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return row;
        }

        private TextView valueOf(LinearLayout row) {
            return (TextView) row.getChildAt(1);
        }

        private static String shortAddr(String a) {
            if (a == null || "null".equals(a) || a.length() < 14) return a == null || "null".equals(a) ? "" : a;
            return a.substring(0, 6) + "…" + a.substring(a.length() - 4);
        }

        private static String ago(Object ts) {
            long n = ts instanceof Number ? ((Number) ts).longValue() : 0L;
            if (n <= 0) return "";
            long s = Math.max(0, (System.currentTimeMillis() - n) / 1000);
            if (s < 60) return "Just now";
            if (s < 3600) return (s / 60) + " min ago";
            if (s < 86400) return (s / 3600) + " hours ago";
            return (s / 86400) + " days ago";
        }

        private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    }
}
