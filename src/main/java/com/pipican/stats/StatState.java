package com.pipican.stats;

import java.util.EnumMap;
import java.util.Map;

/**
 * Persist this data per player through NeoForge attachments once the loader is present.
 * All changes go through StatEngine on the server. Never trust a client-sent value.
 */
public final class StatState {
    private final EnumMap<Stat, Integer> invested = new EnumMap<>(Stat.class);
    private long xp;
    private long earnedPoints;
    private long spentPoints;
    public StatState() { for (Stat stat : Stat.values()) invested.put(stat, 0); }
    public long xp() { return xp; }
    public long earnedPoints() { return earnedPoints; }
    public long spentPoints() { return spentPoints; }
    public long availablePoints() { return earnedPoints - spentPoints; }
    public int invested(Stat stat) { return invested.get(stat); }
    public Map<Stat,Integer> investments() { return Map.copyOf(invested); }
    /** Validated snapshot restoration, used only by persistence. */
    public static StatState restore(long xp, long earned, Map<Stat,Integer> values) {
        if (xp < 0 || earned < 0 || values == null) throw new IllegalArgumentException("invalid save");
        StatState s = new StatState();
        long sum = 0;
        for (Stat stat : Stat.values()) {
            int v = values.getOrDefault(stat, 0);
            if (v < 0) throw new IllegalArgumentException("negative stat");
            sum = Math.addExact(sum, v);
            s.invested.put(stat, v);
        }
        if (sum > earned) throw new IllegalArgumentException("overspent save");
        s.xp = xp; s.earnedPoints = earned; s.spentPoints = sum;
        return s;
    }
    void award(long delta, long newEarned) {
        xp = Math.addExact(xp, delta);
        earnedPoints = newEarned;
    }
    void spend(Map<Stat,Integer> increments, long cost) {
        for (var e : increments.entrySet()) invested.put(e.getKey(), Math.addExact(invested.get(e.getKey()), e.getValue()));
        spentPoints = Math.addExact(spentPoints, cost);
    }
}
