package com.harvana.app.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

import com.harvana.app.R;

import java.util.Locale;

/**
 * The one avatar. Three sizes, then stop: 24dp in lists, 32dp in headers, 40dp on profiles.
 *
 * Without a photo it draws the person's initials in white on a blue gradient disc; with a photo (Glide
 * {@code .circleCrop().into(view)} works as before) the photo fills the disc. An optional green dot marks
 * someone online. Because it is an ImageView, existing code that loads a picture into an ImageView keeps working.
 */
public class AvatarView extends AppCompatImageView {

    public enum Size {
        LIST(24, 10), HEADER(32, 12), PROFILE(40, 15);
        final int dp, textSp;
        Size(int dp, int textSp) { this.dp = dp; this.textSp = textSp; }
    }

    private Size size = Size.PROFILE;
    private String initials = "";
    private boolean online;
    private final Paint disc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);

    public AvatarView(Context c) { this(c, null); }

    public AvatarView(Context c, AttributeSet attrs) {
        super(c, attrs);
        if (attrs != null) {
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.AvatarView);
            int s = a.getInt(R.styleable.AvatarView_avatarSize, 2);
            size = Size.values()[Math.max(0, Math.min(2, s))];
            online = a.getBoolean(R.styleable.AvatarView_avatarOnline, false);
            a.recycle();
        }
        text.setColor(0xFFFFFFFF);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        dot.setColor(0xFF22C55E);
        ring.setColor(androidx.core.content.ContextCompat.getColor(c, R.color.colorSurface));
        setScaleType(ScaleType.CENTER_CROP);
    }

    public void setAvatarSize(Size s) { size = s; requestLayout(); invalidate(); }

    /** Initials from a display name: "Sarah Chen" gives SC, "sarah" gives S. */
    public void setPerson(String name) { setInitials(initialsOf(name)); }

    public void setInitials(String i) { initials = i == null ? "" : i; invalidate(); }

    public void setOnline(boolean on) { online = on; invalidate(); }

    public static String initialsOf(String name) {
        if (name == null) return "?";
        String source = name.trim();
        if (source.startsWith("@")) source = source.substring(1);
        String[] parts = source.split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return "?";
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase(Locale.US);
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.US);
    }

    private int px(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }

    @Override protected void onMeasure(int w, int h) {
        int p = px(size.dp);
        setMeasuredDimension(p, p);
    }

    @Override protected void onDraw(Canvas canvas) {
        float s = Math.min(getWidth(), getHeight());
        if (getDrawable() == null) {
            disc.setShader(new LinearGradient(0, 0, s, s, 0xFF2BA597, 0xFF0B6B61, Shader.TileMode.CLAMP));
            canvas.drawCircle(s / 2f, s / 2f, s / 2f, disc);
            if (!initials.isEmpty()) {
                text.setTextSize(size.textSp * getResources().getDisplayMetrics().scaledDensity);
                float y = s / 2f - (text.descent() + text.ascent()) / 2f;
                canvas.drawText(initials, s / 2f, y, text);
            }
        } else {
            int save = canvas.save();
            android.graphics.Path clip = new android.graphics.Path();
            clip.addCircle(s / 2f, s / 2f, s / 2f, android.graphics.Path.Direction.CW);
            canvas.clipPath(clip);
            super.onDraw(canvas);
            canvas.restoreToCount(save);
        }
        if (online) {
            float r = s * 0.16f;
            float cx = s - r - s * 0.02f, cy = s - r - s * 0.02f;
            canvas.drawCircle(cx, cy, r + px(1), ring);
            canvas.drawCircle(cx, cy, r, dot);
        }
    }
}
