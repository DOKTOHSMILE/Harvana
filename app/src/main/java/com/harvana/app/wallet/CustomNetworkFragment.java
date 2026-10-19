package com.harvana.app.wallet;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.harvana.app.R;

import java.util.Locale;

/**
 * Custom networks tab (MetaMask's form): Network name, RPC URL, Chain ID (decimal or 0x hex), Symbol, Block explorer URL.
 * The RPC is asked for its chain ID as it is typed: an empty Chain ID is filled from it, a different one is an error and Add
 * stays off. The chain list is read for the chain ID: it fills an empty name, and a symbol that differs gets a warning.
 */
public class CustomNetworkFragment extends Fragment {

    private EditText name, rpc, chainId, symbol, explorer;
    private TextView rpcError, chainError, symbolWarning;
    private MaterialButton add;
    private final Handler ui = new Handler(Looper.getMainLooper());
    /** What the RPC answered with (-1 = not asked yet or no answer) and for which URL. */
    private volatile long rpcAnswer = -1;
    private String rpcAsked = "";
    /** The chain list's entry for the typed chain ID, or null. */
    private CustomAssets.Asset listed;
    private long listedFor = -1;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_network_custom, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle state) {
        name = v.findViewById(R.id.et_network_name);
        rpc = v.findViewById(R.id.et_network_rpc);
        chainId = v.findViewById(R.id.et_network_chain_id);
        symbol = v.findViewById(R.id.et_network_symbol);
        explorer = v.findViewById(R.id.et_network_explorer);
        rpcError = v.findViewById(R.id.tv_network_rpc_error);
        chainError = v.findViewById(R.id.tv_network_chain_error);
        symbolWarning = v.findViewById(R.id.tv_network_symbol_warning);
        add = v.findViewById(R.id.btn_network_custom_add);

        rpc.addTextChangedListener(watch(() -> { ui.removeCallbacks(askRpc); ui.postDelayed(askRpc, 600); check(); }));
        chainId.addTextChangedListener(watch(() -> { ui.removeCallbacks(askList); ui.postDelayed(askList, 600); check(); }));
        symbol.addTextChangedListener(watch(this::checkSymbol));
        add.setOnClickListener(x -> review());
    }

    @Override public void onDestroyView() { ui.removeCallbacksAndMessages(null); super.onDestroyView(); }

    // ── checks ────────────────────────────────────────────────────────────────

    private final Runnable askRpc = () -> {
        String url = rpcUrl();
        rpcAnswer = -1;
        rpcAsked = url;
        if (url.isEmpty()) { show(rpcError, null); check(); return; }
        if (!url.startsWith("https://")) { show(rpcError, "The RPC URL must start with https://"); check(); return; }
        show(rpcError, null);
        new Thread(() -> {
            long got;
            try { got = CustomAssets.rpcChainId(url); } catch (Exception e) { got = -2; }
            final long answer = got;
            ui.post(() -> {
                if (!isAdded() || !url.equals(rpcUrl())) return;
                rpcAnswer = answer;
                if (answer < 0) show(rpcError, "Could not fetch the chain ID. Is the RPC URL correct?");
                else if (typedChainId() < 0) chainId.setText(String.valueOf(answer));
                check();
            });
        }).start();
    };

    private final Runnable askList = () -> {
        long id = typedChainId();
        if (id <= 0 || id == listedFor) { checkSymbol(); return; }
        listedFor = id;
        listed = null;
        CustomAssets.registry(id, (a, err) -> {
            if (!isAdded() || typedChainId() != id) return;
            listed = a;
            if (a != null) {
                name.setHint(a.chainName);
                symbol.setHint(a.symbol);
                if (explorer.getText().toString().trim().isEmpty() && !a.explorer.isEmpty()) explorer.setHint(a.explorer);
            }
            checkSymbol();
        });
    };

    /** Add is on only when the RPC answered with the chain ID typed (MetaMask's rule). */
    private void check() {
        long typed = typedChainId();
        String typedText = chainId.getText().toString().trim();
        if (!typedText.isEmpty() && typed <= 0) show(chainError, "Enter a valid chain ID (a number, or 0x hex)");
        else if (typed > 0 && rpcAnswer > 0 && rpcAsked.equals(rpcUrl()) && rpcAnswer != typed)
            show(chainError, "The RPC URL answered with a different chain ID (" + rpcAnswer + ")");
        else show(chainError, null);
        add.setEnabled(typed > 0 && rpcAnswer == typed && rpcAsked.equals(rpcUrl()));
    }

    private void checkSymbol() {
        String s = symbol.getText().toString().trim();
        if (listed != null && !s.isEmpty() && !listed.symbol.isEmpty() && !s.equalsIgnoreCase(listed.symbol))
            show(symbolWarning, "Chain ID " + listed.chainId + " may use a different currency symbol (" + listed.symbol + ")");
        else show(symbolWarning, null);
    }

    // ── add ───────────────────────────────────────────────────────────────────

    private void review() {
        long id = typedChainId();
        if (id <= 0 || rpcAnswer != id) return;
        if (CustomAssets.find("C:" + id) != null || builtIn(id)) {
            Toast.makeText(requireContext(), "A network with chain ID " + id + " is already in the wallet", Toast.LENGTH_LONG).show();
            return;
        }
        CustomAssets.Asset a = new CustomAssets.Asset();
        a.kind = "CHAIN";
        a.family = "EVM";
        a.chainId = id;
        String n = name.getText().toString().trim();
        a.chainName = !n.isEmpty() ? n : listed != null && listed.chainId == id ? listed.chainName : "Chain " + id;
        a.name = a.chainName;
        String s = symbol.getText().toString().trim().toUpperCase(Locale.US);
        a.symbol = !s.isEmpty() ? s : listed != null && listed.chainId == id && !listed.symbol.isEmpty() ? listed.symbol : "ETH";
        a.decimals = listed != null && listed.chainId == id ? listed.decimals : 18;
        a.rpc = rpcUrl();
        String ex = explorer.getText().toString().trim();
        if (ex.isEmpty() && listed != null && listed.chainId == id) ex = listed.explorer;
        while (ex.endsWith("/")) ex = ex.substring(0, ex.length() - 1);
        a.explorer = ex;
        String rawIc = listed != null && listed.chainId == id ? listed.icon : "";
        a.icon = (rawIc != null && !rawIc.isEmpty()) ? rawIc : MultiCoinEngine.resolveChainIcon(id, "");
        boolean known = listed != null && listed.chainId == id;
        NetworkSheets.approve(requireActivity(), a, null, known, () -> {
            CustomAssets.addNetwork(requireContext(), a);
            if (getActivity() instanceof NetworksActivity) ((NetworksActivity) getActivity()).networkAdded();
            Toast.makeText(requireContext(), a.chainName + " added", Toast.LENGTH_SHORT).show();
            requireActivity().finish();
        }, null);
    }

    private static boolean builtIn(long id) {
        for (String k : Web3Signer.CHAINS) if (Net.evm(k).chainId == id) return true;
        return false;
    }

    // ── small helpers ─────────────────────────────────────────────────────────

    private String rpcUrl() { return rpc.getText().toString().trim(); }

    /** The typed chain ID: decimal or 0x hex; -1 when empty or not a number. */
    private long typedChainId() {
        String t = chainId.getText().toString().trim().toLowerCase(Locale.US);
        try {
            if (t.startsWith("0x")) return Long.parseLong(t.substring(2), 16);
            return t.isEmpty() ? -1 : Long.parseLong(t);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void show(TextView t, String msg) {
        t.setText(msg == null ? "" : msg);
        t.setVisibility(msg == null ? View.GONE : View.VISIBLE);
    }

    private static TextWatcher watch(Runnable r) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { r.run(); }
        };
    }
}
