package com.harvana.app.learn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VideoLinksTest {

    @Test public void youtubeForms() {
        String id = "dQw4w9WgXcQ";
        String[] links = {
            "https://www.youtube.com/watch?v=" + id,
            "https://youtu.be/" + id + "?si=abc",
            "https://m.youtube.com/watch?v=" + id + "&t=30s",
            "youtube.com/shorts/" + id,
            "https://www.youtube.com/embed/" + id,
            "https://www.youtube.com/live/" + id + "?feature=share",
            "http://www.youtube.com/watch?feature=share&v=" + id,
        };
        for (String l : links) {
            VideoLinks.Video v = VideoLinks.parse(l);
            assertNotNull(l, v);
            assertEquals(l, VideoLinks.Platform.YOUTUBE, v.platform);
            assertEquals(l, id, v.id);
            assertEquals("https://www.youtube.com/watch?v=" + id, v.link);
            assertTrue(v.embedUrl.startsWith("https://www.youtube.com/embed/" + id));
            assertEquals("https://img.youtube.com/vi/" + id + "/hqdefault.jpg", v.thumbnail);
        }
    }

    @Test public void youtubeNonVideosAreRejected() {
        assertNull(VideoLinks.parse("https://www.youtube.com/"));
        assertNull(VideoLinks.parse("https://www.youtube.com/@somechannel"));
        assertNull(VideoLinks.parse("https://www.youtube.com/watch?v=short"));
        assertNotNull(VideoLinks.problem("https://www.youtube.com/@somechannel"));
    }

    @Test public void facebookVideos() {
        VideoLinks.Video a = VideoLinks.parse("https://www.facebook.com/somepage/videos/1234567890123/");
        assertNotNull(a);
        assertEquals(VideoLinks.Platform.FACEBOOK, a.platform);
        assertTrue(a.embedUrl.startsWith("https://www.facebook.com/plugins/video.php?show_text=false&href=https%3A%2F%2Fwww.facebook.com%2Fsomepage%2Fvideos%2F"));
        assertNotNull(VideoLinks.parse("https://fb.watch/abcDEF123/"));
        assertNotNull(VideoLinks.parse("https://m.facebook.com/watch/?v=123456789012"));
        assertNotNull(VideoLinks.parse("https://www.facebook.com/reel/123456789012345"));
        assertNull(VideoLinks.parse("https://www.facebook.com/somepage"));
    }

    @Test public void tiktokNeedsTheFullLink() {
        VideoLinks.Video v = VideoLinks.parse("https://www.tiktok.com/@someone/video/7234567890123456789?is_from_webapp=1");
        assertNotNull(v);
        assertEquals(VideoLinks.Platform.TIKTOK, v.platform);
        assertEquals("7234567890123456789", v.id);
        assertEquals("https://www.tiktok.com/embed/v2/7234567890123456789", v.embedUrl);
        assertNull(VideoLinks.parse("https://vm.tiktok.com/ZMabc123/"));
        assertTrue(VideoLinks.problem("https://vm.tiktok.com/ZMabc123/").contains("full link"));
    }

    @Test public void vimeo() {
        VideoLinks.Video v = VideoLinks.parse("https://vimeo.com/123456789");
        assertNotNull(v);
        assertEquals("https://player.vimeo.com/video/123456789?playsinline=1", v.embedUrl);
        assertNotNull(VideoLinks.parse("https://player.vimeo.com/video/123456789"));
    }

    @Test public void otherSitesAndJunkAreRefused() {
        assertNull(VideoLinks.parse("https://example.com/video.mp4"));
        assertNull(VideoLinks.parse("javascript:alert(1)"));
        assertNull(VideoLinks.parse("not a link"));
        assertNull(VideoLinks.parse(""));
        assertNull(VideoLinks.parse(null));
        assertNull(VideoLinks.problem(""));
        assertTrue(VideoLinks.problem("https://example.com/x").contains("YouTube, Facebook, TikTok and Vimeo"));
    }
}
