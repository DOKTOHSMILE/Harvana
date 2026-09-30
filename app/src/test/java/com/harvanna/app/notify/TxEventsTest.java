package com.harvanna.app.notify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class TxEventsTest {
    private static final String ME = "HV0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", OTHER = "HV0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    private static TxEvents.Tx tx(String hash, String from, String to, double amt, String memo, String status, long ts) {
        TxEvents.Tx t = new TxEvents.Tx();
        t.hash = hash; t.from = from; t.to = to; t.amount = amt; t.memo = memo; t.status = status; t.timestamp = ts;
        return t;
    }

    @Test public void receivedAndSent() {
        List<TxEvents.Tx> l = new ArrayList<>();
        l.add(tx("h1", OTHER, ME, 12.5, "", "confirmed", 100));
        l.add(tx("h2", ME, OTHER, 3, "", "confirmed", 101));
        List<TxEvents.Event> e = TxEvents.classify(l, ME, 0, "HARV");
        assertEquals(2, e.size());
        assertEquals("RECEIVED", e.get(0).kind);
        assertEquals("12.5 HARV from HV0xbbbb…bbbb", e.get(0).body);
        assertEquals("SENT", e.get(1).kind);
        assertEquals("3 HARV to HV0xbbbb…bbbb was confirmed.", e.get(1).body);
    }

    @Test public void signedGroupPaymentsAndPayouts() {
        List<TxEvents.Tx> l = new ArrayList<>();
        l.add(tx("h3", ME, OTHER, 5, "CIRCLE_CONTRIBUTION:CIRC-1:2", "confirmed", 100));
        l.add(tx("h4", OTHER, ME, 20, "CIRCLE_PAYOUT:CIRC-1:2", "confirmed", 101));
        l.add(tx("h5", OTHER, ME, 4, "CIRCLE_ABORT_REFUND:CIRC-1", "confirmed", 102));
        List<TxEvents.Event> e = TxEvents.classify(l, ME, 0, "HARV");
        assertEquals("SIGNED", e.get(0).kind);
        assertEquals("Payout received", e.get(1).title);
        assertEquals("Refund received", e.get(2).title);
        assertTrue(e.get(1).body.contains("your circle"));
    }

    @Test public void oldPendingAndSelfTransfersAreSkipped() {
        List<TxEvents.Tx> l = new ArrayList<>();
        l.add(tx("old", OTHER, ME, 1, "", "confirmed", 50));
        l.add(tx("pend", OTHER, ME, 1, "", "pending", 200));
        l.add(tx("self", ME, ME, 1, "", "confirmed", 200));
        l.add(tx("", OTHER, ME, 1, "", "confirmed", 200));
        assertTrue(TxEvents.classify(l, ME, 100, "HARV").isEmpty());
    }

    @Test public void amountFormatting() {
        assertEquals("12.5", TxEvents.amt(12.5));
        assertEquals("3", TxEvents.amt(3));
        assertEquals("0.0001", TxEvents.amt(0.0001));
        assertEquals("0", TxEvents.amt(0));
    }
}
