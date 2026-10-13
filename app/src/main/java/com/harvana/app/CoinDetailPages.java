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
            String priceKey = st != null ? null : coin == null || coin.nativeHarv ? "HARV" : coin.key;
            if (priceKey != null && com.harvana.app.wallet.PriceChart.supports(priceKey)) {
                body.addView(card(com.harvana.app.wallet.PriceChart.create(requireContext(), priceKey)));
            }
            body.addView(label("WALLET ADDRESS"));
            TextView addr = new TextView(requireContext());
            addr.setText(address().isEmpty() ? "Address not ready yet" : address());
            addr.setTextIsSelectable(true);
            addr.setTextSize(14);
            addr.setTextColor(com.harvana.app.utils.UiColors.textSecondary());
            addr.setTypeface(android.graphics.Typeface.MONOSPACE);
            body.addView(card(addr));
            if (st != null || coin == null || coin.nativeHarv) basicInfo(body, st);
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
            moreInfo(body, coin, st);
        }

        /** HARV's most there will ever be: 1.25 HARV a block, halving every 23,260,000 blocks (node: ConsensusMath). */
        private static final double HARV_MAX_SUPPLY = 58_149_996.9762;

        /**
         * Basic information (owner, 13 Oct 2026): price, market cap and supply from the node, then the token's own
         * identity on Harvana (a native token, no contract: the token ID is what a transfer names).
         */
        private void basicInfo(LinearLayout body, String st) {
            body.addView(label("BASIC INFORMATION"));
            LinearLayout box = new LinearLayout(requireContext());
            box.setOrientation(LinearLayout.VERTICAL);
            TextView price = valueOf(addStat(box, "Price", "…"));
            TextView cap = valueOf(addStat(box, "Market cap", "…"));
            if (st == null) {
                TextView circ = valueOf(addStat(box, "Circulating supply", "…"));
                addStat(box, "Maximum supply", compact(HARV_MAX_SUPPLY) + " HARV");
                TextView holders = valueOf(addStat(box, "Holders", "…"));
                TextView txs = valueOf(addStat(box, "Transactions", "…"));
                body.addView(card(box));
                final double[] supply = {Double.NaN}, usd = {Double.NaN};
                Runnable paint = () -> {
                    price.setText(Double.isNaN(usd[0]) ? "—" : com.harvana.app.wallet.FxRates.money(requireContext(), usd[0]));
                    cap.setText(Double.isNaN(usd[0]) || Double.isNaN(supply[0]) ? "—" : com.harvana.app.wallet.FxRates.money(requireContext(), usd[0] * supply[0]));
                };
                com.harvana.app.sync.HarvanaNodeClient.nodeGet("/api/v1/supply", new com.harvana.app.sync.HarvanaNodeClient.JsonCallback() {
                    @Override public void onSuccess(org.json.JSONObject j) {
                        if (!isAdded()) return;
                        supply[0] = j.optLong("circulatingSupplyRaw") / 1e8;
                        circ.setText(compact(supply[0]) + " HARV");
                        holders.setText(String.format(Locale.US, "%,d", j.optLong("holderCount")));
                        txs.setText(String.format(Locale.US, "%,d", j.optLong("totalTransactions")));
                        paint.run();
                    }
                    @Override public void onError(String e) { if (isAdded()) { circ.setText("—"); holders.setText("—"); txs.setText("—"); } }
                });
                com.harvana.app.wallet.HarvPrice.get(q -> {
                    if (!isAdded()) return;
                    com.harvana.app.wallet.FxRates.load(requireContext(), () -> {
                        if (!isAdded()) return;
                        usd[0] = com.harvana.app.wallet.HarvPrice.usd(requireContext(), q);
                        paint.run();
                    });
                });
                body.addView(label("TOKEN"));
                LinearLayout tok = new LinearLayout(requireContext());
                tok.setOrientation(LinearLayout.VERTICAL);
                addStat(tok, "Type", "Native coin");
                addStat(tok, "Token ID", "HARV");
                addStat(tok, "Contract", "None (native)");
                addStat(tok, "Decimals", "8");
                addStat(tok, "Network", com.harvana.app.wallet.CoinCatalog.harv().liveNetwork());
                body.addView(card(tok));
                return;
            }
            String sym = com.harvana.app.husd.Stable.symbol(st);
            TextView supply = valueOf(addStat(box, "Total supply", "…"));
            TextView backing = valueOf(addStat(box, "Backing required", "…"));
            TextView pending = valueOf(addStat(box, "Pending withdrawals", "…"));
            TextView burned = valueOf(addStat(box, "Burned", "…"));
            body.addView(card(box));
            body.addView(label("TOKEN"));
            LinearLayout tok = new LinearLayout(requireContext());
            tok.setOrientation(LinearLayout.VERTICAL);
            addStat(tok, "Type", "Harvana stable coin");
            addStat(tok, "Token ID", st);
            addStat(tok, "On Harvana", "Native token");
            addStat(tok, "Decimals", "6");
            addStat(tok, "Backed by", com.harvana.app.husd.Stable.reserve(st, null));
            LinearLayout bridgeRow = addStat(tok, "Bridge address", "…");
            LinearLayout burnRow = addStat(tok, "Burn address", "…");
            LinearLayout treasuryRow = addStat(tok, "Treasury", "…");
            body.addView(card(tok));
            // The coin's ERC-20 contract on each EVM chain where it exists (bridge/contracts/HarvanaToken.sol).
            body.addView(label("CONTRACT ADDRESSES"));
            LinearLayout contracts = new LinearLayout(requireContext());
            contracts.setOrientation(LinearLayout.VERTICAL);
            addStat(contracts, "Other chains", "…");
            body.addView(card(contracts));
            com.google.android.material.button.MaterialButton toChain = new com.google.android.material.button.MaterialButton(
                    requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            toChain.setText("Send to another chain");
            toChain.setAllCaps(false);
            toChain.setVisibility(View.GONE);
            LinearLayout.LayoutParams tcLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tcLp.topMargin = dp(10);
            body.addView(toChain, tcLp);
            com.harvana.app.wallet.FxRates.load(requireContext(), () -> {
                if (!isAdded()) return;
                double per = com.harvana.app.wallet.FxRates.perUsd(requireContext(), com.harvana.app.husd.Stable.currency(st));
                double usd = Double.isNaN(per) || per <= 0 ? Double.NaN : 1d / per;
                price.setText(Double.isNaN(usd) ? "—" : com.harvana.app.wallet.FxRates.money(requireContext(), usd));
                price.setTag(usd);
                paintCap(cap, supply);
            });
            com.harvana.app.husd.HusdClient.info(st, new com.harvana.app.husd.HusdClient.Cb() {
                @Override public void ok(org.json.JSONObject j) {
                    if (!isAdded()) return;
                    double units = j.optLong("supplyRaw") / 1e6;
                    supply.setText(compact(units) + " " + sym);
                    supply.setTag(units);
                    backing.setText(compact(j.optLong("backingRequiredRaw") / 1e6) + " " + sym);
                    pending.setText(compact(j.optLong("pendingWithdrawalsRaw") / 1e6) + " " + sym);
                    burned.setText(compact(j.optLong("burnedRaw") / 1e6) + " " + sym);
                    copyable(bridgeRow, j.optString("bridgeAddress"));
                    org.json.JSONObject tokens = j.optJSONObject("tokens");
                    contracts.removeAllViews();
                    java.util.List<String> chains = new java.util.ArrayList<>();
                    if (tokens != null) for (java.util.Iterator<String> it = tokens.keys(); it.hasNext(); ) {
                        String chain = it.next();
                        chains.add(chain);
                        copyable(addStat(contracts, chainName(chain), "…"), tokens.optString(chain));
                    }
                    if (chains.isEmpty()) addStat(contracts, "Other chains", "Not deployed yet");
                    else {
                        toChain.setVisibility(View.VISIBLE);
                        toChain.setOnClickListener(v -> sendToChain(st, chains, j));
                    }
                    copyable(burnRow, j.optString("burnAddress"));
                    copyable(treasuryRow, j.optString("treasuryAddress"));
                    price.post(() -> paintCap(cap, supply));
                }
                @Override public void fail(String e) {
                    if (!isAdded()) return;
                    supply.setText("—"); backing.setText("—"); pending.setText("—"); burned.setText("—");
                    valueOf(bridgeRow).setText("—"); valueOf(burnRow).setText("—"); valueOf(treasuryRow).setText("—");
                }
            });
            cap.setTag(price);
        }

        /** "BASE" → "Base", "BSC" → "BNB Chain", … */
        private static String chainName(String chain) {
            switch (chain) {
                case "BASE": return "Base";
                case "BSC": return "BNB Chain";
                case "ETHEREUM": return "Ethereum";
                case "POLYGON": return "Polygon";
                case "ARBITRUM": return "Arbitrum";
                case "CELO": return "Celo";
                default: return chain;
            }
        }

        /**
         * Sends the coin to its ERC-20 on another chain: a signed transfer into the coin's export wallet with memo
         * "<CODE>_EXPORT:<chain>:<0x address>". The bridge signers then mint the same amount there for that address
         * (this wallet's own EVM address by default). Review, then PIN or fingerprint.
         */
        private void sendToChain(String code, java.util.List<String> chains, org.json.JSONObject info) {
            if (getContext() == null) return;
            String sym = com.harvana.app.husd.Stable.symbol(code);
            LinearLayout box = new LinearLayout(requireContext());
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(20), dp(8), dp(20), 0);
            final String[] chain = {chains.get(0)};
            com.google.android.material.chip.ChipGroup g = new com.google.android.material.chip.ChipGroup(requireContext());
            g.setSingleSelection(true);
            for (String c : chains) {
                com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(requireContext());
                chip.setText(chainName(c));
                chip.setCheckable(true);
                chip.setChecked(c.equals(chain[0]));
                chip.setOnClickListener(v -> chain[0] = c);
                g.addView(chip);
            }
            box.addView(g);
            android.widget.EditText amount = new com.google.android.material.textfield.TextInputEditText(requireContext());
            amount.setHint("Amount (" + sym + ")");
            amount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            box.addView(amount);
            android.widget.EditText to = new com.google.android.material.textfield.TextInputEditText(requireContext());
            to.setHint("0x… address on that chain");
            to.setSingleLine(true);
            box.addView(to);
            new Thread(() -> {                                             // this wallet's own EVM address, as the default
                try {
                    String mine = com.harvana.app.wallet.Web3Signer.address(requireContext().getApplicationContext());
                    to.post(() -> { if (to.getText().length() == 0 && mine != null) to.setText(mine); });
                } catch (Exception ignored) { }
            }).start();
            long minRaw = info.optLong("minExportRaw", 0);
            org.json.JSONObject fees = info.optJSONObject("fees");
            org.json.JSONObject bridgeFees = fees == null ? null : fees.optJSONObject("bridgeFeeRaw");
            new com.harvana.app.ui.SheetDialog.Builder(requireContext()).setTitle("Send to another chain").setView(box)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Continue", (d, w) -> {
                        long raw = com.harvana.app.group.NodePayActions.coinRaw(amount.getText().toString());
                        String evm = to.getText().toString().trim();
                        if (raw <= 0 || raw < minRaw) {
                            android.widget.Toast.makeText(requireContext(), "Minimum " + compact(minRaw / 1e6) + " " + sym, android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (!evm.matches("^0x[0-9a-fA-F]{40}$")) {
                            android.widget.Toast.makeText(requireContext(), "Enter a 0x address", android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }
                        long fee = bridgeFees == null ? 0 : bridgeFees.optLong(chain[0], 0);
                        java.util.List<String[]> rows = new java.util.ArrayList<>();
                        rows.add(new String[]{"Network", chainName(chain[0])});
                        rows.add(new String[]{"To", evm});
                        rows.add(new String[]{"Contract", info.optJSONObject("tokens") == null ? "—" : info.optJSONObject("tokens").optString(chain[0])});
                        rows.add(new String[]{"Bridge fee", compact(fee / 1e6) + " " + sym});
                        rows.add(new String[]{"tFare", "HARV"});
                        String from = address();
                        String memo = code + "_EXPORT:" + chain[0] + ":" + evm;
                        com.harvana.app.group.PaymentConfirmSheet.reviewThenAuthorize(requireContext(), "Send to " + chainName(chain[0]),
                                compact(raw / 1e6) + " " + sym, rows, () -> com.harvana.app.husd.HusdClient.send(requireContext(), code, from,
                                        info.optString("exportAddress"), raw, memo, new com.harvana.app.husd.HusdClient.Cb() {
                                    @Override public void ok(org.json.JSONObject r) {
                                        if (isAdded()) android.widget.Toast.makeText(requireContext(), "Sent. It arrives on " + chainName(chain[0]) + " once the bridge mints it.", android.widget.Toast.LENGTH_LONG).show();
                                    }
                                    @Override public void fail(String e) {
                                        if (isAdded()) android.widget.Toast.makeText(requireContext(), com.harvana.app.utils.ErrorText.plain(e), android.widget.Toast.LENGTH_LONG).show();
                                    }
                                }));
                    }).show();
        }

        /** Market cap of a stable coin: its dollar price (kept on the price row) × its supply. */
        private void paintCap(TextView cap, TextView supply) {
            if (!isAdded() || !(cap.getTag() instanceof TextView)) return;
            Object p = ((TextView) cap.getTag()).getTag(), s0 = supply.getTag();
            if (!(p instanceof Double) || !(s0 instanceof Double) || ((Double) p).isNaN()) { cap.setText("—"); return; }
            cap.setText(com.harvana.app.wallet.FxRates.money(requireContext(), (Double) p * (Double) s0));
        }

        /** An address row: shortened, tap to copy. */
        private void copyable(LinearLayout row, String address) {
            TextView v = valueOf(row);
            if (address == null || address.isEmpty()) { v.setText("—"); return; }
            v.setText(shortAddr(address));
            // The icon is sized before it is attached, so the row keeps the text's height and the value lines up with its label.
            android.graphics.drawable.Drawable icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_copy_line);
            if (icon != null) {
                icon = icon.mutate();
                icon.setBounds(0, 0, dp(16), dp(16));
                icon.setTint(com.harvana.app.utils.UiColors.textSecondary());
            }
            v.setCompoundDrawablesRelative(null, null, icon, null);
            v.setCompoundDrawablePadding(dp(6));
            v.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            row.setOnClickListener(x -> {
                android.content.ClipboardManager cm = (android.content.ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                if (cm != null) cm.setPrimaryClip(android.content.ClipData.newPlainText("Address", address));
                android.widget.Toast.makeText(requireContext(), "Copied", android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        /** More info: the coin's links, opened in the app's browser (the explorer in the explorer). */
        private void moreInfo(LinearLayout body, CoinCatalog.Coin coin, String st) {
            java.util.List<String[]> links = new java.util.ArrayList<>();
            boolean harvana = st != null || coin == null || coin.nativeHarv;
            links.add(new String[]{"Explorer", ""});                         // this coin's explorer, for this wallet
            if (harvana) {
                links.add(new String[]{"Official website", "https://harvannaprotocol.com/"});
                links.add(new String[]{"Harvana Swap", com.harvana.app.web3.Web3Home.SWAP_URL});
            }
            // Bridge deposits from another chain: shown only once a chain takes them (Receive is the Harvana address only).
            com.harvana.app.husd.StableActions sa = getActivity() instanceof CoinDetailActivity ? ((CoinDetailActivity) getActivity()).stable() : null;
            if (st != null && sa != null && sa.depositsOpen()) links.add(new String[]{"Deposit " + sa.reserve(), "deposit:"});
            if (links.isEmpty()) return;
            body.addView(label("MORE INFO"));
            com.google.android.material.chip.ChipGroup g = new com.google.android.material.chip.ChipGroup(requireContext());
            for (String[] l : links) {
                com.google.android.material.chip.Chip c = new com.google.android.material.chip.Chip(requireContext());
                c.setText(l[0]);
                c.setChipIconResource(R.drawable.icon_link_twotone_line);
                c.setChipIconTint(android.content.res.ColorStateList.valueOf(com.harvana.app.utils.UiColors.accent()));
                c.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(com.harvana.app.utils.UiColors.surfaceAlt()));
                c.setTextColor(com.harvana.app.utils.UiColors.textPrimary());
                c.setOnClickListener(v -> {
                    if (l[1].isEmpty()) { if (getActivity() instanceof CoinDetailActivity) ((CoinDetailActivity) getActivity()).openExplorer(); }
                    else if ("deposit:".equals(l[1])) { if (sa != null) sa.deposit(); }
                    else com.harvana.app.web3.Web3Home.open(requireContext(), l[1]);
                });
                g.addView(c);
            }
            body.addView(g);
        }

        private LinearLayout addStat(LinearLayout box, String name, String value) {
            LinearLayout row = stat(name, value);
            box.addView(row);
            return row;
        }

        /** 1,234,567.8 → "1.23M"; small figures in full. */
        private static String compact(double v) {
            if (Double.isNaN(v)) return "—";
            double a = Math.abs(v);
            if (a >= 1e9) return String.format(Locale.US, "%.2fB", v / 1e9);
            if (a >= 1e6) return String.format(Locale.US, "%.2fM", v / 1e6);
            if (a >= 1e3) return String.format(Locale.US, "%,.2f", v);
            return String.format(Locale.US, "%.4f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
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
