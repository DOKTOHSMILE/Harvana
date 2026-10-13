package com.harvana.app.group;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.util.Locale;

/**
 * The chat apps a group can link to, from around the world. Each one: the name members see, a round badge in the app's
 * own colour, the link hosts it accepts (only https links on those hosts are saved), and an example link.
 */
public final class ChatPlatforms {
    private ChatPlatforms() {}

    public static final class Platform {
        public final String kind, name, glyph, hint;
        public final int color, glyphColor;
        final String[] hosts;

        Platform(String kind, String name, String glyph, int color, int glyphColor, String hint, String... hosts) {
            this.kind = kind; this.name = name; this.glyph = glyph; this.color = color; this.glyphColor = glyphColor;
            this.hint = hint; this.hosts = hosts;
        }

        /** True when {@code host} (lower case, no "www.") belongs to this app. Any host for OTHER. */
        public boolean accepts(String host) {
            if (hosts.length == 0) return host.contains(".");
            for (String h : hosts) {
                if (h.startsWith("*.") ? host.endsWith(h.substring(1)) || host.equals(h.substring(2)) : host.equals(h)) return true;
            }
            return false;
        }
    }

    private static final int WHITE = 0xFFFFFFFF;

    /** In the order the chips show: the most used first, then by region, "Other" last. */
    public static final Platform[] ALL = {
        new Platform("WHATSAPP", "WhatsApp", "W", 0xFF25D366, WHITE, "https://chat.whatsapp.com/…", "chat.whatsapp.com", "wa.me", "whatsapp.com", "api.whatsapp.com"),
        new Platform("TELEGRAM", "Telegram", "T", 0xFF229ED9, WHITE, "https://t.me/…", "t.me", "telegram.me", "telegram.org"),
        new Platform("FACEBOOK", "Facebook", "f", 0xFF1877F2, WHITE, "https://facebook.com/groups/…", "facebook.com", "m.facebook.com", "fb.com", "fb.me", "*.facebook.com"),
        new Platform("MESSENGER", "Messenger", "M", 0xFF0084FF, WHITE, "https://m.me/j/…", "m.me", "messenger.com"),
        new Platform("WECHAT", "WeChat", "W", 0xFF07C160, WHITE, "https://weixin.qq.com/…", "weixin.qq.com", "wechat.com", "u.wechat.com"),
        new Platform("GOOGLECHAT", "Google Chat", "G", 0xFF00AC47, WHITE, "https://chat.google.com/room/…", "chat.google.com"),
        new Platform("SIGNAL", "Signal", "S", 0xFF3A76F0, WHITE, "https://signal.group/#…", "signal.group", "signal.me"),
        new Platform("DISCORD", "Discord", "D", 0xFF5865F2, WHITE, "https://discord.gg/…", "discord.gg", "discord.com"),
        new Platform("VIBER", "Viber", "V", 0xFF7360F2, WHITE, "https://invite.viber.com/…", "invite.viber.com", "viber.com", "chats.viber.com"),
        new Platform("LINE", "LINE", "L", 0xFF06C755, WHITE, "https://line.me/R/ti/g/…", "line.me", "lin.ee"),
        new Platform("KAKAOTALK", "KakaoTalk", "K", 0xFFFEE500, 0xFF191919, "https://open.kakao.com/o/…", "open.kakao.com"),
        new Platform("ZALO", "Zalo", "Z", 0xFF0068FF, WHITE, "https://zalo.me/g/…", "zalo.me"),
        new Platform("INSTAGRAM", "Instagram", "I", 0xFFE1306C, WHITE, "https://ig.me/j/…", "ig.me", "instagram.com"),
        new Platform("SLACK", "Slack", "S", 0xFF4A154B, WHITE, "https://join.slack.com/…", "join.slack.com", "slack.com", "*.slack.com"),
        new Platform("TEAMS", "Microsoft Teams", "T", 0xFF5059C9, WHITE, "https://teams.microsoft.com/l/…", "teams.microsoft.com", "teams.live.com"),
        new Platform("OTHER", "Other", "·", 0xFF6B7280, WHITE, "https://…"),
    };

    /** The app for a stored kind; "Other" for anything unknown. */
    public static Platform of(String kind) {
        String k = kind == null ? "" : kind.toUpperCase(Locale.US);
        for (Platform p : ALL) if (p.kind.equals(k)) return p;
        return ALL[ALL.length - 1];
    }

    /** The app a pasted link belongs to (so pasting picks the chip), or null when no named app matches. */
    public static Platform forHost(String host) {
        if (host == null) return null;
        String h = host.toLowerCase(Locale.US);
        if (h.startsWith("www.")) h = h.substring(4);
        for (Platform p : ALL) if (p.hosts.length > 0 && p.accepts(h)) return p;
        return null;
    }

    /** A round badge in the app's colour with its letter: for the chips and the list rows. */
    public static Drawable badge(Resources res, Platform p, int sizeDp) {
        int px = Math.max(1, Math.round(sizeDp * res.getDisplayMetrics().density));
        Bitmap b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(p.color);
        c.drawCircle(px / 2f, px / 2f, px / 2f, paint);
        paint.setColor(p.glyphColor);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(px * 0.5f);
        Paint.FontMetrics fm = paint.getFontMetrics();
        c.drawText(p.glyph, px / 2f, px / 2f - (fm.ascent + fm.descent) / 2f, paint);
        BitmapDrawable d = new BitmapDrawable(res, b);
        d.setBounds(0, 0, px, px);
        return d;
    }
}
