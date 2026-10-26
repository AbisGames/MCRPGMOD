package com.pipican.stats;

import java.util.Map;
import java.util.Objects;

/** Configurable kill XP independent of vanilla XP, with safe threshold arithmetic. */
public final class XpRules {
    private final Map<String, Long> rewards;
    private final long firstThreshold;
    private final long thresholdStep;
    public XpRules(Map<String, Long> rewards, long firstThreshold, long thresholdStep) {
        if (firstThreshold <= 0 || thresholdStep < 0) throw new IllegalArgumentException("threshold");
        rewards.forEach((id, value) -> { if (id == null || value == null || value < 0) throw new IllegalArgumentException("reward"); });
        this.rewards = Map.copyOf(rewards);
        this.firstThreshold = firstThreshold;
        this.thresholdStep = thresholdStep;
    }
    public long reward(String entityId) { return rewards.getOrDefault(Objects.requireNonNull(entityId), 0L); }
    /** Cumulative XP required to have earned the Nth stat point (N >= 1). */
    public long threshold(long n) {
        if (n <= 0) return 0;
        try {
            long a = Math.multiplyExact(firstThreshold, n);
            // n(n-1)/2, avoiding avoidable intermediate overflow
            long x = n, y = n - 1;
            if ((x & 1L) == 0) x /= 2; else y /= 2;
            return Math.addExact(a, Math.multiplyExact(thresholdStep, Math.multiplyExact(x, y)));
        } catch (ArithmeticException overflow) { return Long.MAX_VALUE; }
    }
    public long pointsForXp(long xp) {
        if (xp < 0) throw new IllegalArgumentException("xp");
        long lo = 0, hi = Math.min(xp / firstThreshold + 1, 1_000_000_000L);
        while (lo < hi) {
            long mid = lo + (hi - lo + 1) / 2;
            if (threshold(mid) <= xp) lo = mid; else hi = mid - 1;
        }
        return lo;
    }
    public static XpRules defaults() {
        return new XpRules(Map.of(
            "minecraft:zombie", 5L, "minecraft:skeleton", 7L,
            "minecraft:creeper", 8L, "minecraft:enderman", 20L), 100, 50);
    }
}
