package com.harvana.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.harvana.app.telemetry.CrashReportManager;
import com.harvana.app.utils.CrashExplainer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class DebugActivity extends Activity {

    private String crashStack = "";
    private String fullReport = "";
    private String savedPath = "";
    private TextView tvDownloadPath, tvMeaning, tvWhere, tvAdvice, tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        tvDownloadPath = findViewById(R.id.tv_debug_download_path);
        tvMeaning = findViewById(R.id.tv_debug_meaning);
        tvWhere = findViewById(R.id.tv_debug_where);
        tvAdvice = findViewById(R.id.tv_debug_advice);
        tvError = findViewById(R.id.tv_debug_error);

        // 1. Retrieve crash details from Intent extras or SharedPreferences
        Intent incoming = getIntent();
        if (incoming != null) {
            fullReport = incoming.getStringExtra("crash_report");
            crashStack = incoming.getStringExtra("crash_stack");
            savedPath = incoming.getStringExtra("crash_saved_path");
        }

        if (fullReport == null || fullReport.isEmpty()) {
            SharedPreferences prefs = getSharedPreferences(CrashReportManager.PREFS_NAME, Context.MODE_PRIVATE);
            fullReport = prefs.getString(CrashReportManager.KEY_CRASH_REPORT, "");
            crashStack = prefs.getString(CrashReportManager.KEY_CRASH_ERROR, "");
            savedPath = prefs.getString(CrashReportManager.KEY_CRASH_PATH, "");
        }

        // 2. If memory / prefs are empty, check disk for latest crash file
        if (fullReport == null || fullReport.isEmpty()) {
            fullReport = loadLatestReportFromDisk();
            if (crashStack == null || crashStack.isEmpty()) {
                crashStack = fullReport;
            }
        }

        // 3. Ensure a copy is saved to Download folder
        if (fullReport != null && !fullReport.isEmpty()) {
            if (savedPath == null || savedPath.isEmpty()) {
                savedPath = CrashReportManager.saveReportToDownloads(this, fullReport);
            }
        } else {
            // Generate a fresh environment diagnostics dump so user still has useful device info
            fullReport = CrashReportManager.buildFullReport(this, Thread.currentThread(), null, "Manual Debug Inspection (No uncaught crash in memory)");
            savedPath = CrashReportManager.saveReportToDownloads(this, fullReport);
        }

        // 4. Update UI fields
        updateUi(fullReport, crashStack, savedPath);

        // 5. Button click listeners
        findViewById(R.id.btn_debug_save_download).setOnClickListener(v -> {
            String path = CrashReportManager.saveReportToDownloads(DebugActivity.this, fullReport);
            if (path != null && !path.isEmpty()) {
                savedPath = path;
                tvDownloadPath.setText(savedPath);
                Toast.makeText(DebugActivity.this, "Crash report saved to Download folder", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(DebugActivity.this, "Saved to Downloads", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_debug_share).setOnClickListener(v -> {
            if (fullReport != null && !fullReport.isEmpty()) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Harvana Crash Report");
                shareIntent.putExtra(Intent.EXTRA_TEXT, fullReport);
                startActivity(Intent.createChooser(shareIntent, "Share Crash Report via"));
            } else {
                Toast.makeText(DebugActivity.this, "Nothing to share", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_debug_copy).setOnClickListener(v -> {
            String toCopy = (fullReport != null && !fullReport.isEmpty()) ? fullReport : crashStack;
            if (toCopy != null && !toCopy.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Harvana Crash Log", toCopy);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(DebugActivity.this, "Crash log copied to clipboard", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(DebugActivity.this, "Nothing to copy", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_debug_restart).setOnClickListener(v -> {
            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (intent == null) {
                intent = new Intent(DebugActivity.this, SplashScreenActivity.class);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finishAffinity();
            Process.killProcess(Process.myPid());
        });
    }

    private void updateUi(String report, String stack, String path) {
        if (path != null && !path.isEmpty()) {
            tvDownloadPath.setText(path);
        } else {
            tvDownloadPath.setText("Download/Harvana_Latest_Crash.txt");
        }

        CrashExplainer.Result r = CrashExplainer.explain(stack != null ? stack : "");
        tvMeaning.setText(r.meaning != null && !r.meaning.isEmpty() ? r.meaning : "Unexpected crash encountered during app execution.");
        
        if (r.where != null && !r.where.isEmpty()) {
            tvWhere.setText("Where: " + r.where);
            tvWhere.setVisibility(View.VISIBLE);
        } else {
            tvWhere.setVisibility(View.GONE);
        }

        if (r.advice != null && !r.advice.isEmpty()) {
            tvAdvice.setText(r.advice);
            tvAdvice.setVisibility(View.VISIBLE);
        } else {
            tvAdvice.setVisibility(View.GONE);
        }

        if (report != null && !report.isEmpty()) {
            tvError.setText(report);
        } else if (stack != null && !stack.isEmpty()) {
            tvError.setText(stack);
        } else {
            tvError.setText("No active crash stack trace found.");
        }
    }

    private String loadLatestReportFromDisk() {
        try {
            // Check public Downloads directory
            File pub = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File latest = new File(pub, "Harvana_Latest_Crash.txt");
            if (latest.exists() && latest.canRead()) {
                return readFile(latest);
            }

            // Check app external files Downloads
            File ext = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (ext != null) {
                File extLatest = new File(ext, "Harvana_Latest_Crash.txt");
                if (extLatest.exists() && extLatest.canRead()) {
                    return readFile(extLatest);
                }
            }

            // Check private internal CrashLogs folder
            File internalDir = new File(getFilesDir(), "CrashLogs");
            if (internalDir.exists() && internalDir.isDirectory()) {
                File[] files = internalDir.listFiles();
                if (files != null && files.length > 0) {
                    File newest = files[0];
                    for (File f : files) {
                        if (f.lastModified() > newest.lastModified()) {
                            newest = f;
                        }
                    }
                    return readFile(newest);
                }
            }
        } catch (Throwable ignored) {}
        return "";
    }

    private String readFile(File file) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (Throwable ignored) {}
        return sb.toString();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
        Process.killProcess(Process.myPid());
    }
}
