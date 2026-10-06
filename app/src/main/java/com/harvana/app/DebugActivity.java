package com.harvana.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Process;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class DebugActivity extends Activity {

    private String crashLog = "";
    private String meaningReport = "";
    private TextView meaningWhere, meaningText, adviceText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        float density = getResources().getDisplayMetrics().density;
        int p16 = (int) (16 * density);
        int p12 = (int) (12 * density);
        int p24 = (int) (24 * density);

        // 1. Root ScrollView Layout
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(0xFF0B131F); // Modern deep slate-navy

        // Main Container Stack
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(p24, p24, p24, p24);
        scrollView.addView(rootLayout);

        // 2. Header Section (Title & Subtitle)
        TextView titleText = new TextView(this);
        titleText.setText("⚠️ Application Crash");
        titleText.setTextColor(Color.WHITE);
        titleText.setTextSize(20f);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        rootLayout.addView(titleText);

        TextView subtitleText = new TextView(this);
        subtitleText.setText("An unexpected error caused the app to stop running.");
        subtitleText.setTextColor(0xFF8C9BAE); // Muted gray-blue
        subtitleText.setTextSize(13f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.setMargins(0, (int)(4 * density), 0, p16);
        subtitleText.setLayoutParams(subtitleParams);
        rootLayout.addView(subtitleText);

        // 2b. Plain-English card: what went wrong, where, and what to try.
        LinearLayout meaningCard = new LinearLayout(this);
        meaningCard.setOrientation(LinearLayout.VERTICAL);
        meaningCard.setPadding(p16, p16, p16, p16);
        GradientDrawable meaningBg = new GradientDrawable();
        meaningBg.setColor(0xFF131F33);
        meaningBg.setCornerRadius(12 * density);
        meaningBg.setStroke(1, 0xFF1E2D4A);
        meaningCard.setBackground(meaningBg);
        LinearLayout.LayoutParams meaningParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        meaningParams.setMargins(0, 0, 0, p16);
        meaningCard.setLayoutParams(meaningParams);

        TextView meaningLabel = new TextView(this);
        meaningLabel.setText("Crash meaning:");
        meaningLabel.setTextColor(0xFF7DD3FC);
        meaningLabel.setTextSize(12f);
        meaningLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        meaningCard.addView(meaningLabel);

        meaningText = new TextView(this);
        meaningText.setTextColor(Color.WHITE);
        meaningText.setTextSize(15f);
        meaningText.setTextIsSelectable(true);
        meaningText.setPadding(0, (int) (6 * density), 0, 0);
        meaningCard.addView(meaningText);

        meaningWhere = new TextView(this);
        meaningWhere.setTextColor(0xFF8C9BAE);
        meaningWhere.setTextSize(12.5f);
        meaningWhere.setPadding(0, (int) (10 * density), 0, 0);
        meaningCard.addView(meaningWhere);

        adviceText = new TextView(this);
        adviceText.setTextColor(0xFFB6E3C2);
        adviceText.setTextSize(13f);
        adviceText.setPadding(0, (int) (10 * density), 0, 0);
        meaningCard.addView(adviceText);
        rootLayout.addView(meaningCard);

        // 3. Error Log Card Container (Rounded Card UI)
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setPadding(p16, p16, p16, p16);
        
        // Card background shape with rounded corners
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(0xFF131F33); // Slightly lighter card surface
        cardBg.setCornerRadius(12 * density);
        cardBg.setStroke(1, 0xFF1E2D4A); // Subtle border
        cardLayout.setBackground(cardBg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f); // Weight 1 to expand
        cardParams.setMargins(0, 0, 0, p16);
        cardLayout.setLayoutParams(cardParams);

        // The raw message, for the developer, under its own tag.
        TextView messageLabel = new TextView(this);
        messageLabel.setText("Crash message:");
        messageLabel.setTextColor(0xFFFF9B9B);
        messageLabel.setTextSize(12f);
        messageLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        messageLabel.setPadding(0, 0, 0, (int) (8 * density));
        cardLayout.addView(messageLabel);

        // Error TextView
        TextView errorText = new TextView(this);
        errorText.setTextColor(0xFFFF6B6B); // Soft modern red
        errorText.setTextSize(12f);
        errorText.setTypeface(android.graphics.Typeface.MONOSPACE);
        errorText.setTextIsSelectable(true);
        cardLayout.addView(errorText);
        rootLayout.addView(cardLayout);

        // 4. Action Buttons Layout (Copy & Restart)
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setWeightSum(2);

        Button copyButton = createStyledButton("Copy Log", 0xFF1E2D4A, Color.WHITE, density);
        LinearLayout.LayoutParams btnParams1 = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btnParams1.setMargins(0, 0, (int)(8 * density), 0);
        copyButton.setLayoutParams(btnParams1);
        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!crashLog.isEmpty()) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Crash Log", meaningReport + "Crash message:\n" + crashLog);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(DebugActivity.this, "Crash log copied to clipboard", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(DebugActivity.this, "Nothing to copy", Toast.LENGTH_SHORT).show();
                }
            }
        });
        buttonLayout.addView(copyButton);

        Button restartButton = createStyledButton("Restart App", 0xFF0066FF, Color.WHITE, density);
        LinearLayout.LayoutParams btnParams2 = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btnParams2.setMargins((int)(8 * density), 0, 0, 0);
        restartButton.setLayoutParams(btnParams2);
        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                finishAffinity();
                Process.killProcess(Process.myPid());
            }
        });
        buttonLayout.addView(restartButton);

        rootLayout.addView(buttonLayout);
        setContentView(scrollView);

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

    // Helper method to build nice flat-style buttons programmatically
    private Button createStyledButton(String text, int bgColor, int textColor, float density) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(textColor);
        button.setTextSize(13f);
        button.setTypeface(null, android.graphics.Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(8 * density);
        button.setBackground(bg);
        
        int padVertical = (int) (12 * density);
        button.setPadding(0, padVertical, 0, padVertical);
        return button;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
        Process.killProcess(Process.myPid());
    }
}

