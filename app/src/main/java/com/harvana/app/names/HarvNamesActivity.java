package com.harvana.app.names;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.harvana.app.R;
import com.harvana.app.engine.Amounts;
import com.harvana.app.sync.NodeGroupClient;
import com.harvana.app.utils.AppBar;
import com.harvana.app.utils.InfoTable;
import com.harvana.app.wallet.WalletManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

/** Harvana names (doktohsmile.harv): claim one for this wallet, and renew, make primary or pass on the ones it holds. */
public class HarvNamesActivity extends AppCompatActivity {

    private final Handler ui = new Handler(Looper.getMainLooper());
    private EditText etName;
    private LinearLayout checkBox, myNames;
    private ChipGroup years;
    private MaterialButton btnClaim;
    private String me = "", checked = "";
    private long yearlyFee = 0;
    private int pickedYears = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppBar.setContent(this, R.layout.activity_harv_names, "Harvana names", "name.harv").setNavigationOnClickListener(v -> finish());
        me = WalletManager.getActiveWalletAddress(this);
        if (me == null) me = "";
        etName = findViewById(R.id.et_name);
        checkBox = findViewById(R.id.layout_name_check);
        myNames = findViewById(R.id.layout_my_names);
        years = findViewById(R.id.chips_years);
        btnClaim = findViewById(R.id.btn_claim_name);
        for (int y = 1; y <= 5; y++) {
            Chip c = new Chip(this);
            c.setText(y + (y == 1 ? " year" : " years"));
            c.setCheckable(true);
            c.setChecked(y == 1);
            final int yy = y;
            c.setOnCheckedChangeListener((b, on) -> { if (on) { pickedYears = yy; paintCheck(null); } });
            years.addView(c);
        }
        etName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                checked = "";
                btnClaim.setEnabled(false);
                ui.removeCallbacksAndMessages(null);
                String t = s.toString().trim().toLowerCase(java.util.Locale.US);
                if (t.length() >= 3) ui.postDelayed(() -> check(t), 450);
                else { checkBox.removeAllViews(); years.setVisibility(View.GONE); }
            }
        });
        btnClaim.setOnClickListener(v -> claim());
        loadMine();
    }

    private void check(String name) {
        NodeGroupClient.checkName(name, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject j) {
                runOnUiThread(() -> {
                    String now = etName.getText() == null ? "" : etName.getText().toString().trim().toLowerCase(java.util.Locale.US);
                    if (!now.equals(name) || isFinishing()) return;
                    boolean free = j.optBoolean("available");
                    checked = free ? name : "";
                    yearlyFee = j.optLong("yearlyFeeRaw", 0);
                    paintCheck(j);
                });
            }
            @Override public void onError(String e) { runOnUiThread(() -> paintCheck(null)); }
        });
    }

    private JSONObject lastCheck;

    private void paintCheck(JSONObject j) {
        if (j != null) lastCheck = j;
        checkBox.removeAllViews();
        if (lastCheck == null) return;
        boolean free = lastCheck.optBoolean("available");
        String reason = lastCheck.optString("reason", "");
        InfoTable t = new InfoTable(this)
                .row("Name", lastCheck.optString("name"))
                .row("Status", free ? "Available" : "INVALID_NAME".equals(reason) ? "Not allowed" : "NAME_RESERVED".equals(reason) ? "Reserved" : "Taken",
                        getColor(free ? R.color.colorSuccess : R.color.colorError));
        if (yearlyFee > 0) t.row("Per year", Amounts.harv(yearlyFee));
        if (free && yearlyFee > 0) t.row("Total (" + pickedYears + (pickedYears == 1 ? " year)" : " years)"), Amounts.harv(yearlyFee * pickedYears));
        if (!free && lastCheck.optLong("expiresAt") > 0) t.row("Taken until", DateFormat.getDateInstance().format(new Date(lastCheck.optLong("expiresAt"))));
        checkBox.addView(t.view());
        years.setVisibility(free ? View.VISIBLE : View.GONE);
        btnClaim.setEnabled(free && !me.isEmpty());
        btnClaim.setText(free ? "Claim " + lastCheck.optString("name") : "Claim");
    }

    private void claim() {
        if (checked.isEmpty() || me.isEmpty()) return;
        String name = checked;
        btnClaim.setEnabled(false);
        NodeGroupClient.nameAction(this, me, "CLAIM", name, String.valueOf(pickedYears), done("Claimed " + name + ".harv", () -> {
            etName.setText("");
            loadMine();
        }));
    }

    private void loadMine() {
        if (me.isEmpty()) return;
        NodeGroupClient.ownedNames(me, new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject j) { runOnUiThread(() -> paintMine(j.optJSONArray("names"))); }
            @Override public void onError(String e) { runOnUiThread(() -> paintMine(null)); }
        });
    }

    private void paintMine(JSONArray names) {
        if (isFinishing()) return;
        myNames.removeAllViews();
        if (names == null || names.length() == 0) {
            myNames.addView(new InfoTable(this).row("Names", "0").view());
            return;
        }
        LayoutInflater inf = LayoutInflater.from(this);
        DateFormat f = DateFormat.getDateInstance();
        for (int i = 0; i < names.length(); i++) {
            JSONObject n = names.optJSONObject(i);
            if (n == null) continue;
            View row = inf.inflate(R.layout.item_harv_name, myNames, false);
            ((TextView) row.findViewById(R.id.tv_hn_name)).setText(n.optString("name"));
            boolean active = n.optBoolean("active");
            ((TextView) row.findViewById(R.id.tv_hn_until)).setText((active ? "Until " : "Ran out ") + f.format(new Date(n.optLong("expiresAt"))));
            TextView badge = row.findViewById(R.id.tv_hn_badge);
            badge.setVisibility(n.optBoolean("primary") ? View.VISIBLE : !active ? View.VISIBLE : View.GONE);
            if (!active) { badge.setText("RENEW"); badge.setBackgroundResource(R.drawable.bg_chip_muted); }
            row.setOnClickListener(v -> actions(n));
            myNames.addView(row);
        }
    }

    private void actions(JSONObject n) {
        String full = n.optString("name");
        String name = full.endsWith(".harv") ? full.substring(0, full.length() - 5) : full;
        boolean active = n.optBoolean("active");
        java.util.List<String> labels = new java.util.ArrayList<>();
        labels.add("Renew");
        if (active && !n.optBoolean("primary")) labels.add("Make primary");
        if (active) labels.add("Transfer");
        new com.harvana.app.ui.SheetDialog.Builder(this).setTitle(full)
                .setItems(labels.toArray(new CharSequence[0]), (d, which) -> {
                    String l = labels.get(which);
                    if ("Renew".equals(l)) renew(name);
                    else if ("Make primary".equals(l)) NodeGroupClient.nameAction(this, me, "PRIMARY", name, "", done(full + " is primary", this::loadMine));
                    else transfer(name, full);
                }).show();
    }

    private void renew(String name) {
        CharSequence[] opts = new CharSequence[5];
        for (int y = 1; y <= 5; y++) opts[y - 1] = y + (y == 1 ? " year" : " years");
        new com.harvana.app.ui.SheetDialog.Builder(this).setTitle("Renew " + name + ".harv")
                .setItems(opts, (d, which) -> NodeGroupClient.nameAction(this, me, "RENEW", name, String.valueOf(which + 1),
                        done("Renewed " + name + ".harv", this::loadMine))).show();
    }

    private void transfer(String name, String full) {
        EditText to = new EditText(this);
        to.setHint("HV0x…");
        to.setSingleLine(true);
        int p = (int) (16 * getResources().getDisplayMetrics().density);
        to.setPadding(p, p, p, p);
        new com.harvana.app.ui.SheetDialog.Builder(this).setTitle("Transfer " + full).setView(to)
                .setPositiveButton("Transfer", (d, w) -> {
                    String dest = to.getText() == null ? "" : to.getText().toString().trim();
                    if (!dest.matches("^HV0x[0-9a-fA-F]{40}$")) { Toast.makeText(this, "Enter a Harvana address (HV0x…)", Toast.LENGTH_SHORT).show(); return; }
                    NodeGroupClient.nameAction(this, me, "TRANSFER", name, dest, done(full + " transferred", this::loadMine));
                })
                .setNegativeButton("Cancel", null).show();
    }

    private NodeGroupClient.Callback done(String ok, Runnable then) {
        return new NodeGroupClient.Callback() {
            @Override public void onSuccess(JSONObject json) {
                runOnUiThread(() -> { Toast.makeText(HarvNamesActivity.this, ok, Toast.LENGTH_SHORT).show(); then.run(); });
            }
            @Override public void onError(String e) {
                runOnUiThread(() -> {
                    btnClaim.setEnabled(!checked.isEmpty());
                    Toast.makeText(HarvNamesActivity.this, problem(e), Toast.LENGTH_LONG).show();
                });
            }
        };
    }

    private static String problem(String e) {
        String s = e == null ? "" : e;
        if (s.contains("INSUFFICIENT_BALANCE")) return "Not enough HARV for the name fee and tFare";
        if (s.contains("NAME_TAKEN")) return "That name is taken";
        if (s.contains("NAME_RESERVED")) return "That name is reserved";
        if (s.contains("INVALID_NAME")) return "3–32 letters, numbers or hyphens";
        if (s.contains("TOO_MANY_YEARS")) return "A name runs at most 5 years ahead";
        if (s.contains("NOT_NAME_OWNER")) return "This wallet does not hold that name";
        if (s.contains("NAME_RELEASED")) return "The name was released; claim it again";
        return s;
    }
}
