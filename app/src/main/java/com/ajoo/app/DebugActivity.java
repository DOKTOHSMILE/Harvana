package com.ajoo.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

public class DebugActivity extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 1. Build a basic screen completely in Java (Zero XML dependencies)
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(0xFF000E1F); // Dark Navy background
        
        TextView errorText = new TextView(this);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        errorText.setPadding(padding, padding, padding, padding);
        errorText.setTextColor(0xFFFF5E5E); // Red text
        errorText.setTextSize(13f);
        errorText.setTextIsSelectable(true); // Allows copying text
        
        scrollView.addView(errorText);
        setContentView(scrollView);
        
        // 2. Read the crash log saved by SketchApplication
        android.content.SharedPreferences prefs = getSharedPreferences("crash_prefs", android.content.Context.MODE_PRIVATE);
        String crashLog = prefs.getString("crash_error", "");
        
        // Clear the data so it doesn't get stuck in a loop next time
        prefs.edit().clear().apply(); 
        
        // 3. Display the error
        if (crashLog != null && !crashLog.isEmpty()) {
            errorText.setText("FATAL CRASH DETECTED:\n\n" + crashLog);
        } else {
            errorText.setText("No crash data found. Check the 'Downloads' or 'Internal Storage/Android/data/com.ajoo.app/files/CrashLogs' folder for the .txt file.");
        }
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Force the app to close completely so the user doesn't get a frozen black screen
        finishAffinity();
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
