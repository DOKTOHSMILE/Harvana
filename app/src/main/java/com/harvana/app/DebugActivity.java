package com.harvana.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Process;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class DebugActivity extends Activity {

    private String crashLog = "";
    private String meaningReport = "";
    private TextView meaningWhere, meaningText, adviceText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_debug);
        meaningText = findViewById(R.id.tv_debug_meaning);
        meaningWhere = findViewById(R.id.tv_debug_where);
        adviceText = findViewById(R.id.tv_debug_advice);
        TextView errorText = findViewById(R.id.tv_debug_error);

        findViewById(R.id.btn_debug_copy).setOnClickListener(v -> {
            if (!crashLog.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Crash Log", meaningReport + "Crash message:\n" + crashLog);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(DebugActivity.this, "Crash log copied to clipboard", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(DebugActivity.this, "Nothing to copy", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btn_debug_restart).setOnClickListener(v -> {
            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            finishAffinity();
            Process.killProcess(Process.myPid());
        });

        // 5. Read and Clear Crash Data
        SharedPreferences prefs = getSharedPreferences("crash_prefs", Context.MODE_PRIVATE);
        crashLog = prefs.getString("crash_error", "");
        
        // Clear data so it doesn't loop
        prefs.edit().clear().apply();

        if (crashLog != null && !crashLog.isEmpty()) {
            errorText.setText(crashLog);
            com.harvana.app.utils.CrashExplainer.Result r = com.harvana.app.utils.CrashExplainer.explain(crashLog);
            meaningText.setText(r.meaning);
            meaningWhere.setText(r.where.isEmpty() ? "" : "Where: " + r.where);
            meaningWhere.setVisibility(r.where.isEmpty() ? View.GONE : View.VISIBLE);
            adviceText.setText(r.advice);
            meaningReport = "Crash meaning:\n" + r.meaning + (r.where.isEmpty() ? "" : "\nWhere: " + r.where) + "\n\n";
        } else {
            meaningText.setText("No crash was recorded for this run.");
            meaningWhere.setVisibility(View.GONE);
            adviceText.setVisibility(View.GONE);
            errorText.setText("No active crash logs found in memory.\n\nIf you experienced a silent crash, check your device storage or internal IDE folder under Android/data/com.harvana.app/files/CrashLogs.");
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
        Process.killProcess(Process.myPid());
    }
}

