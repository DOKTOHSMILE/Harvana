package com.harvana.app.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CrashExplainerTest {

    @Test public void nullViewIsExplainedAndLocated() {
        String trace = "java.lang.NullPointerException: Attempt to invoke virtual method 'int android.view.View.getVisibility()' on a null object reference\n"
                + "\tat android.widget.FrameLayout.layoutChildren(FrameLayout.java:284)\n"
                + "\tat com.harvana.app.EditProfileActivity.build(EditProfileActivity.java:95)\n";
        CrashExplainer.Result r = CrashExplainer.explain(trace);
        assertTrue(r.meaning, r.meaning.contains("screen element"));
        assertTrue(r.meaning, r.meaning.contains("getVisibility"));
        assertEquals("Edit profile screen (EditProfileActivity, EditProfileActivity.java line 95)", r.where);
        assertTrue(r.advice.contains("Restart App"));
    }

    @Test public void usesTheRootCause() {
        String trace = "java.lang.RuntimeException: Unable to start activity ComponentInfo{com.harvana.app/com.harvana.app.social.PublicProfileActivity}\n"
                + "\tat android.app.ActivityThread.performLaunchActivity(ActivityThread.java:1)\n"
                + "Caused by: java.lang.NumberFormatException: For input string: \"abc\"\n"
                + "\tat com.harvana.app.social.PublicProfileActivity.onCreate(PublicProfileActivity.java:40)\n";
        CrashExplainer.Result r = CrashExplainer.explain(trace);
        assertTrue(r.meaning, r.meaning.contains("not a valid number"));
        assertTrue(r.where, r.where.startsWith("Public profile screen"));
    }

    @Test public void networkFailureGetsNetworkAdvice() {
        CrashExplainer.Result r = CrashExplainer.explain("java.net.UnknownHostException: Unable to resolve host \"x\"\n\tat java.net.InetAddress.getAllByName(InetAddress.java:1)\n");
        assertTrue(r.meaning.contains("reach the server"));
        assertTrue(r.advice.contains("internet connection"));
    }

    @Test public void emptyTraceIsHandled() {
        assertEquals("No crash was recorded.", CrashExplainer.explain("").meaning);
        assertEquals("No crash was recorded.", CrashExplainer.explain(null).meaning);
    }

    @Test public void unknownTypeStillSaysSomething() {
        CrashExplainer.Result r = CrashExplainer.explain("java.lang.WeirdThingError: boom\n");
        assertTrue(r.meaning.contains("WeirdThingError"));
        assertTrue(r.meaning.contains("boom"));
    }

    @Test public void fragmentNamesBecomeTabs() {
        assertEquals("Groups tab", CrashExplainer.screenName("GroupsFragmentActivity"));
        assertEquals("Trust score screen", CrashExplainer.screenName("TrustScoreActivity"));
    }
}
