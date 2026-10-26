package com.pipican.stats;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Authoritative, transactional stat spending and kill-XP progression. */
public final class StatEngine {
    private final XpRules rules;
    public StatEngine(XpRules rules) { this.rules = Objects.requireNonNull(rules); }
    public long awardKill(StatState state, String entityId) {
        long reward = rules.reward(entityId);
        if (reward == 0) return 0;
        long nextXp = Math.addExact(state.xp(), reward);
        state.award(reward, rules.pointsForXp(nextXp));
        return reward;
    }
    public long xpToNextPoint(StatState state) {
        long target = rules.threshold(state.earnedPoints() + 1);
        return target == Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(0, target - state.xp());
    }
    /** Pass positive increments only; validates everything before mutating. */
    public void allocate(StatState state, Map<Stat,Integer> increments) {
        Objects.requireNonNull(state);
        Objects.requireNonNull(increments);
        EnumMap<Stat,Integer> safe = new EnumMap<>(Stat.class);
        long cost = 0;
        for (var e : increments.entrySet()) {
            if (e.getKey() == null || e.getValue() == null || e.getValue() <= 0) throw new IllegalArgumentException("increments");
            cost = Math.addExact(cost, e.getValue().longValue());
            Math.addExact(state.invested(e.getKey()), e.getValue());
            safe.put(e.getKey(), e.getValue());
        }
        if (cost == 0 || cost > state.availablePoints()) throw new IllegalArgumentException("insufficient stat points");
        state.spend(safe, cost);
    }
    public int baseStat(StatState state, StatProfile profile, Stat stat) {
        return Math.addExact(profile.starting(stat), state.invested(stat));
    }
}
