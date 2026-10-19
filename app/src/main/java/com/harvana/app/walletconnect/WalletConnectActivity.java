package com.harvana.app.walletconnect;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.harvana.app.R;
import com.harvana.app.utils.InfoTable;
import com.harvana.app.utils.UiColors;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

/** WalletConnect: pair with a dApp on another device or browser (link or QR), and see or end the connections. */
public class WalletConnectActivity extends AppCompatActivity {

    public static final String EXTRA_URI = "uri";
    private static final int RC_SCAN = 4110;

    private WalletConnect wc;
    private LinearLayout status, sessions;
    private TextView sessionsTitle;
    private final Runnable refresh = this::render;

    public static void open(Context c, String uri) {
        Intent i = new Intent(c, WalletConnectActivity.class);
        if (uri != null) i.putExtra(EXTRA_URI, uri);
        c.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        com.harvana.app.utils.AppBar.setContent(this, R.layout.activity_wallet_connect, "WalletConnect", null);
        wc = WalletConnect.get(this);
        status = findViewById(R.id.wc_status);
        sessions = findViewById(R.id.wc_sessions);
        sessionsTitle = findViewById(R.id.wc_sessions_title);
        TextInputEditText uri = findViewById(R.id.wc_uri);
        findViewById(R.id.wc_connect).setOnClickListener(v -> pair(uri.getText() == null ? "" : uri.getText().toString()));
        findViewById(R.id.wc_scan).setOnClickListener(v -> {
            Intent scan = new Intent(this, com.harvana.app.ScanQrCodeActivity.class);
            scan.putExtra(com.harvana.app.ScanQrCodeActivity.EXTRA_TITLE, "Scan a WalletConnect code");
            scan.putExtra(com.harvana.app.ScanQrCodeActivity.EXTRA_MODE, com.harvana.app.ScanQrCodeActivity.MODE_ANY);
            startActivityForResult(scan, RC_SCAN);
        });
        TextInputEditText pid = findViewById(R.id.wc_project_id);
        findViewById(R.id.wc_project_save).setOnClickListener(v -> {
            String id = pid.getText() == null ? "" : pid.getText().toString().trim();
            if (!id.matches("[0-9a-fA-F]{32}")) { pid.setError("32 letters and numbers"); return; }
            wc.setProjectId(id);
            render();
        });
        String incoming = linkOf(getIntent());
        if (incoming != null) pair(incoming);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String incoming = linkOf(intent);
        if (incoming != null) pair(incoming);
    }

    /** A wc: link from the extra or from the link the system opened this screen with. */
    private static String linkOf(Intent i) {
        if (i == null) return null;
        String u = i.getStringExtra(EXTRA_URI);
        if (u == null && i.getData() != null) u = i.getData().toString();
        if (u != null && u.startsWith("harvana://wc?uri=")) u = android.net.Uri.decode(u.substring("harvana://wc?uri=".length()));
        return u != null && u.startsWith("wc:") ? u : null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        wc.watch(refresh);
        if (wc.ready()) wc.start();
        render();
    }

    @Override
    protected void onPause() {
        wc.unwatch(refresh);
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SCAN && resultCode == RESULT_OK && data != null) {
            pair(data.getStringExtra(com.harvana.app.ScanQrCodeActivity.EXTRA_RESULT_TEXT));
        }
    }

    private void pair(String uri) {
        String err = wc.pair(uri);
        if (err != null) { Toast.makeText(this, err, Toast.LENGTH_LONG).show(); return; }
        TextInputEditText field = findViewById(R.id.wc_uri);
        field.setText("");
        Toast.makeText(this, "Waiting for the dApp…", Toast.LENGTH_SHORT).show();
    }

    private void render() {
        if (isFinishing()) return;
        boolean ready = wc.ready();
        findViewById(R.id.wc_project_box).setVisibility(ready ? View.GONE : View.VISIBLE);
        status.removeAllViews();
        InfoTable t = new InfoTable(this);
        t.row("Relay", !ready ? "Project ID needed" : wc.connected() ? "Connected" : "Connecting…",
                !ready ? UiColors.error() : wc.connected() ? UiColors.success() : UiColors.textSecondary());
        t.row("Connected dApps", String.valueOf(wc.sessions().size()));
        status.addView(t.view());

        sessions.removeAllViews();
        sessionsTitle.setText(wc.sessions().isEmpty() ? "" : "Connected");
        for (WalletConnect.Session s : wc.sessions()) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_wc_session, sessions, false);
            ((TextView) row.findViewById(R.id.wc_session_name)).setText(s.name());
            ((TextView) row.findViewById(R.id.wc_session_url)).setText(s.url());
            ((TextView) row.findViewById(R.id.wc_session_networks)).setText(networks(s.namespaces));
            ImageView icon = row.findViewById(R.id.wc_session_icon);
            if (!s.icon().isEmpty()) Glide.with(this).load(s.icon()).circleCrop().into(icon);
            else icon.setImageResource(R.drawable.icon_link_twotone_line);
            row.findViewById(R.id.wc_session_disconnect).setOnClickListener(v -> new com.harvana.app.ui.SheetDialog.Builder(this)
                    .setTitle("Disconnect " + s.name())
                    .setView(new InfoTable(this).row("Site", s.url()).view())
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Disconnect", (d, w) -> { wc.disconnect(s); render(); })
                    .show());
            sessions.addView(row);
        }
    }

    /** "Ethereum, Polygon, Solana" from approved namespaces. */
    private static String networks(JSONObject ns) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<String> it = ns.keys(); it.hasNext(); ) {
            String space = it.next();
            JSONArray chains = ns.optJSONObject(space) == null ? null : ns.optJSONObject(space).optJSONArray("chains");
            for (int i = 0; chains != null && i < chains.length(); i++) {
                String c = chains.optString(i);
                String name;
                if ("solana".equals(space)) name = "Solana";
                else {
                    String key = WcPrompts.evmKey(WcPrompts.chainNumber(c));
                    name = key == null ? c : com.harvana.app.wallet.Web3Signer.net(key).name;
                }
                if (sb.indexOf(name) >= 0) continue;
                if (sb.length() > 0) sb.append(", ");
                sb.append(name);
            }
        }
        return sb.toString();
    }
}
