package com.harvanna.app.group;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GroupSaveWorkerTest {
    @Test public void retryTimesAreFixedThenHourly() {
        assertEquals(1, GroupSaveWorker.delayMinutes(0));
        assertEquals(5, GroupSaveWorker.delayMinutes(1));
        assertEquals(15, GroupSaveWorker.delayMinutes(2));
        assertEquals(30, GroupSaveWorker.delayMinutes(3));
        assertEquals(60, GroupSaveWorker.delayMinutes(4));
        assertEquals(60, GroupSaveWorker.delayMinutes(50));
    }
}
