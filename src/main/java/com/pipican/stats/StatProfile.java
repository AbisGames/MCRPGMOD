package com.pipican.stats;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Class-owned starting attributes and conversion rates. Skill trees live elsewhere. */
public final class StatProfile {
    private final String classId;
    private final double baseHp;
    private final EnumMap<Stat, Integer> starting;
    private final EnumMap<Stat, Double> scaling;

    public StatProfile(String classId, double baseHp, Map<Stat, Integer> starting, Map<Stat, Double> scaling) {
        this.classId = Objects.requireNonNull(classId);
        if (!Double.isFinite(baseHp) || baseHp <= 0) throw new IllegalArgumentException("baseHp");
        this.baseHp = baseHp;
        this.starting = new EnumMap<>(Stat.class);
        this.scaling = new EnumMap<>(Stat.class);
        for (Stat stat : Stat.values()) {
            int base = starting.getOrDefault(stat, 0);
            double rate = scaling.getOrDefault(stat, 1.0);
            if (base < 0 || !Double.isFinite(rate) || rate < 0) throw new IllegalArgumentException(stat.name());
            this.starting.put(stat, base);
            this.scaling.put(stat, rate);
        }
    }
    public String classId() { return classId; }
    public double baseHp() { return baseHp; }
    public int starting(Stat stat) { return starting.get(stat); }
    public double scaling(Stat stat) { return scaling.get(stat); }
}
