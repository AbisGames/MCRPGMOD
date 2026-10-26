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
    public void allocate(StatState state, Map<Stat,Integer> increments) {
        Objects.requireNonNull(increments);
        long cost = 0;
        for (var e : increments.entrySet()) {
            if (e.getKey() == null || e.getValue() == null || e.getValue() <= 0) throw new IllegalArgumentException("increments");
            cost = Math.addExact(cost, e.getValue().longValue());
        }
        if (cost == 0) throw new IllegalArgumentException("empty spend");
        spendTreeNode(state, cost, increments);
    }
    /** Only the tree service should invoke this after validating node prerequisites. */
    void spendTreeNode(StatState state, long cost, Map<Stat,Integer> increments) {
        Objects.requireNonNull(state); Objects.requireNonNull(increments);
        if (cost <= 0 || cost > state.availablePoints()) throw new IllegalArgumentException("insufficient stat points");
        EnumMap<Stat,Integer> validated = new EnumMap<>(Stat.class);
        for (var e : increments.entrySet()) {
            if (e.getKey() == null || e.getValue() == null || e.getValue() < 0) throw new IllegalArgumentException("increments");
            Math.addExact(state.invested(e.getKey()), e.getValue());
            validated.put(e.getKey(), e.getValue());
        }
        state.spend(validated, cost);
    }
    public int baseStat(StatState state, StatProfile profile, Stat stat) {
        return Math.addExact(profile.starting(stat), state.invested(stat));
    }
}
