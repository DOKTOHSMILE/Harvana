package com.harvanna.app.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

public class SoundUtils {
    public static void playSound(Context context, int resId) {
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(context, resId);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                });
                mediaPlayer.start();
            }
        } catch (Exception e) {
            Log.e("SoundUtils", "Failed to play sound: " + e.getMessage());
        }
    }
}
