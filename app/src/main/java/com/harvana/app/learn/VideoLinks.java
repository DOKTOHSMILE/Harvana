package com.harvana.app.learn;

import java.net.URI;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recognises the video links a Learn Hub author may paste (YouTube, Facebook, TikTok, Vimeo) and turns each into the address of that
 * platform's own embeddable player. The app never downloads or re-hosts a video: the reader streams it from the platform. Pure Java, so it
 * is unit tested without a phone.
 */
public final class VideoLinks {
    private VideoLinks() {}

    public enum Platform {
        YOUTUBE("YouTube"), FACEBOOK("Facebook"), TIKTOK("TikTok"), VIMEO("Vimeo");
        public final String label;
        Platform(String label) { this.label = label; }
    }

    public static final class Video {
        public final Platform platform;
        /** The video's id on its platform (a Facebook video has none we can read, so its link is used). */
        public final String id;
        /** A clean https link to the video's own page; this is what is saved with the article. */
        public final String link;
        /** The https address of the platform's embeddable player. */
        public final String embedUrl;
        /** A picture to show before play, or empty when the platform gives none without signing in. */
        public final String thumbnail;
        Video(Platform platform, String id, String link, String embedUrl, String thumbnail) {
            this.platform = platform; this.id = id; this.link = link; this.embedUrl = embedUrl; this.thumbnail = thumbnail;
        }
    }

    private static final Pattern YT_ID = Pattern.compile("^[A-Za-z0-9_-]{11}$");
    private static final Pattern DIGITS = Pattern.compile("^\\d{5,25}$");
    private static final Pattern TIKTOK_VIDEO = Pattern.compile("^/@[^/]+/video/(\\d{5,25})");
    private static final Pattern VIMEO_ID = Pattern.compile("^/(?:(?:channels/[^/]+|groups/[^/]+/videos|album/\\d+/video)/)?(\\d{5,12})(?:/[0-9a-f]+)?/?$");

    /** The video for a pasted link, or null if it is not one we can play. */
    public static Video parse(String raw) {
        URI u = uri(raw);
        if (u == null) return null;
        String host = host(u);
        String path = u.getRawPath() == null ? "" : u.getRawPath();
        String query = u.getRawQuery() == null ? "" : u.getRawQuery();

        // YouTube
        if (host.equals("youtu.be")) {
            String id = firstSegment(path);
            return youtube(id);
        }
        if (host.equals("youtube.com") || host.equals("youtube-nocookie.com") || host.equals("music.youtube.com")) {
            if (path.equals("/watch")) return youtube(param(query, "v"));
            for (String prefix : new String[] {"/shorts/", "/embed/", "/live/", "/v/"}) {
                if (path.startsWith(prefix)) return youtube(firstSegment(path.substring(prefix.length() - 1)));
            }
            return null;
        }

        // Vimeo
        if (host.equals("vimeo.com") || host.equals("player.vimeo.com")) {
            String p = host.equals("player.vimeo.com") && path.startsWith("/video/") ? path.substring("/video".length()) : path;
            Matcher m = VIMEO_ID.matcher(p);
            if (!m.find()) return null;
            String id = m.group(1);
            return new Video(Platform.VIMEO, id, "https://vimeo.com/" + id, "https://player.vimeo.com/video/" + id + "?playsinline=1", "");
        }

        // TikTok (only the full link: a short link has to be opened to find the video, which needs the internet and TikTok's say-so)
        if (host.equals("tiktok.com")) {
            Matcher m = TIKTOK_VIDEO.matcher(path);
            if (!m.find()) return null;
            String id = m.group(1);
            return new Video(Platform.TIKTOK, id, "https://www.tiktok.com" + m.group(0), "https://www.tiktok.com/embed/v2/" + id, "");
        }

        // Facebook: the embed takes the video's own page address
        if (host.equals("facebook.com") || host.equals("fb.watch") || host.equals("fb.com")) {
            boolean isVideo = host.equals("fb.watch")
                    ? !firstSegment(path).isEmpty()
                    : path.contains("/videos/") || path.startsWith("/watch") || path.contains("/reel/") || path.startsWith("/reel")
                      || path.startsWith("/share/v/") || path.startsWith("/share/r/");
            if (!isVideo) return null;
            String link = "https://" + (host.equals("fb.watch") ? "fb.watch" : "www.facebook.com") + path + (query.isEmpty() ? "" : "?" + query);
            String embed = "https://www.facebook.com/plugins/video.php?show_text=false&href=" + enc(link);
            return new Video(Platform.FACEBOOK, link, link, embed, "");
        }
        return null;
    }

    /**
     * Why a pasted link cannot be used, in a sentence for the author; null when it is fine or is empty.
     */
    public static String problem(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        if (parse(raw) != null) return null;
        URI u = uri(raw);
        if (u == null) return "That doesn't look like a web link. Paste the full link, starting with https://";
        String host = host(u);
        if (host.endsWith("tiktok.com")) {
            return "Open the video in TikTok or a browser, copy the full link (it has /video/ and a long number in it) and paste that. Short links can't be played here.";
        }
        if (host.endsWith("youtube.com") || host.equals("youtu.be")) return "That YouTube link isn't a single video. Open the video and copy its link.";
        if (host.endsWith("facebook.com") || host.equals("fb.watch") || host.equals("fb.com")) return "That Facebook link isn't a video. Open the video and copy its link.";
        if (host.endsWith("vimeo.com")) return "That Vimeo link isn't a single video. Open the video and copy its link.";
        return "Only YouTube, Facebook, TikTok and Vimeo video links can be played in an article.";
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private static Video youtube(String id) {
        if (id == null || !YT_ID.matcher(id).matches()) return null;
        return new Video(Platform.YOUTUBE, id, "https://www.youtube.com/watch?v=" + id,
                "https://www.youtube.com/embed/" + id + "?playsinline=1&rel=0&modestbranding=1",
                "https://img.youtube.com/vi/" + id + "/hqdefault.jpg");
    }

    private static URI uri(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty() || s.contains(" ")) return null;
        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://")) s = "https://" + s.substring(7);
        else if (!lower.startsWith("https://")) {
            if (lower.contains("://")) return null;
            s = "https://" + s;
        }
        try {
            URI u = new URI(s);
            return u.getHost() == null ? null : u;
        } catch (Exception e) {
            return null;
        }
    }

    private static String host(URI u) {
        String h = u.getHost().toLowerCase(Locale.ROOT);
        for (String prefix : new String[] {"www.", "m.", "mobile.", "web.", "music."}) {
            if (h.startsWith(prefix) && h.indexOf('.', prefix.length()) > 0 && !prefix.equals("music.")) h = h.substring(prefix.length());
        }
        return h;
    }

    private static String firstSegment(String path) {
        String p = path.startsWith("/") ? path.substring(1) : path;
        int slash = p.indexOf('/');
        return slash >= 0 ? p.substring(0, slash) : p;
    }

    private static String param(String query, String name) {
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(name)) return pair.substring(eq + 1);
        }
        return null;
    }

    private static String enc(String s) {
        try { return URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { return s; }
    }
}
