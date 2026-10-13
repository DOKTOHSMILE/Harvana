package com.harvana.app.utils;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.harvana.app.R;

/**
 * The regular action bar for screens built in code: a toolbar in the theme's app-bar colours with a back arrow,
 * a title and an optional subtitle, installed as the activity's support action bar (so option menus and
 * getSupportActionBar() work) and matched by the status bar. The app theme has no built-in action bar, so a
 * screen that only calls setTitle() shows none.
 */
public final class AppBar {
    private AppBar() {}

    /** Sets the screen's content: toolbar (title, optional subtitle) above the screen's own layout {@code bodyLayout}. */
    public static Toolbar setContent(AppCompatActivity a, int bodyLayout, String title, String subtitle) {
        View body = android.view.LayoutInflater.from(a).inflate(bodyLayout, null, false);
        return setContent(a, body, title, subtitle);
    }

    /** Sets the screen's content: toolbar (title, optional subtitle) above {@code body}. The shell is res/layout/screen_app_bar.xml. */
    public static Toolbar setContent(AppCompatActivity a, View body, String title, String subtitle) {
        a.setContentView(R.layout.screen_app_bar);
        Toolbar tb = a.findViewById(R.id.app_bar_toolbar);
        TypedValue on = new TypedValue();
        a.getTheme().resolveAttribute(R.attr.onAppBarColor, on, true);
        ((ViewGroup) a.findViewById(R.id.app_bar_body)).addView(body,
            new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        a.setSupportActionBar(tb);
        if (a.getSupportActionBar() != null) {
            a.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            a.getSupportActionBar().setTitle(title);
            a.getSupportActionBar().setSubtitle(subtitle == null || subtitle.isEmpty() ? null : subtitle);
        }
        // Back arrow AFTER the toolbar is installed as the action bar: installing it resets the navigation icon.
        Drawable back = ContextCompat.getDrawable(a, R.drawable.ic_arrow_back);
        if (back != null) {
            // The arrow is a pressed-state selector that fades between states; drawn into a bitmap at once it would be caught
            // at the start of its fade-in, fully transparent (the "missing back arrow"). Skip the fade before drawing.
            back = back.mutate();
            back.jumpToCurrentState();
            if (back instanceof android.graphics.drawable.DrawableContainer) back = back.getCurrent() == null ? back : back.getCurrent().mutate();
            back = DrawableCompat.wrap(back);
            DrawableCompat.setTint(back, on.data);
            int px = Math.round(24 * a.getResources().getDisplayMetrics().density);
            android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(px, px, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bmp);
            back.setBounds(0, 0, px, px);
            back.draw(canvas);
            tb.setNavigationIcon(new android.graphics.drawable.BitmapDrawable(a.getResources(), bmp));
        }
        tb.setNavigationContentDescription("Back");
        tb.setNavigationOnClickListener(v -> a.finish());
        // Three-dot menu icon in the bar's contrast colour.
        tb.post(() -> {
            Drawable o = tb.getOverflowIcon();
            if (o != null) {
                o = DrawableCompat.wrap(o.mutate());
                DrawableCompat.setTint(o, on.data);
                tb.setOverflowIcon(o);
            }
        });
        StatusBarUtil.matchToolbar(a);
        return tb;
    }

    /** Changes the subtitle after the data has loaded (for example the group's name). */
    public static void setSubtitle(Activity a, String subtitle) {
        if (a instanceof AppCompatActivity && ((AppCompatActivity) a).getSupportActionBar() != null) {
            ((AppCompatActivity) a).getSupportActionBar().setSubtitle(subtitle == null || subtitle.isEmpty() ? null : subtitle);
        }
    }

    /**
     * For screens whose layout repeats the screen name as a big heading: puts the title (and a short subtitle)
     * in the action bar and hides the repeated heading views.
     */
    public static void header(Activity a, String title, String subtitle, int... hideViewIds) {
        if (a instanceof AppCompatActivity && ((AppCompatActivity) a).getSupportActionBar() != null) {
            androidx.appcompat.app.ActionBar bar = ((AppCompatActivity) a).getSupportActionBar();
            if (title != null) bar.setTitle(title);
            bar.setSubtitle(subtitle == null || subtitle.isEmpty() ? null : subtitle);
        }
        for (int id : hideViewIds) {
            View v = a.findViewById(id);
            if (v != null) v.setVisibility(View.GONE);
        }
    }
}
