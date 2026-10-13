package com.harvana.app.wallet;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Popular tab: the networks most people add, each with its chain ID and coin. Add fetches it from the chain list, finds an RPC that answers, then asks. */
public class PopularNetworksFragment extends Fragment {

    private static final String LOGO = "https://icons.llamao.fi/icons/chains/rsz_";

    /** name, chain ID, coin, logo name. */
    static final String[][] POPULAR = {
        {"Ethereum", "1", "ETH", "ethereum"},
        {"Arbitrum One", "42161", "ETH", "arbitrum"},
        {"Avalanche C-Chain", "43114", "AVAX", "avalanche"},
        {"BNB Smart Chain", "56", "BNB", "binance"},
        {"Base", "8453", "ETH", "base"},
        {"Linea", "59144", "ETH", "linea"},
        {"OP Mainnet", "10", "ETH", "optimism"},
        {"Polygon", "137", "POL", "polygon"},
        {"zkSync Era", "324", "ETH", "zksync era"},
        {"Celo", "42220", "CELO", "celo"},
        {"Gnosis", "100", "XDAI", "gnosis"},
        {"Scroll", "534352", "ETH", "scroll"},
        {"Mantle", "5000", "MNT", "mantle"},
        {"Blast", "81457", "ETH", "blast"},
        {"Core", "1116", "CORE", "core"},
        {"Cronos", "25", "CRO", "cronos"},
        {"Fantom", "250", "FTM", "fantom"},
        {"Moonbeam", "1284", "GLMR", "moonbeam"},
        {"Kava", "2222", "KAVA", "kava"},
    };

    private final List<String[]> shown = new ArrayList<>();
    private final java.util.Set<Long> busy = new java.util.HashSet<>();
    private RecyclerView list;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_networks_popular, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle state) {
        list = v.findViewById(R.id.rv_networks_popular);
        list.setAdapter(new Adapter());
        EditText search = v.findViewById(R.id.et_networks_search);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { filter(s.toString()); }
        });
        filter("");
    }

    @Override public void onResume() { super.onResume(); if (list != null && list.getAdapter() != null) list.getAdapter().notifyDataSetChanged(); }

    private void filter(String q) {
        String f = q.trim().toLowerCase(Locale.US);
        shown.clear();
        for (String[] n : POPULAR) if (f.isEmpty() || n[0].toLowerCase(Locale.US).contains(f) || n[1].equals(f) || n[2].toLowerCase(Locale.US).equals(f)) shown.add(n);
        if (list != null && list.getAdapter() != null) list.getAdapter().notifyDataSetChanged();
    }

    /** True when the wallet already has this chain: one of its own (Ethereum, BNB, Polygon, Base) or one added before. */
    private static boolean have(long chainId) {
        for (String k : Web3Signer.CHAINS) if (Net.evm(k).chainId == chainId) return true;
        return CustomAssets.find("C:" + chainId) != null;
    }

    private void add(String[] n, Runnable redraw) {
        long id = Long.parseLong(n[1]);
        busy.add(id);
        redraw.run();
        CustomAssets.fetchChain(id, (a, err) -> {
            busy.remove(id);
            redraw.run();
            if (!isAdded()) return;
            if (a == null) { Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show(); return; }
            if (a.icon.isEmpty()) a.icon = LOGO + n[3] + ".jpg";
            NetworkSheets.approve(requireActivity(), a, null, true, () -> {
                CustomAssets.addNetwork(requireContext(), a);
                if (getActivity() instanceof NetworksActivity) ((NetworksActivity) getActivity()).networkAdded();
                Toast.makeText(requireContext(), a.chainName + " added", Toast.LENGTH_SHORT).show();
                redraw.run();
            }, null);
        });
    }

    private final class Adapter extends RecyclerView.Adapter<Adapter.H> {
        @NonNull @Override public H onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new H(getLayoutInflater().inflate(R.layout.item_network, p, false));
        }

        @Override public void onBindViewHolder(@NonNull H h, int i) {
            String[] n = shown.get(i);
            long id = Long.parseLong(n[1]);
            h.name.setText(n[0]);
            h.chain.setText(n[1]);
            h.symbol.setText(n[2]);
            NetworkSheets.icon(h.icon, "C:" + id, LOGO + n[3] + ".jpg");
            boolean have = have(id);
            h.added.setVisibility(have ? View.VISIBLE : View.GONE);
            h.add.setVisibility(have ? View.GONE : View.VISIBLE);
            h.add.setEnabled(!busy.contains(id));
            h.add.setText(busy.contains(id) ? "…" : "Add");
            h.add.setOnClickListener(v -> add(n, () -> notifyDataSetChanged()));
        }

        @Override public int getItemCount() { return shown.size(); }

        final class H extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView name, chain, symbol, added;
            final MaterialButton add;
            H(View v) {
                super(v);
                icon = v.findViewById(R.id.iv_network_icon);
                name = v.findViewById(R.id.tv_network_name);
                chain = v.findViewById(R.id.tv_network_chain_id);
                symbol = v.findViewById(R.id.tv_network_symbol);
                added = v.findViewById(R.id.tv_network_added);
                add = v.findViewById(R.id.btn_network_add);
            }
        }
    }
}
