package com.harvanna.wallet;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.FirebaseApp;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SketchApplication extends Application {
    
    private static Context appContext;
    
    public static Context getContext() {
        return appContext;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        
        // EXPLICIT INITIALIZATION: Forces Firebase to wake up early
        FirebaseApp.initializeApp(this);
        
        // One-time AJ0x -> HV0x wallet address migration (idempotent)
        com.harvanna.wallet.wallet.AddressMigration.run(this);
        
        // GLOBAL CRASH INTERCEPTOR (File Writer + Debug Screen)
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                String stackTrace = Log.getStackTraceString(throwable);
                
                if (stackTrace.contains("DebugActivity")) {
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(1);
                    return;
                }
                
                getApplicationContext().getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("crash_error", stackTrace)
                        .commit(); 
                
                try {
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                    String fileName = "Ajoo_Crash_" + timeStamp + ".txt";
                    
                    File downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                    File publicCrashFile = new File(downloadDir, fileName);
                    FileWriter pubWriter = new FileWriter(publicCrashFile);
                    pubWriter.append("FATAL CRASH DETECTED:\n\n").append(stackTrace);
                    pubWriter.flush();
                    pubWriter.close();
                    
                    File privateDir = new File(getApplicationContext().getExternalFilesDir(null), "CrashLogs");
                    if (!privateDir.exists()) privateDir.mkdirs();
                    File privateCrashFile = new File(privateDir, fileName);
                    FileWriter privWriter = new FileWriter(privateCrashFile);
                    privWriter.append("FATAL CRASH DETECTED:\n\n").append(stackTrace);
                    privWriter.flush();
                    privWriter.close();
                } catch (Exception e) {
                    // Ignore file writing errors during a crash
                }
                
                Intent intent = new Intent(getApplicationContext(), DebugActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                
                try { Thread.sleep(300); } catch (Exception ignored) {}
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
        });
    }
}
