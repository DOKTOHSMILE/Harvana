package com.harvanna.app.engine;

import java.util.ArrayList;
import java.util.List;

/** Everything a group screen needs, computed from the node's records. Pure data: no Android types. */
public final class GroupSnapshot {
    public static final class Stat {
        public final String label, value;
        public Stat(String label, String value) { this.label = label; this.value = value; }
    }

    public String family = "";          // CIRCLE | ACCUM | FIXED
    public String id = "";
    public String state = "";
    public String headline = "";
    public int progressCurrent, progressMax;
    public String deadlineNote = "";
    public final List<Stat> terms = new ArrayList<>();
    public String myState = "";
    public final List<Stat> mine = new ArrayList<>();
    public final List<String> notes = new ArrayList<>();
    public final List<GroupAction> actions = new ArrayList<>();
    public String escrow = "";

    public Stat term(String l, String v) { Stat s = new Stat(l, v); terms.add(s); return s; }
    public void my(String l, String v) { mine.add(new Stat(l, v)); }
}
